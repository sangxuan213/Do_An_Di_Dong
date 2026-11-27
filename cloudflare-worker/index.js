// Cloudflare Worker: đẩy thông báo tin nhắn qua FCM, KHÔNG cần gói Blaze của Firebase.
//
// Luồng: app gửi tin xong -> POST /notify-chat {chatId} kèm Firebase ID token ->
//   Worker xác thực token, đọc chats/{chatId} và users/{người nhận} bằng service account,
//   rồi gửi FCM (data message ưu tiên cao) tới máy người nhận.
//
// Worker KHÔNG tin nội dung do app gửi lên: tên người gửi, nội dung và người nhận đều đọc từ Firestore,
// và chỉ gửi khi người gọi đúng là người vừa gửi tin mới nhất của cuộc trò chuyện đó.

const JWK_URL = 'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';
const MAX_AGE_MS = 2 * 60 * 1000; // chỉ báo cho tin vừa gửi trong 2 phút gần đây

let jwkCache = { keys: null, exp: 0 };
let tokenCache = { token: null, exp: 0 };

export default {
  async fetch(request, env) {
    if (request.method !== 'POST') return json({ error: 'method_not_allowed' }, 405);
    const url = new URL(request.url);
    if (url.pathname !== '/notify-chat') return json({ error: 'not_found' }, 404);

    let uid;
    try {
      const header = request.headers.get('Authorization') || '';
      uid = await verifyIdToken(header.startsWith('Bearer ') ? header.slice(7) : '', env.FIREBASE_PROJECT_ID);
    } catch (e) {
      return json({ error: 'unauthorized' }, 401);
    }

    try {
      const body = await request.json().catch(() => ({}));
      const chatId = String(body.chatId || '');
      if (!/^[A-Za-z0-9_-]{1,200}$/.test(chatId)) return json({ error: 'bad_chat_id' }, 400);

      const sa = JSON.parse(env.SERVICE_ACCOUNT_JSON);
      const accessToken = await getAccessToken(sa);
      const project = env.FIREBASE_PROJECT_ID;

      const chat = await getDoc(project, accessToken, `chats/${chatId}`);
      if (!chat) return json({ sent: false, reason: 'chat_not_found' });

      const participants = arr(chat.participants).map((v) => v.stringValue);
      const lastSenderId = str(chat.lastSenderId);
      const updatedAt = num(chat.updatedAt);
      if (!participants.includes(uid) || lastSenderId !== uid) return json({ error: 'forbidden' }, 403);
      if (Date.now() - updatedAt > MAX_AGE_MS) return json({ sent: false, reason: 'stale' });

      const recipientId = participants.find((p) => p !== uid);
      if (!recipientId) return json({ sent: false, reason: 'no_recipient' });

      const names = (chat.participantNames && chat.participantNames.mapValue && chat.participantNames.mapValue.fields) || {};
      const senderName = str(names[uid]) || 'Tin nhắn mới';
      const text = (str(chat.lastMessage) || 'Đã gửi cho bạn một tin nhắn').slice(0, 200);

      const user = await getDoc(project, accessToken, `users/${recipientId}`);
      const fcmToken = user ? str(user.fcmToken) : '';
      if (!fcmToken) return json({ sent: false, reason: 'no_token' });

      const res = await fetch(`https://fcm.googleapis.com/v1/projects/${project}/messages:send`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: {
            token: fcmToken,
            // data message ưu tiên cao: app luôn tự dựng thông báo (kênh, nội dung, bấm vào mở đúng cuộc trò chuyện)
            data: {
              type: 'chat',
              chatId,
              recipientId,
              title: senderName,
              body: text,
              updatedAt: String(updatedAt),
            },
            android: { priority: 'HIGH', ttl: '3600s' },
          },
        }),
      });
      if (!res.ok) {
        console.error('FCM error', res.status, await res.text());
        return json({ sent: false, reason: 'fcm_error', status: res.status });
      }
      return json({ sent: true });
    } catch (e) {
      console.error('notify-chat failed', e && e.stack ? e.stack : e);
      return json({ error: 'internal' }, 500);
    }
  },
};

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), { status, headers: { 'Content-Type': 'application/json' } });
}

// ---- Firestore REST: đọc giá trị có kiểu ----
const str = (f) => (f && f.stringValue) || '';
const num = (f) => Number((f && (f.integerValue ?? f.doubleValue)) || 0);
const arr = (f) => (f && f.arrayValue && f.arrayValue.values) || [];

async function getDoc(project, accessToken, path) {
  const res = await fetch(
    `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents/${path}`,
    { headers: { Authorization: `Bearer ${accessToken}` } },
  );
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Firestore ${res.status}: ${await res.text()}`);
  return (await res.json()).fields || {};
}

// ---- Xác thực Firebase ID token (RS256) ----
async function verifyIdToken(token, projectId) {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('malformed');
  const header = b64urlJson(parts[0]);
  const payload = b64urlJson(parts[1]);
  if (header.alg !== 'RS256') throw new Error('alg');

  const jwk = (await getJwks()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error('kid');
  const key = await crypto.subtle.importKey(
    'jwk', jwk, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify'],
  );
  const ok = await crypto.subtle.verify(
    'RSASSA-PKCS1-v1_5', key, b64urlToBytes(parts[2]), new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
  );
  if (!ok) throw new Error('signature');

  const now = Math.floor(Date.now() / 1000);
  if (payload.aud !== projectId) throw new Error('aud');
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) throw new Error('iss');
  if (!payload.exp || payload.exp < now) throw new Error('expired');
  if (!payload.sub) throw new Error('sub');
  return payload.sub;
}

async function getJwks() {
  if (jwkCache.keys && jwkCache.exp > Date.now()) return jwkCache.keys;
  const res = await fetch(JWK_URL);
  if (!res.ok) throw new Error('jwk fetch');
  jwkCache = { keys: (await res.json()).keys, exp: Date.now() + 60 * 60 * 1000 };
  return jwkCache.keys;
}

// ---- Access token của service account (scope Firestore + FCM) ----
async function getAccessToken(sa) {
  if (tokenCache.token && tokenCache.exp > Date.now() + 60_000) return tokenCache.token;

  const iat = Math.floor(Date.now() / 1000);
  const claims = {
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat,
    exp: iat + 3600,
  };
  const unsigned = `${bytesToB64url(new TextEncoder().encode(JSON.stringify({ alg: 'RS256', typ: 'JWT' })))}.${bytesToB64url(
    new TextEncoder().encode(JSON.stringify(claims)),
  )}`;
  const key = await crypto.subtle.importKey(
    'pkcs8', pemToBytes(sa.private_key), { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign'],
  );
  const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(unsigned));

  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${bytesToB64url(sig)}`,
    }),
  });
  if (!res.ok) throw new Error(`token ${res.status}: ${await res.text()}`);
  const data = await res.json();
  tokenCache = { token: data.access_token, exp: Date.now() + (data.expires_in || 3600) * 1000 };
  return tokenCache.token;
}

// ---- base64 ----
function b64urlToBytes(s) {
  s = s.replace(/-/g, '+').replace(/_/g, '/');
  while (s.length % 4) s += '=';
  const bin = atob(s);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
function bytesToB64url(buf) {
  let s = '';
  for (const b of new Uint8Array(buf)) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function b64urlJson(s) {
  return JSON.parse(new TextDecoder().decode(b64urlToBytes(s)));
}
function pemToBytes(pem) {
  const b64 = pem.replace(/-----[A-Z ]+-----/g, '').replace(/\s+/g, '');
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out.buffer;
}

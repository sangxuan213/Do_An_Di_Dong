# Đẩy thông báo tin nhắn qua Cloudflare Worker (không cần Blaze)

App gửi tin xong thì gọi Worker này; Worker gửi FCM tới người nhận nên thông báo hiện ngay cả khi app đã tắt.

## Cài đặt (làm một lần)

1. Tạo tài khoản miễn phí tại https://dash.cloudflare.com (không cần thẻ).
2. Lấy khóa service account:
   Firebase Console > Project settings > Service accounts > **Generate new private key** > tải file JSON.
   **Khóa này là bí mật: không commit lên Git, không gửi cho ai, không dán vào chat.**
3. Mở terminal trong thư mục `cloudflare-worker` (cần Node.js):
   ```
   npx wrangler login
   npx wrangler secret put SERVICE_ACCOUNT_JSON     # dán TOÀN BỘ nội dung file JSON rồi Enter
   npx wrangler deploy
   ```
4. Lệnh deploy in ra địa chỉ dạng `https://uth-chat-push.<tên-bạn>.workers.dev`.
   Dán địa chỉ đó vào `AppConfig.PUSH_WORKER_URL` trong app, rồi build lại.
5. Sau khi dán khóa xong, xóa file JSON vừa tải khỏi máy (hoặc cất ở nơi an toàn, ngoài thư mục dự án).

## Kiểm tra

Dùng 2 máy/2 tài khoản. Máy A đăng nhập rồi vuốt tắt app hẳn. Máy B nhắn cho A: thông báo hiện trên A sau vài giây.
Lỗi thì xem log: `npx wrangler tail`.

## Bảo mật

- Worker kiểm tra Firebase ID token của người gọi (chữ ký, hạn dùng, đúng project).
- Tên, nội dung và người nhận đều đọc từ Firestore, không tin dữ liệu app gửi lên.
- Chỉ gửi khi người gọi là người vừa gửi tin mới nhất của cuộc trò chuyện đó và tin gửi trong 2 phút gần đây.
- Gói miễn phí của Cloudflare Workers cho 100.000 yêu cầu mỗi ngày, quá đủ cho đồ án.

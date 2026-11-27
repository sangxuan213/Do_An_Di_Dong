package com.example.uthdocument.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uthdocument.MainActivity
import com.example.uthdocument.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Thông báo tin nhắn mới, không cần Cloud Functions / gói Blaze:
 *  - Khi tiến trình app còn sống (đang mở hoặc chạy nền): lắng nghe Firestore nên báo gần như tức thì.
 *  - Khi app đã bị tắt hẳn: FollowNotificationWorker gọi [checkNow] khoảng 15 phút một lần.
 * Đang mở app thì không hiện thông báo (đã có chấm "chưa đọc" trong app).
 *
 * Quy ước dữ liệu (xem FirebaseChatRepositoryImpl): chats/{id}.participants, participantNames{uid},
 * lastMessage, lastSenderId, updatedAt, lastReadAt{uid}.
 */
object ChatNotifier {

    /** MainActivity bật/tắt khi vào/ra màn hình. */
    @Volatile
    var appVisible: Boolean = false

    private const val TAG = "ChatNotifier"
    private const val PREFS = "chat_notifier"
    private const val CHANNEL_ID = "uth_chat_channel"
    const val EXTRA_CHAT_ID = "chatId"

    private var started = false
    private var registration: ListenerRegistration? = null

    /** Gọi một lần khi mở app. Tự đổi người nghe khi đăng nhập/đăng xuất. */
    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            registration?.remove()
            registration = null
            val me = auth.currentUser?.uid ?: return@addAuthStateListener
            ensureBaseline(app, me)
            registration = FirebaseFirestore.getInstance().collection("chats")
                .whereArrayContains("participants", me)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Không nghe được tin nhắn mới", error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    process(app, me, snapshot.documents, show = !appVisible)
                }
        }
    }

    /** Dùng cho worker nền: hỏi Firestore một lần rồi báo những tin chưa báo. */
    suspend fun checkNow(context: Context, me: String) {
        val app = context.applicationContext
        ensureBaseline(app, me)
        val snapshot = FirebaseFirestore.getInstance().collection("chats")
            .whereArrayContains("participants", me)
            .get().await()
        process(app, me, snapshot.documents, show = !appVisible)
    }

    /** Mốc ban đầu: chỉ báo tin đến SAU lúc bắt đầu dùng tính năng này, không báo cả núi tin cũ. */
    private fun ensureBaseline(context: Context, me: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("baseline_$me")) {
            prefs.edit().putLong("baseline_$me", System.currentTimeMillis()).apply()
        }
    }

    private data class Item(val chatId: String, val sender: String, val text: String)

    @Synchronized
    private fun process(context: Context, me: String, docs: List<DocumentSnapshot>, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val baseline = prefs.getLong("baseline_$me", 0L)
        val editor = prefs.edit()
        val pending = mutableListOf<Item>()

        for (d in docs) {
            if (!d.exists()) continue
            val participants = (d.get("participants") as? List<*>)?.filterIsInstance<String>() ?: continue
            val otherId = participants.firstOrNull { it != me } ?: continue
            val lastSender = d.getString("lastSenderId").orEmpty()
            if (lastSender.isEmpty() || lastSender == me) continue

            val updatedAt = (d.get("updatedAt") as? Number)?.toLong() ?: 0L
            val lastRead = ((d.get("lastReadAt") as? Map<*, *>)?.get(me) as? Number)?.toLong() ?: 0L
            if (updatedAt <= baseline || updatedAt <= lastRead) continue

            val key = "notified_${me}_${d.id}"
            if (updatedAt <= prefs.getLong(key, 0L)) continue
            editor.putLong(key, updatedAt) // báo hay không thì cũng đánh dấu đã xử lý

            val names = d.get("participantNames") as? Map<*, *>
            pending += Item(
                chatId = d.id,
                sender = (names?.get(otherId) as? String).orEmpty().ifBlank { "Tin nhắn mới" },
                text = d.getString("lastMessage").orEmpty().ifBlank { "Đã gửi cho bạn một tin nhắn" }
            )
        }
        editor.apply()

        if (show && pending.isNotEmpty()) show(context, pending)
    }

    /**
     * Thông báo đến từ FCM (Worker đẩy). Dùng chung mốc "đã báo" với bộ nghe Firestore nên không báo trùng.
     * Bỏ qua nếu đang đăng nhập tài khoản khác với người nhận (máy dùng chung, đã đăng xuất).
     */
    @Synchronized
    fun showFromPush(
        context: Context,
        chatId: String?,
        recipientId: String?,
        sender: String?,
        text: String?,
        updatedAt: Long?
    ) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (chatId.isNullOrBlank()) return
        if (!recipientId.isNullOrBlank() && recipientId != me) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "notified_${me}_$chatId"
        if (updatedAt != null && updatedAt <= prefs.getLong(key, 0L)) return
        prefs.edit().putLong(key, updatedAt ?: System.currentTimeMillis()).apply()
        if (appVisible) return

        show(
            context,
            listOf(
                Item(
                    chatId = chatId,
                    sender = sender?.ifBlank { null } ?: "Tin nhắn mới",
                    text = text?.ifBlank { null } ?: "Đã gửi cho bạn một tin nhắn"
                )
            )
        )
    }

    private fun show(context: Context, items: List<Item>) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tin nhắn", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val manager = NotificationManagerCompat.from(context)
        // Mỗi cuộc trò chuyện một thông báo; tin mới hơn thay thế tin cũ trong cùng cuộc trò chuyện
        for (item in items) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_CHAT_ID, item.chatId)
            }
            val pending = PendingIntent.getActivity(
                context, item.chatId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFF3B71F7.toInt())
                .setContentTitle(item.sender)
                .setContentText(item.text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
            try {
                manager.notify(item.chatId.hashCode(), notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Không có quyền hiện thông báo", e)
            }
        }
    }
}

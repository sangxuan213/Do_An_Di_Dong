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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.uthdocument.MainActivity
import com.example.uthdocument.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Báo "X vừa đăng tài liệu mới" cho người đang theo dõi X, KHÔNG cần Cloud Functions / gói Blaze.
 * Cứ khoảng 15 phút (mức thấp nhất Android cho phép) app tự kiểm tra Firestore, kể cả khi đã tắt app.
 * Máy tiết kiệm pin có thể trễ lâu hơn. Muốn báo tức thì thì cần Cloud Function gửi FCM (phải bật Blaze).
 *
 * Quy ước: users/{me}/following/{uid}.followedAt (mili-giây) và documents/{id}.uploadedBy + timestamp (mili-giây).
 */
class FollowNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        var failed = false
        try {
            check(me)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Không kiểm tra được bài mới của người đang theo dõi", e)
            failed = true
        }
        // Cùng lượt chạy nền này cũng kiểm tra tin nhắn mới (khi app đã tắt hẳn)
        try {
            ChatNotifier.checkNow(applicationContext, me)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Không kiểm tra được tin nhắn mới", e)
            failed = true
        }
        return if (failed) Result.retry() else Result.success()
    }

    private suspend fun check(me: String) {
        val db = FirebaseFirestore.getInstance()

        val following: Map<String, Long> = db.collection("users").document(me).collection("following")
            .get().await()
            .documents.associate { it.id to (it.get("followedAt") as? Number)?.toLong().orZero() }
        if (following.isEmpty()) return

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "last_checked_$me"
        // Lần đầu: chỉ cần xét từ lúc bắt đầu theo dõi người đầu tiên
        val last = prefs.getLong(key, following.values.min())

        val snapshot = db.collection("documents").whereGreaterThan("timestamp", last).get().await()
        if (snapshot.isEmpty) return

        val fresh = snapshot.documents.mapNotNull { d ->
            val uploader = d.getString("uploadedBy") ?: return@mapNotNull null
            val followedAt = following[uploader] ?: return@mapNotNull null
            val time = d.millis("timestamp")
            if (time <= followedAt) return@mapNotNull null
            NewDocument(d.id, d.getString("title").orEmpty().ifBlank { "Tài liệu mới" }, uploader, time)
        }.sortedBy { it.time }

        // Nhớ mốc đã xem (kể cả bài của người không theo dõi) để lần sau không báo lại
        val newest = snapshot.documents.maxOf { it.millis("timestamp") }
        prefs.edit().putLong(key, maxOf(last, newest)).apply()

        if (fresh.isNotEmpty()) notify(db, fresh)
    }

    private suspend fun notify(db: FirebaseFirestore, docs: List<NewDocument>) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        ensureChannel()

        // Tên người đăng (mỗi người chỉ hỏi một lần)
        val names = HashMap<String, String>()
        for (uid in docs.map { it.uploaderId }.distinct()) {
            names[uid] = runCatching {
                db.collection("users").document(uid).get().await().getString("fullName")
            }.getOrNull().orEmpty().ifBlank { "Một người bạn theo dõi" }
        }

        val manager = NotificationManagerCompat.from(applicationContext)
        if (docs.size <= MAX_SINGLE) {
            // Mỗi bài một thông báo, giống Facebook
            docs.forEach { doc ->
                val title = "${names[doc.uploaderId]} vừa đăng tài liệu mới"
                manager.notify(doc.id.hashCode(), build(title, doc.title, doc.id, doc.id.hashCode()).build())
            }
        } else {
            val who = docs.map { names[it.uploaderId] }.distinct()
            val title = "${docs.size} tài liệu mới từ người bạn theo dõi"
            val body = who.take(3).joinToString(", ") + if (who.size > 3) " và ${who.size - 3} người khác" else ""
            manager.notify(SUMMARY_ID, build(title, body, null, SUMMARY_ID).build())
        }
    }

    private fun build(title: String, text: String, documentId: String?, requestCode: Int): NotificationCompat.Builder {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (documentId != null) putExtra(EXTRA_DOCUMENT_ID, documentId)
        }
        val pending = PendingIntent.getActivity(
            applicationContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF3B71F7.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Bài đăng của người bạn theo dõi", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private data class NewDocument(val id: String, val title: String, val uploaderId: String, val time: Long)

    private fun Long?.orZero() = this ?: 0L

    /** timestamp có thể là số (app) hoặc Timestamp (web của Bạn B). */
    private fun DocumentSnapshot.millis(field: String): Long = when (val v = get(field)) {
        is Number -> v.toLong()
        is Timestamp -> v.toDate().time
        else -> 0L
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "documentId"
        private const val TAG = "FollowNotifier"
        private const val PREFS = "follow_notifier"
        private const val CHANNEL_ID = "uth_follow_channel"
        private const val WORK_NAME = "follow_notifications"
        private const val MAX_SINGLE = 3
        private const val SUMMARY_ID = 1001

        /** Gọi một lần khi mở app; đã có lịch rồi thì giữ nguyên. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FollowNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

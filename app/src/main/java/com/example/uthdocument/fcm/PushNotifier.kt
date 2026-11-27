package com.example.uthdocument.fcm

import android.util.Log
import com.example.uthdocument.data.AppConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Nhờ Cloudflare Worker (xem thư mục cloudflare-worker) đẩy FCM tới người nhận sau khi mình gửi tin.
 * Chạy ngầm, lỗi chỉ ghi log, không ảnh hưởng việc gửi tin. AppConfig.PUSH_WORKER_URL để trống thì không làm gì.
 */
object PushNotifier {

    private const val TAG = "PushNotifier"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun notifyChat(chatId: String) {
        val base = AppConfig.PUSH_WORKER_URL.trim().trimEnd('/')
        if (base.isEmpty()) return
        scope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val token = user.getIdToken(false).await().token ?: return@launch
                val connection = (URL("$base/notify-chat").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                }
                try {
                    connection.outputStream.use { it.write(JSONObject().put("chatId", chatId).toString().toByteArray()) }
                    val code = connection.responseCode
                    if (code !in 200..299) Log.w(TAG, "Worker trả về mã $code")
                } finally {
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Không gọi được Worker đẩy thông báo", e)
            }
        }
    }
}

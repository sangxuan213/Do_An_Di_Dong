package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.model.Document
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tải tệp tài liệu từ Firebase Storage (qua fileUrl) về bộ nhớ riêng của app.
 * Tải một lần, lần sau mở lại là đọc offline luôn. Thư mục: filesDir/documents/<id>.<đuôi>
 * (cùng thư mục với tệp người dùng tự thêm, nên tài liệu vừa đăng không phải tải lại).
 */
class DocumentFileRepository(private val context: Context) {

    private companion object {
        const val FIRESTORE_SCHEME = "firestore://"
    }

    suspend fun getFile(document: Document, onProgress: (Float?) -> Unit = {}): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val target = targetFile(document)
                if (target.exists() && target.length() > 0) return@withContext Result.success(target)

                val url = document.fileUrl
                    ?: throw IOException("Tài liệu này chưa có đường dẫn tệp.")

                val partial = File(target.parentFile, target.name + ".part")
                try {
                    if (url.startsWith(FIRESTORE_SCHEME)) {
                        downloadFromFirestore(url, partial, onProgress)
                    } else {
                        download(url, partial, onProgress)
                    }
                    if (!partial.renameTo(target)) throw IOException("Không lưu được tệp vào máy.")
                } catch (e: Throwable) {
                    partial.delete()
                    throw e
                }
                Result.success(target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(IOException(e.message ?: "Không tải được tệp.", e))
            }
        }

    private fun targetFile(document: Document): File {
        val dir = File(context.filesDir, "documents").apply { mkdirs() }
        return File(dir, "${document.id}.${document.type.extension.lowercase()}")
    }

    /** Ghép các mảnh trong documents/{id}/chunks/ lại thành tệp (đường dẫn dạng "firestore://{id}?chunks=N"). */
    private suspend fun downloadFromFirestore(url: String, target: File, onProgress: (Float?) -> Unit) {
        val spec = url.removePrefix(FIRESTORE_SCHEME)
        val documentId = spec.substringBefore('?')
        val count = spec.substringAfter("chunks=", "").toIntOrNull()
            ?: throw IOException("Đường dẫn tệp không hợp lệ.")
        val chunks = FirebaseFirestore.getInstance()
            .collection("documents").document(documentId).collection("chunks")

        try {
            target.outputStream().use { output ->
                for (i in 0 until count) {
                    currentCoroutineContext().ensureActive()
                    val snapshot = chunks.document(i.toString().padStart(4, '0')).get().await()
                    val bytes = snapshot.getBlob("data")?.toBytes()
                        ?: throw IOException("Thiếu phần ${i + 1}/$count của tệp.")
                    output.write(bytes)
                    onProgress((i + 1).toFloat() / count)
                }
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw IOException("Firestore từ chối quyền đọc nội dung tệp. Hãy báo Bạn B thêm rule đọc cho documents/{id}/chunks.")
            }
            throw e
        }
    }

    private suspend fun download(url: String, target: File, onProgress: (Float?) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Máy chủ trả về lỗi ${connection.responseCode}.")
            }
            if (connection.contentType.orEmpty().lowercase().startsWith("text/html")) {
                throw IOException("Liên kết không trả về tệp trực tiếp (đang là trang web). Kiểm tra quyền chia sẻ hoặc dùng liên kết tải trực tiếp.")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive() // dừng ngay nếu người dùng thoát màn hình
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(if (total > 0) downloaded.toFloat() / total else null)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

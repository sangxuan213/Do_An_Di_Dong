package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.model.Document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

    suspend fun getFile(document: Document, onProgress: (Float?) -> Unit = {}): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val target = targetFile(document)
                if (target.exists() && target.length() > 0) return@withContext Result.success(target)

                val url = document.fileUrl
                    ?: throw IOException("Tài liệu này chưa có đường dẫn tệp.")

                val partial = File(target.parentFile, target.name + ".part")
                try {
                    download(url, partial, onProgress)
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

    private suspend fun download(url: String, target: File, onProgress: (Float?) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Máy chủ trả về lỗi ${connection.responseCode}.")
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

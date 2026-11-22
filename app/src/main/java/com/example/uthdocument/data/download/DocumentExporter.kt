package com.example.uthdocument.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Sao chép tệp tài liệu ra nơi người dùng thấy được (thư mục Tải xuống / Downloads). */
class DocumentExporter(private val context: Context) {

    /**
     * Android 10 trở lên: lưu thẳng vào Tải xuống/UTH Documents, không cần xin quyền.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun exportToDownloads(source: File, fileName: String, mimeType: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/UTH Documents")
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // ẩn cho tới khi ghi xong
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Không tạo được tệp trong thư mục Tải xuống.")
                try {
                    val output = resolver.openOutputStream(uri) ?: throw IOException("Không ghi được tệp.")
                    output.use { out -> source.inputStream().use { it.copyTo(out) } }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Throwable) {
                    resolver.delete(uri, null, null) // bỏ tệp dở dang
                    throw e
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(IOException(e.message ?: "Không lưu được tệp.", e))
            }
        }

    /** Android 9 trở xuống: người dùng chọn nơi lưu bằng hộp thoại hệ thống, rồi ghi vào Uri đó. */
    suspend fun copyToUri(source: File, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val output = context.contentResolver.openOutputStream(uri) ?: throw IOException("Không ghi được tệp.")
            output.use { out -> source.inputStream().use { it.copyTo(out) } }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IOException(e.message ?: "Không lưu được tệp.", e))
        }
    }
}

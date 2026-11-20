package com.example.uthdocument.data.link

import com.example.uthdocument.domain.model.DocumentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

/**
 * Kiểm tra nhanh một liên kết có thật sự trả về tệp đúng loại không (chỉ đọc vài byte đầu, không tải cả tệp).
 * Nhờ đó tài liệu đăng lên chắc chắn mở được, không bị "chết link" hay nhầm sang trang web.
 */
class LinkChecker {

    /** Thành công: trả về dung lượng tệp (byte), 0 nếu máy chủ không cho biết. */
    suspend fun check(url: String, type: DocumentType): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Range", "bytes=0-15")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IOException("Máy chủ trả về lỗi $code. Hãy kiểm tra lại liên kết và quyền chia sẻ (chọn \"Bất kỳ ai có đường liên kết\").")
                }
                val contentType = connection.contentType.orEmpty().lowercase()
                if (contentType.startsWith("text/html")) {
                    throw IOException("Liên kết đang mở ra một trang web chứ không phải tệp. Hãy chia sẻ công khai hoặc dùng liên kết tải trực tiếp.")
                }

                val head = ByteArray(4)
                val read = connection.inputStream.use { stream ->
                    var total = 0
                    while (total < head.size) {
                        val n = stream.read(head, total, head.size - total)
                        if (n == -1) break
                        total += n
                    }
                    total
                }
                if (read < head.size || !matchesType(head, type)) {
                    throw IOException("Nội dung tệp không đúng định dạng ${type.extension}. Hãy chọn đúng loại tệp ở trên.")
                }
                Result.success(totalSize(connection))
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Không có kết nối mạng."))
        } catch (e: Exception) {
            Result.failure(IOException(e.message ?: "Không kiểm tra được liên kết."))
        }
    }

    // PDF bắt đầu bằng "%PDF"; DOCX và PPTX là tệp zip nên bắt đầu bằng "PK\u0003\u0004"
    private fun matchesType(head: ByteArray, type: DocumentType): Boolean = when (type) {
        DocumentType.PDF -> head[0] == 0x25.toByte() && head[1] == 0x50.toByte() &&
            head[2] == 0x44.toByte() && head[3] == 0x46.toByte()
        DocumentType.DOCX, DocumentType.PPTX -> head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
            head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
    }

    private fun totalSize(connection: HttpURLConnection): Long {
        val fromRange = connection.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
        if (fromRange != null) return fromRange
        // Máy chủ bỏ qua yêu cầu đọc một phần và trả cả tệp: Content-Length chính là dung lượng tệp
        return connection.contentLengthLong.takeIf { it > 16 } ?: 0L
    }
}

package com.example.uthdocument.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Các tài liệu đã được tải về bộ nhớ của máy (đọc được khi không có mạng). */
interface DownloadsRepository {
    /** id tài liệu -> dung lượng tệp trên máy (byte). */
    val downloaded: StateFlow<Map<String, Long>>

    /** Quét lại thư mục (gọi sau khi vừa tải xong hoặc vừa thêm tệp). */
    fun refresh()

    /** Xóa bản tải về trên máy (tài liệu vẫn còn trên hệ thống, tải lại được). */
    suspend fun remove(documentId: String)
}

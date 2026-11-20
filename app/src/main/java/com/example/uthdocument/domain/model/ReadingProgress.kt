package com.example.uthdocument.domain.model

/** Tiến độ đọc một tài liệu PDF trên máy này. */
data class ReadingProgress(
    val documentId: String,
    val page: Int,          // trang đang đọc (tính từ 0), dùng để mở lại đúng chỗ
    val pageCount: Int,
    val fraction: Float,    // 0..1: đã đọc tới đâu (lấy mức xa nhất từng đọc tới)
    val lastReadAt: Long    // lần đọc gần nhất (mili-giây)
)

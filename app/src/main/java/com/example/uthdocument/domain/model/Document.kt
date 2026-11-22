package com.example.uthdocument.domain.model

/**
 * Model đại diện cho một Tài liệu trong hệ thống.
 * Áp dụng tính Đóng gói (Encapsulation) của OOP bằng cách sử dụng các thuộc tính val (chỉ đọc)
 * hoặc var (có thể thay đổi) với private setter nếu cần thiết.
 */
data class Document(
    val id: String,
    val title: String,
    val type: DocumentType, // Sử dụng Enum cho type
    val category: Category,
    val sizeInMb: Double,
    val isDownloaded: Boolean = false,
    val progress: Float = 0f, // Dành cho "Tài liệu tiếp tục học", 0.0 -> 1.0
    val filePath: String? = null, // Đường dẫn tệp trên máy (tài liệu người dùng thêm); null = chưa có tệp
    val courseId: String? = null, // Môn học mà tài liệu thuộc về; null = chưa gán môn
    val uploadedAt: Long = 0L, // Thời điểm đăng (mili-giây), dùng để sắp xếp "Mới nhất"
    val viewCount: Int = 0, // Số lượt xem, dùng để sắp xếp "Xem nhiều"
    val fileUrl: String? = null, // Đường dẫn tải tệp trên Firebase Storage; null = tệp chỉ có trên máy hoặc dữ liệu giả
    val uploaderId: String? = null // Id người đăng tài liệu; null = không rõ
)

enum class DocumentType(val extension: String) {
    PDF("PDF"),
    DOCX("DOCX"),
    PPTX("PPTX")
}

enum class Category(val displayName: String) {
    ECONOMICS("Kinh tế"),
    LOGISTICS("Logistics"),
    LAW("Pháp luật"),
    OTHER("Khác") // tài liệu có danh mục chưa khớp loại nào ở trên
}
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
    val filePath: String? = null // Đường dẫn tệp trên máy (tài liệu người dùng thêm); null = chưa có tệp
)

enum class DocumentType(val extension: String) {
    PDF("PDF"),
    DOCX("DOCX"),
    PPTX("PPTX")
}

enum class Category(val displayName: String) {
    ECONOMICS("Kinh tế"),
    LOGISTICS("Logistics"),
    LAW("Pháp luật")
}
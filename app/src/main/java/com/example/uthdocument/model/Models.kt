package com.example.uthdocument.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val studentId: String = "", // Mã số sinh viên / cán bộ
    val faculty: String = "",    // Khoa
    val className: String = "",  // Lớp
    val role: String = "STUDENT", // STUDENT, LECTURER, ADMIN (thống nhất chữ in hoa)
    val fcmToken: String = ""    // Lưu token thông báo đẩy
)

data class DocumentItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileName: String = "",    // Tên tệp gốc
    val fileType: String = "",    // Loại tệp: PDF, DOCX, PPTX
    val fileSize: Long = 0L,      // Dung lượng tệp
    val category: String = "",    // Danh mục tài liệu
    val uploadedBy: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val faculty: String = "",
    val courseCode: String = "",
    val qrCodeHash: String = ""
)

data class OfficialDispatch(
    val id: String = "",
    val dispatchNumber: String = "",
    val title: String = "",
    val content: String = "",
    val publishDate: String = "",
    val deadline: Long = 0L,      // Hạn xử lý (timestamp)
    val sender: String = "",
    val type: String = "",
    val status: String = "",
    val isSigned: Boolean = false,
    val signedFileUrl: String = "",
    val annotations: List<String> = emptyList()
)
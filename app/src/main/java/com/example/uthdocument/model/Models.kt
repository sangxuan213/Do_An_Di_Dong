package com.example.uthdocument.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "student" // student, admin, staff
)

data class DocumentItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
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
    val sender: String = "",
    val type: String = "",
    val status: String = "",
    val isSigned: Boolean = false,
    val signedFileUrl: String = "",
    val annotations: List<String> = emptyList()
)

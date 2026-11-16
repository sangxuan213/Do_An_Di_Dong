package com.uth.document.models

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
    val timestamp: Long = System.currentTimeMillis()
)

data class OfficialDispatch(
    val id: String = "",
    val dispatchNumber: String = "",
    val title: String = "",
    val content: String = "",
    val publishDate: String = "",
    val sender: String = ""
)
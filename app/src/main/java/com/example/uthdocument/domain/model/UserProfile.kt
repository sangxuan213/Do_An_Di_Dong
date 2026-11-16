package com.example.uthdocument.domain.model

enum class UserRole(val displayName: String) {
    STUDENT("Sinh viên"),
    LECTURER("Giảng viên"),
    ADMIN("Quản trị viên")
}

data class UserProfile(
    val fullName: String,
    val studentId: String,
    val faculty: String,
    val className: String,
    val email: String,
    val role: UserRole
)

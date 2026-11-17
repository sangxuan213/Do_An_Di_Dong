package com.example.uthdocument.domain.repository

/** Lỗi đăng nhập / đổi mật khẩu, message đã là tiếng Việt để hiện thẳng lên giao diện. */
class AuthException(message: String) : Exception(message)

interface SessionRepository {
    /** Đã đăng nhập sẵn chưa (phiên đăng nhập được giữ lại sau khi tắt app). */
    fun isLoggedIn(): Boolean

    /** identifier: mã số sinh viên / cán bộ hoặc email. */
    suspend fun login(identifier: String, password: String): Result<Unit>

    fun logout()

    suspend fun sendPasswordReset(identifier: String): Result<Unit>

    suspend fun changePassword(newPassword: String): Result<Unit>
}

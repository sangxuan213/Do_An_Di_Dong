package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.repository.AuthException
import com.example.uthdocument.domain.repository.SessionRepository

/** Bản giả để chạy thử giao diện khi chưa có Firebase (bật bằng AppConfig.USE_FAKE_AUTH). */
class FakeSessionRepositoryImpl : SessionRepository {

    private var loggedIn = false

    override fun isLoggedIn(): Boolean = loggedIn

    override suspend fun login(identifier: String, password: String): Result<Unit> =
        if (identifier.isNotBlank() && password.isNotBlank()) {
            loggedIn = true
            Result.success(Unit)
        } else {
            Result.failure(AuthException("Sai tài khoản hoặc mật khẩu."))
        }

    override fun logout() {
        loggedIn = false
    }

    override suspend fun sendPasswordReset(identifier: String): Result<Unit> = Result.success(Unit)

    override suspend fun changePassword(newPassword: String): Result<Unit> = Result.success(Unit)
}

package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.PublicProfile
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.domain.repository.UserRepository

/** Bản giả (khi USE_FAKE_AUTH = true): người dùng hiện tại là "fake-user". */
class FakeUserRepositoryImpl : UserRepository {

    private val profiles = listOf(
        PublicProfile("u-1", "TS. Trần Minh", UserRole.LECTURER, "Kinh tế vận tải", "Giảng viên"),
        PublicProfile("u-2", "Lê Hoa", UserRole.STUDENT, "Logistics", "LG22A"),
        PublicProfile(ME, "Nguyễn Văn A", UserRole.STUDENT, "Công nghệ thông tin", "CN22A")
    ).associateBy { it.uid }

    override fun currentUserId(): String = ME

    override suspend fun getPublicProfile(uid: String): PublicProfile? = profiles[uid]

    companion object {
        const val ME = "fake-user"
    }
}

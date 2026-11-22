package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.PublicProfile

interface UserRepository {
    /** Id của người đang đăng nhập (null nếu chưa đăng nhập). */
    fun currentUserId(): String?

    /** Hồ sơ công khai của một người dùng; null nếu không có hoặc không đọc được. */
    suspend fun getPublicProfile(uid: String): PublicProfile?
}

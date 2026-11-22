package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    // Thông tin người dùng đang đăng nhập
    fun getCurrentUser(): Flow<UserProfile>
}

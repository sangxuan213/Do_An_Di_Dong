package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.UserProfile
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProfileRepositoryImpl : ProfileRepository {

    // TODO (phối hợp Bạn B): lấy từ Firebase Auth / Firestore sau khi đăng nhập
    override fun getCurrentUser(): Flow<UserProfile> = flowOf(
        UserProfile(
            fullName = "Nguyễn Văn A",
            studentId = "2251012345",
            faculty = "Công nghệ thông tin",
            className = "CN22A",
            email = "2251012345@ut.edu.vn",
            role = UserRole.STUDENT
        )
    )
}

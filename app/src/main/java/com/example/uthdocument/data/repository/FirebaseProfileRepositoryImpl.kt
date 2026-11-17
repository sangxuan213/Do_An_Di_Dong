package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.UserProfile
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.domain.repository.ProfileRepository
import com.example.uthdocument.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Đọc hồ sơ từ Firestore (users/{uid}) qua AuthRepository của Bạn B; thiếu thì lấy từ email đăng nhập. */
class FirebaseProfileRepositoryImpl(
    private val authRepository: AuthRepository = AuthRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProfileRepository {

    override fun getCurrentUser(): Flow<UserProfile> = flow {
        val firebaseUser = auth.currentUser
        val email = firebaseUser?.email.orEmpty()
        val idFromEmail = email.substringBefore('@')
        val profile = firebaseUser?.uid?.let { authRepository.getUserProfile(it) }

        emit(
            UserProfile(
                fullName = profile?.fullName?.ifBlank { null } ?: idFromEmail.ifBlank { "Người dùng" },
                studentId = profile?.studentId?.ifBlank { null } ?: idFromEmail,
                faculty = profile?.faculty.orEmpty(),
                className = profile?.className.orEmpty(),
                email = profile?.email?.ifBlank { null } ?: email,
                role = parseRole(profile?.role)
            )
        )
    }

    private fun parseRole(raw: String?): UserRole =
        UserRole.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: UserRole.STUDENT
}

package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.PublicProfile
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Đọc hồ sơ công khai từ Firestore (users/{uid}); chỉ lấy tên, vai trò, khoa, lớp. */
class FirebaseUserRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun getPublicProfile(uid: String): PublicProfile? = try {
        val doc = db.collection("users").document(uid).get().await()
        if (!doc.exists()) {
            null
        } else {
            PublicProfile(
                uid = uid,
                fullName = doc.getString("fullName").orEmpty().ifBlank { "Người dùng" },
                role = UserRole.entries.firstOrNull { it.name.equals(doc.getString("role")?.trim(), ignoreCase = true) }
                    ?: UserRole.STUDENT,
                faculty = doc.getString("faculty").orEmpty(),
                className = doc.getString("className").orEmpty()
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

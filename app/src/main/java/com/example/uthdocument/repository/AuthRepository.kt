package com.example.uthdocument.repository

import com.example.uthdocument.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Đăng nhập tài khoản
    suspend fun login(email: String, pass: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Lấy thông tin chi tiết hồ sơ người dùng (bao gồm role, faculty, className) từ Firestore
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Gửi email quên mật khẩu
    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Đổi mật khẩu cho tài khoản đang đăng nhập
    suspend fun changePassword(newPassword: String): Boolean {
        return try {
            auth.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun logout() = auth.signOut()
}
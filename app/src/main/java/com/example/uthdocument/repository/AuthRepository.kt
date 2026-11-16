package com.example.uthdocument.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun login(email: String, pass: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun logout() = auth.signOut()
}

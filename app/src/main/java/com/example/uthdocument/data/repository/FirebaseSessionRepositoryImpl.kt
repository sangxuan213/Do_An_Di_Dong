package com.example.uthdocument.data.repository

import com.example.uthdocument.data.AppConfig
import com.example.uthdocument.domain.repository.AuthException
import com.example.uthdocument.domain.repository.SessionRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Dùng FirebaseAuth trực tiếp (thay vì AuthRepository của Bạn B) vì cần biết lỗi cụ thể
 * (sai mật khẩu, mất mạng, chưa bật Authentication...) để báo cho người dùng.
 */
class FirebaseSessionRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SessionRepository {

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun login(identifier: String, password: String): Result<Unit> = runAuth {
        auth.signInWithEmailAndPassword(toEmail(identifier), password).await()
        syncFcmToken()
    }

    override fun logout() = auth.signOut()

    override suspend fun sendPasswordReset(identifier: String): Result<Unit> = runAuth {
        auth.sendPasswordResetEmail(toEmail(identifier)).await()
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> = runAuth {
        val user = auth.currentUser ?: throw AuthException("Bạn chưa đăng nhập.")
        user.updatePassword(newPassword).await()
    }

    /** Lưu token thông báo đẩy của máy này vào users/{uid}. Lỗi thì bỏ qua, không ảnh hưởng đăng nhập. */
    private suspend fun syncFcmToken() {
        try {
            val uid = auth.currentUser?.uid ?: return
            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // bỏ qua
        }
    }

    private suspend fun runAuth(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AuthException(e.toVietnameseMessage()))
        }

    private fun toEmail(identifier: String): String {
        val id = identifier.trim()
        return if ('@' in id) id else "$id@${AppConfig.EMAIL_DOMAIN}"
    }

    private fun Exception.toVietnameseMessage(): String = when {
        message?.contains("CONFIGURATION_NOT_FOUND") == true ->
            "Dự án Firebase chưa bật Authentication. Hãy báo Bạn B."
        this is FirebaseNetworkException ->
            "Không có kết nối mạng. Hãy kiểm tra Wi-Fi hoặc dữ liệu di động."
        this is FirebaseTooManyRequestsException ->
            "Bạn thử quá nhiều lần. Vui lòng đợi một lát rồi thử lại."
        this is FirebaseAuthRecentLoginRequiredException ->
            "Vì lý do bảo mật, hãy đăng xuất rồi đăng nhập lại trước khi đổi mật khẩu."
        this is FirebaseAuthWeakPasswordException ->
            "Mật khẩu quá yếu, cần ít nhất 6 ký tự."
        this is FirebaseAuthException -> when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Mã số hoặc email không hợp lệ."
            "ERROR_USER_DISABLED" -> "Tài khoản này đã bị vô hiệu hóa."
            "ERROR_USER_NOT_FOUND" -> "Không tìm thấy tài khoản này."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Sai tài khoản hoặc mật khẩu."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Firebase chưa bật đăng nhập bằng Email/Mật khẩu. Hãy báo Bạn B."
            else -> "Thao tác thất bại ($errorCode)."
        }
        else -> "Có lỗi xảy ra: ${message ?: javaClass.simpleName}"
    }
}

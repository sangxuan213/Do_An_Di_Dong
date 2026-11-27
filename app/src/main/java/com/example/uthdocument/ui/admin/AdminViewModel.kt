package com.example.uthdocument.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminUserItem(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val studentId: String = "",
    val faculty: String = "",
    val className: String = "",
    val role: UserRole = UserRole.STUDENT,
    val disabled: Boolean = false // tài khoản đang bị khóa
)

data class AdminUiState(
    val isLoading: Boolean = false,
    val users: List<AdminUserItem> = emptyList(),
    val userQuery: String = "",
    val roleFilter: UserRole? = null, // null = tất cả vai trò
    val docQuery: String = "",
    val message: String? = null,
    val error: String? = null
)

class AdminViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val documentRepository: DocumentRepository = AppContainer.documentRepository,
    userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    /** Id của admin đang đăng nhập: không cho tự đổi vai trò hay tự khóa mình. */
    val currentUserId: String? = userRepository.currentUserId()

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    /** Toàn bộ tài liệu trên hệ thống (cập nhật thời gian thực); null = đang tải. */
    val documents: StateFlow<List<Document>?> = documentRepository
        .getDocumentsByCategory(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val snapshot = firestore.collection("users").get().await()
                snapshot.documents.map { doc ->
                    val roleStr = doc.getString("role")
                    val role = UserRole.entries.firstOrNull { it.name.equals(roleStr?.trim(), ignoreCase = true) }
                        ?: UserRole.STUDENT

                    AdminUserItem(
                        uid = doc.id,
                        email = doc.getString("email").orEmpty(),
                        fullName = doc.getString("fullName").orEmpty().ifBlank { doc.getString("studentId").orEmpty() },
                        studentId = doc.getString("studentId").orEmpty(),
                        faculty = doc.getString("faculty").orEmpty(),
                        className = doc.getString("className").orEmpty(),
                        role = role,
                        disabled = doc.getBoolean("disabled") ?: false
                    )
                }.sortedBy { it.fullName.lowercase() }
            }.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, users = list) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = "Không tải được danh sách người dùng: ${e.message}") }
            }
        }
    }

    fun onUserQueryChange(query: String) {
        _uiState.update { it.copy(userQuery = query) }
    }

    fun onRoleFilterChange(role: UserRole?) {
        _uiState.update { it.copy(roleFilter = role) }
    }

    fun onDocQueryChange(query: String) {
        _uiState.update { it.copy(docQuery = query) }
    }

    fun updateUserRole(uid: String, newRole: UserRole) {
        if (uid == currentUserId) {
            _uiState.update { it.copy(error = "Bạn không thể tự đổi vai trò của chính mình.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).update("role", newRole.name).await()
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map { if (it.uid == uid) it.copy(role = newRole) else it },
                        message = "Đã cập nhật vai trò người dùng."
                    )
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Lỗi cập nhật vai trò: ${e.message}") }
            }
        }
    }

    /** Khóa: người này bị đăng xuất ở lần mở app kế tiếp và không đăng nhập lại được. Mở khóa: dùng lại bình thường. */
    fun setUserLocked(uid: String, locked: Boolean) {
        if (uid == currentUserId) {
            _uiState.update { it.copy(error = "Bạn không thể tự khóa tài khoản của chính mình.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).update("disabled", locked).await()
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map { if (it.uid == uid) it.copy(disabled = locked) else it },
                        message = if (locked) "Đã khóa tài khoản." else "Đã mở khóa tài khoản."
                    )
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Lỗi cập nhật trạng thái tài khoản: ${e.message}") }
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(documentId)
                _uiState.update { it.copy(message = "Đã xóa tài liệu.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Không xóa được tài liệu.") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

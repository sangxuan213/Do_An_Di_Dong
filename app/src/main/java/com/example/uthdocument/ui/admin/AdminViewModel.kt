package com.example.uthdocument.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.domain.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val role: UserRole = UserRole.STUDENT
)

data class AdminUiState(
    val isLoading: Boolean = false,
    val users: List<AdminUserItem> = emptyList(),
    val searchQuery: String = "",
    val message: String? = null,
    val error: String? = null
)

class AdminViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val snapshot = firestore.collection("users").get().await()
                snapshot.documents.mapNotNull { doc ->
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
                        role = role
                    )
                }
            }.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, users = list) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = "Không tải được danh sách người dùng: ${e.message}") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateUserRole(uid: String, newRole: UserRole) {
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid)
                    .update("role", newRole.name)
                    .await()
            }.onSuccess {
                _uiState.update { state ->
                    val updatedList = state.users.map {
                        if (it.uid == uid) it.copy(role = newRole) else it
                    }
                    state.copy(users = updatedList, message = "Đã cập nhật vai trò người dùng.")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi cập nhật vai trò: ${e.message}") }
            }
        }
    }

    fun deleteUserAccount(uid: String) {
        viewModelScope.launch {
            runCatching {
                firestore.collection("users").document(uid).delete().await()
            }.onSuccess {
                _uiState.update { state ->
                    val updatedList = state.users.filter { it.uid != uid }
                    state.copy(users = updatedList, message = "Đã xóa tài khoản khỏi hệ thống.")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi xóa tài khoản: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}

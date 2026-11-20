package com.example.uthdocument.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val isSendingReset: Boolean = false,
    val resetMessage: String? = null,
    val resetSuccess: Boolean = false
)

class LoginViewModel(
    private val session: SessionRepository = AppContainer.sessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        if (_uiState.value.isLoading) return
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập mã số (hoặc email) và mật khẩu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            session.login(identifier, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, loggedIn = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun sendPasswordReset(identifier: String) {
        if (_uiState.value.isSendingReset) return
        if (identifier.isBlank()) {
            _uiState.update { it.copy(resetMessage = "Vui lòng nhập mã số hoặc email.", resetSuccess = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingReset = true, resetMessage = null) }
            session.sendPasswordReset(identifier)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSendingReset = false,
                            resetSuccess = true,
                            resetMessage = "Đã gửi email đặt lại mật khẩu. Hãy kiểm tra hộp thư của bạn (cả mục Spam)."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSendingReset = false, resetSuccess = false, resetMessage = e.message) }
                }
        }
    }

    fun clearResetMessage() {
        _uiState.update { it.copy(resetMessage = null, resetSuccess = false) }
    }
}

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

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false
)

class RegisterViewModel(
    private val session: SessionRepository = AppContainer.sessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        fullName: String,
        studentId: String,
        faculty: String,
        className: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (_uiState.value.isLoading) return

        val error = when {
            fullName.isBlank() || studentId.isBlank() || faculty.isBlank() || className.isBlank() ->
                "Vui lòng nhập đủ họ tên, mã số, khoa và lớp."
            email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                "Email không hợp lệ."
            '@' in studentId -> "Mã số không được chứa ký tự @."
            password.length < 6 -> "Mật khẩu cần ít nhất 6 ký tự."
            password != confirmPassword -> "Mật khẩu nhập lại không khớp."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            session.register(fullName, studentId, faculty, className, email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, registered = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}

package com.example.uthdocument.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.UserProfile
import com.example.uthdocument.domain.repository.ProfileRepository
import com.example.uthdocument.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordChangeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class ProfileViewModel(
    repository: ProfileRepository = AppContainer.profileRepository,
    private val session: SessionRepository = AppContainer.sessionRepository
) : ViewModel() {

    /** null = đang tải */
    val profile: StateFlow<UserProfile?> = repository
        .getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _passwordState = MutableStateFlow(PasswordChangeUiState())
    val passwordState: StateFlow<PasswordChangeUiState> = _passwordState.asStateFlow()

    fun changePassword(newPassword: String) {
        if (_passwordState.value.isLoading) return
        viewModelScope.launch {
            _passwordState.update { PasswordChangeUiState(isLoading = true) }
            session.changePassword(newPassword)
                .onSuccess { _passwordState.update { PasswordChangeUiState(success = true) } }
                .onFailure { e -> _passwordState.update { PasswordChangeUiState(error = e.message) } }
        }
    }

    fun resetPasswordState() {
        _passwordState.value = PasswordChangeUiState()
    }

    fun logout() = session.logout()
}

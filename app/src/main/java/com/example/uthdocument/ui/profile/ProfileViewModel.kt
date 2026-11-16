package com.example.uthdocument.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeProfileRepositoryImpl
import com.example.uthdocument.domain.model.UserProfile
import com.example.uthdocument.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    repository: ProfileRepository = FakeProfileRepositoryImpl()
) : ViewModel() {

    /** null = đang tải */
    val profile: StateFlow<UserProfile?> = repository
        .getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

package com.example.uthdocument.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.PublicProfile
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Hồ sơ công khai của một người + các tài liệu họ đã đăng + mở trò chuyện với họ. */
class UserProfileViewModel(
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val documentRepository: DocumentRepository = AppContainer.documentRepository,
    private val chatRepository: ChatRepository = AppContainer.chatRepository
) : ViewModel() {

    private val userId = MutableStateFlow<String?>(null)

    private val _profile = MutableStateFlow<PublicProfile?>(null)
    val profile: StateFlow<PublicProfile?> = _profile.asStateFlow()

    /** true khi đã tải xong (kể cả khi không tìm thấy người dùng). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isMe: Boolean get() = userId.value != null && userId.value == userRepository.currentUserId()

    /** null = đang tải */
    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<Document>?> = userId
        .flatMapLatest<String?, List<Document>?> { id ->
            if (id == null) flowOf(null) else documentRepository.getDocumentsByUploader(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load(uid: String) {
        if (userId.value == uid) return
        userId.value = uid
        _loaded.value = false
        viewModelScope.launch {
            _profile.value = userRepository.getPublicProfile(uid)
            _loaded.value = true
        }
    }

    /** Mở (hoặc tạo) cuộc trò chuyện với người này rồi gọi onReady(chatId). */
    fun startChat(onReady: (String) -> Unit) {
        val target = _profile.value ?: return
        viewModelScope.launch {
            try {
                onReady(chatRepository.openChat(target.uid, target.fullName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Không mở được cuộc trò chuyện."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

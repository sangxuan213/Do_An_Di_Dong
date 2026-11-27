package com.example.uthdocument.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.PublicProfile
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.FollowRepository
import com.example.uthdocument.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Hồ sơ công khai của một người + các tài liệu họ đã đăng + mở trò chuyện với họ. */
class UserProfileViewModel(
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val documentRepository: DocumentRepository = AppContainer.documentRepository,
    private val chatRepository: ChatRepository = AppContainer.chatRepository,
    private val followRepository: FollowRepository = AppContainer.followRepository
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

    /** Mình có đang theo dõi người này không. */
    val isFollowing: StateFlow<Boolean> = combine(userId, followRepository.followingIds()) { id, ids ->
        id != null && id in ids
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val followerCount: StateFlow<Int> = userId
        .flatMapLatest<String?, Int> { id -> if (id == null) flowOf(0) else followRepository.followerCount(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleFollow() {
        val id = userId.value ?: return
        val following = isFollowing.value
        viewModelScope.launch {
            try {
                if (following) followRepository.unfollow(id) else followRepository.follow(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Không thực hiện được. Thử lại sau."
            }
        }
    }

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

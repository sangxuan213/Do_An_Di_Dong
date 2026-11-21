package com.example.uthdocument.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.ChatMessage
import com.example.uthdocument.domain.model.Conversation
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Danh sách cuộc trò chuyện (null = đang tải). */
class ConversationsViewModel(
    repository: ChatRepository = AppContainer.chatRepository
) : ViewModel() {
    val conversations: StateFlow<List<Conversation>?> = repository.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Số cuộc trò chuyện có tin chưa đọc, hiện làm huy hiệu ở màn Cá nhân. */
class UnreadMessagesViewModel(
    repository: ChatRepository = AppContainer.chatRepository
) : ViewModel() {
    val unreadCount: StateFlow<Int> = repository.getConversations()
        .map { list -> list.count { it.unread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

/** Một cuộc trò chuyện: đọc tin nhắn, gửi tin, đánh dấu đã đọc. Gọi load(chatId) khi mở màn hình. */
class RepositoryChatViewModel(
    private val repository: ChatRepository = AppContainer.chatRepository,
    userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    val myId: String? = userRepository.currentUserId()

    private val chatId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = chatId
        .flatMapLatest<String?, List<ChatMessage>> { id ->
            if (id == null) flowOf(emptyList()) else repository.getMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversation: StateFlow<Conversation?> = chatId
        .flatMapLatest<String?, Conversation?> { id ->
            if (id == null) flowOf(null) else repository.getConversation(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(id: String) {
        if (chatId.value == id) return
        chatId.value = id
        markRead()
    }

    fun markRead() {
        val id = chatId.value ?: return
        viewModelScope.launch { repository.markRead(id) }
    }

    fun send(text: String) {
        val id = chatId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.sendMessage(id, trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Không gửi được tin nhắn."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

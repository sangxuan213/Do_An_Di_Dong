package com.example.uthdocument.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.data.repository.NotificationSeenStore
import com.example.uthdocument.domain.model.AppNotification
import com.example.uthdocument.domain.model.NotificationType
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.FollowRepository
import com.example.uthdocument.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Thông báo trong app, gồm:
 *  - bài mới của những người mình theo dõi (30 bài gần nhất)
 *  - tin nhắn chưa đọc
 * Mới nhất đứng đầu. "Chưa xem" = mới hơn mốc lần cuối mở màn Thông báo.
 */
class NotificationsViewModel(
    documentRepository: DocumentRepository = AppContainer.documentRepository,
    followRepository: FollowRepository = AppContainer.followRepository,
    chatRepository: ChatRepository = AppContainer.chatRepository,
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val seenStore: NotificationSeenStore = AppContainer.notificationSeenStore
) : ViewModel() {

    private val names = MutableStateFlow<Map<String, String>>(emptyMap())

    private val followedDocuments = combine(
        documentRepository.getDocumentsByCategory(null),
        followRepository.followingIds()
    ) { all, followed ->
        all.filter { doc -> doc.uploaderId?.let { it in followed } == true }
            .sortedByDescending { it.uploadedAt }
            .take(30)
    }

    init {
        // Lấy tên người đăng (mỗi người chỉ hỏi một lần)
        viewModelScope.launch {
            followedDocuments.collect { docs ->
                val missing = docs.mapNotNull { it.uploaderId }.distinct().filter { it !in names.value }
                for (uid in missing) {
                    val name = runCatching { userRepository.getPublicProfile(uid)?.fullName }.getOrNull()
                    names.update { it + (uid to (name?.ifBlank { null } ?: "Người dùng")) }
                }
            }
        }
    }

    val notifications: StateFlow<List<AppNotification>> = combine(
        followedDocuments,
        chatRepository.getConversations(),
        names
    ) { docs, conversations, nameMap ->
        val fromDocuments = docs.map { doc ->
            AppNotification(
                id = "doc_${doc.id}",
                type = NotificationType.NEW_DOCUMENT,
                title = "${nameMap[doc.uploaderId] ?: "Người bạn theo dõi"} vừa đăng tài liệu mới",
                text = doc.title,
                time = doc.uploadedAt,
                targetId = doc.id
            )
        }
        val fromChats = conversations.filter { it.unread }.map { c ->
            AppNotification(
                id = "chat_${c.id}",
                type = NotificationType.MESSAGE,
                title = c.otherName,
                text = c.lastMessage,
                time = c.updatedAt,
                targetId = c.id
            )
        }
        (fromDocuments + fromChats).sortedByDescending { it.time }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seenAt: StateFlow<Long> = seenStore.seenAt

    val unseenCount: StateFlow<Int> = combine(notifications, seenStore.seenAt) { list, seen ->
        list.count { it.time > seen }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Gọi khi rời màn Thông báo: mọi thông báo hiện có coi như đã xem. */
    fun markAllSeen() {
        seenStore.markSeen(maxOf(System.currentTimeMillis(), notifications.value.maxOfOrNull { it.time } ?: 0L))
    }
}

package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.ChatMessage
import com.example.uthdocument.domain.model.Conversation
import com.example.uthdocument.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Bản giả chạy trong bộ nhớ (khi USE_FAKE_AUTH = true), có sẵn một cuộc trò chuyện mẫu. */
class FakeChatRepositoryImpl : ChatRepository {

    override fun getConversations(): Flow<List<Conversation>> =
        store.map { chats -> chats.values.map { it.toConversation() }.sortedByDescending { it.updatedAt } }

    override fun getConversation(chatId: String): Flow<Conversation?> =
        store.map { it[chatId]?.toConversation() }

    override fun getMessages(chatId: String): Flow<List<ChatMessage>> =
        store.map { it[chatId]?.messages.orEmpty() }

    override suspend fun openChat(otherUserId: String, otherName: String): String {
        val id = "fake_$otherUserId"
        store.update { chats -> if (id in chats) chats else chats + (id to FakeChat(id, otherUserId, otherName)) }
        return id
    }

    override suspend fun sendMessage(chatId: String, text: String) {
        store.update { chats ->
            val chat = chats[chatId] ?: return@update chats
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = FakeUserRepositoryImpl.ME,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            chats + (chatId to chat.copy(messages = chat.messages + message, unread = false))
        }
    }

    override suspend fun markRead(chatId: String) {
        store.update { chats -> chats[chatId]?.let { chats + (chatId to it.copy(unread = false)) } ?: chats }
    }

    private data class FakeChat(
        val id: String,
        val otherId: String,
        val otherName: String,
        val messages: List<ChatMessage> = emptyList(),
        val unread: Boolean = false
    ) {
        fun toConversation(): Conversation {
            val last = messages.lastOrNull()
            return Conversation(
                id = id,
                otherUserId = otherId,
                otherName = otherName,
                lastMessage = last?.text.orEmpty(),
                lastSenderId = last?.senderId.orEmpty(),
                updatedAt = last?.timestamp ?: 0L,
                unread = unread
            )
        }
    }

    private companion object {
        private val now = System.currentTimeMillis()

        val store = MutableStateFlow(
            mapOf(
                "fake_u-1" to FakeChat(
                    id = "fake_u-1",
                    otherId = "u-1",
                    otherName = "TS. Trần Minh",
                    messages = listOf(
                        ChatMessage(id = "m1", senderId = "fake-user", text = "Thầy ơi, em xin file bài tập chương 3 ạ.", timestamp = now - 3_600_000),
                        ChatMessage(id = "m2", senderId = "u-1", text = "Thầy đã đăng trong tab Kinh tế vận tải rồi nhé.", timestamp = now - 1_800_000)
                    ),
                    unread = true
                )
            )
        )
    }
}

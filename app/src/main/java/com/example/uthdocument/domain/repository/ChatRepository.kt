package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.ChatMessage
import com.example.uthdocument.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Các cuộc trò chuyện của người dùng hiện tại, mới nhất đứng đầu. */
    fun getConversations(): Flow<List<Conversation>>

    fun getConversation(chatId: String): Flow<Conversation?>

    /** Tin nhắn của một cuộc trò chuyện, cũ -> mới. */
    fun getMessages(chatId: String): Flow<List<ChatMessage>>

    /** Trả về id cuộc trò chuyện với người này, tạo mới nếu chưa có. */
    suspend fun openChat(otherUserId: String, otherName: String): String

    suspend fun sendMessage(chatId: String, text: String)

    /** Đánh dấu đã đọc toàn bộ cuộc trò chuyện. */
    suspend fun markRead(chatId: String)
}

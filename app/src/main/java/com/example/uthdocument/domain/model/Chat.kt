package com.example.uthdocument.domain.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val participantNames: Map<String, String> = emptyMap()
)

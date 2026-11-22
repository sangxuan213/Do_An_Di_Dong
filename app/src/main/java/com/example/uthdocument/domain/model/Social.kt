package com.example.uthdocument.domain.model

/** Thông tin công khai của một người dùng (không gồm email, mã số). */
data class PublicProfile(
    val uid: String,
    val fullName: String,
    val role: UserRole,
    val faculty: String,
    val className: String
)

/** Một cuộc trò chuyện 1-1, nhìn từ phía người dùng hiện tại. */
data class Conversation(
    val id: String,
    val otherUserId: String,
    val otherName: String,
    val lastMessage: String,
    val lastSenderId: String,
    val updatedAt: Long,
    val unread: Boolean // có tin của người kia mà mình chưa đọc
)



package com.example.uthdocument.domain.model

enum class NotificationType { NEW_DOCUMENT, MESSAGE }

/** Một dòng trong màn Thông báo (chuông ở trang chủ). Được tính từ dữ liệu đang có, không lưu riêng. */
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val text: String,
    val time: Long,        // mili-giây
    val targetId: String   // NEW_DOCUMENT: id tài liệu; MESSAGE: id cuộc trò chuyện
)

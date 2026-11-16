package com.example.uthdocument.ui.util

/** "Vừa xong", "5 phút trước", "3 giờ trước", "2 ngày trước". */
fun formatTimeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - timestamp) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "Vừa xong"
        seconds < 3_600 -> "${seconds / 60} phút trước"
        seconds < 86_400 -> "${seconds / 3_600} giờ trước"
        else -> "${seconds / 86_400} ngày trước"
    }
}

package com.example.uthdocument.ui.util

/** 512000 -> "500 KB", 3145728 -> "3,0 MB". */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}

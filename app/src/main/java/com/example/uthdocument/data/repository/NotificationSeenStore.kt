package com.example.uthdocument.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Nhớ mốc thời gian người dùng đã xem màn Thông báo; thông báo mới hơn mốc này là "chưa xem" (hiện chấm đỏ ở chuông). */
class NotificationSeenStore(context: Context) {

    private val prefs = context.getSharedPreferences("notification_seen", Context.MODE_PRIVATE)

    init {
        // Lần đầu dùng: coi những gì có sẵn là đã xem, chỉ đếm thông báo đến sau đó
        if (!prefs.contains(KEY)) prefs.edit().putLong(KEY, System.currentTimeMillis()).apply()
    }

    private val _seenAt = MutableStateFlow(prefs.getLong(KEY, System.currentTimeMillis()))
    val seenAt: StateFlow<Long> = _seenAt

    fun markSeen(time: Long) {
        val value = maxOf(time, _seenAt.value)
        prefs.edit().putLong(KEY, value).apply()
        _seenAt.value = value
    }

    private companion object {
        const val KEY = "seen_at"
    }
}

package com.example.uthdocument.domain.model

import java.util.Calendar

enum class Weekday(val shortLabel: String, val fullLabel: String) {
    MONDAY("T2", "Thứ Hai"),
    TUESDAY("T3", "Thứ Ba"),
    WEDNESDAY("T4", "Thứ Tư"),
    THURSDAY("T5", "Thứ Năm"),
    FRIDAY("T6", "Thứ Sáu"),
    SATURDAY("T7", "Thứ Bảy"),
    SUNDAY("CN", "Chủ nhật");

    companion object {
        fun today(): Weekday = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> MONDAY
            Calendar.TUESDAY -> TUESDAY
            Calendar.WEDNESDAY -> WEDNESDAY
            Calendar.THURSDAY -> THURSDAY
            Calendar.FRIDAY -> FRIDAY
            Calendar.SATURDAY -> SATURDAY
            else -> SUNDAY
        }
    }
}

/** Một buổi học trong thời khóa biểu tuần. Giờ tính theo phút kể từ 00:00 (vd 07:00 = 420). */
data class ClassSession(
    val id: String,
    val subject: String,
    val day: Weekday,
    val startMinute: Int,
    val endMinute: Int,
    val room: String,
    val lecturer: String
) {
    val startText: String get() = formatMinute(startMinute)
    val endText: String get() = formatMinute(endMinute)

    private fun formatMinute(m: Int) = "%02d:%02d".format(m / 60, m % 60)
}

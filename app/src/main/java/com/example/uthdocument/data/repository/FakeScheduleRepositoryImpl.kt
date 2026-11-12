package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.ClassSession
import com.example.uthdocument.domain.model.Weekday
import com.example.uthdocument.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeScheduleRepositoryImpl : ScheduleRepository {

    private val sessions = listOf(
        ClassSession("s1", "Kinh tế vận tải", Weekday.MONDAY, 7 * 60, 9 * 60 + 30, "A1-201", "ThS. Trần Minh"),
        ClassSession("s2", "Logistics cơ bản", Weekday.MONDAY, 13 * 60, 15 * 60 + 30, "B2-105", "TS. Lê Hoa"),
        ClassSession("s3", "Xác suất thống kê", Weekday.TUESDAY, 7 * 60, 9 * 60 + 30, "A2-304", "ThS. Phạm Khoa"),
        ClassSession("s4", "Pháp luật hàng hải", Weekday.WEDNESDAY, 9 * 60 + 40, 11 * 60 + 30, "B1-102", "TS. Võ Lan"),
        ClassSession("s5", "Lập trình di động", Weekday.THURSDAY, 13 * 60, 16 * 60 + 30, "Lab C3-302", "ThS. Đặng Huy"),
        ClassSession("s6", "Xác suất thống kê", Weekday.FRIDAY, 9 * 60 + 40, 11 * 60 + 30, "A2-304", "ThS. Phạm Khoa")
    )

    override fun getSessions(): Flow<List<ClassSession>> = flowOf(sessions)
}

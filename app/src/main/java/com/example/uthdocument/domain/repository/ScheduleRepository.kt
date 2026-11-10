package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.ClassSession
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    // Toàn bộ thời khóa biểu trong tuần
    fun getSessions(): Flow<List<ClassSession>>
}

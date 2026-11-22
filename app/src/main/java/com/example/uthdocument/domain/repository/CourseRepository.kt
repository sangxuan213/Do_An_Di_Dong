package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getCourses(): Flow<List<Course>>

    // null nếu không tồn tại
    fun getCourseById(id: String): Flow<Course?>
}

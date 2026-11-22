package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeCourseRepositoryImpl : CourseRepository {

    private val courses = listOf(
        Course("KTVT", "KTVT", "Kinh tế vận tải", Category.ECONOMICS),
        Course("LOG", "LOG", "Logistics cơ bản", Category.LOGISTICS),
        Course("PLHH", "PLHH", "Pháp luật hàng hải", Category.LAW),
        Course("XSTK", "XSTK", "Xác suất thống kê", Category.ECONOMICS),
        Course("QTCU", "QTCU", "Quản trị chuỗi cung ứng", Category.LOGISTICS),
        Course("LTMQT", "LTMQT", "Luật thương mại quốc tế", Category.LAW)
    )

    override fun getCourses(): Flow<List<Course>> = flowOf(courses)

    override fun getCourseById(id: String): Flow<Course?> =
        flowOf(courses).map { list -> list.find { it.id == id } }
}

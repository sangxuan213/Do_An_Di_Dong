package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeCourseRepositoryImpl : CourseRepository {

    private val courses = listOf(
        Course("c1", "KTVT", "Kinh tế vận tải", Category.ECONOMICS),
        Course("c2", "LOG", "Logistics cơ bản", Category.LOGISTICS),
        Course("c3", "PLHH", "Pháp luật hàng hải", Category.LAW),
        Course("c4", "XSTK", "Xác suất thống kê", Category.ECONOMICS),
        Course("c5", "QTCU", "Quản trị chuỗi cung ứng", Category.LOGISTICS),
        Course("c6", "LTMQT", "Luật thương mại quốc tế", Category.LAW)
    )

    override fun getCourses(): Flow<List<Course>> = flowOf(courses)

    override fun getCourseById(id: String): Flow<Course?> =
        flowOf(courses).map { list -> list.find { it.id == id } }
}

package com.example.uthdocument.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeCourseRepositoryImpl
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.repository.CourseRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.ui.util.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CoursesUiState(
    val courses: List<Course>,
    val documentCounts: Map<String, Int> // courseId -> số tài liệu
)

class CoursesViewModel(
    courseRepository: CourseRepository = FakeCourseRepositoryImpl(),
    documentRepository: DocumentRepository = FakeDocumentRepositoryImpl()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _onlyFollowed = MutableStateFlow(false)
    val onlyFollowed: StateFlow<Boolean> = _onlyFollowed.asStateFlow()

    /** null = đang tải */
    val uiState: StateFlow<CoursesUiState?> = combine(
        courseRepository.getCourses(),
        documentRepository.getDocumentsByCategory(null),
        _query
    ) { courses, documents, q ->
        val key = q.trim().normalizedForSearch()
        val filtered = if (key.isEmpty()) {
            courses
        } else {
            courses.filter {
                it.name.normalizedForSearch().contains(key) || it.code.normalizedForSearch().contains(key)
            }
        }
        val counts = documents.mapNotNull { it.courseId }.groupingBy { it }.eachCount()
        CoursesUiState(filtered, counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun setOnlyFollowed(value: Boolean) {
        _onlyFollowed.value = value
    }
}

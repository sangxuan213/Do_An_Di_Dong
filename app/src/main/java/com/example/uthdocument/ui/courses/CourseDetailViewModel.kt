package com.example.uthdocument.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeCourseRepositoryImpl
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.CourseRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data object NotFound : CourseDetailUiState
    data class Success(val course: Course, val documents: List<Document>) : CourseDetailUiState
}

class CourseDetailViewModel(
    private val courseRepository: CourseRepository = FakeCourseRepositoryImpl(),
    private val documentRepository: DocumentRepository = FakeDocumentRepositoryImpl()
) : ViewModel() {

    private val courseId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CourseDetailUiState> = courseId
        .flatMapLatest<String?, CourseDetailUiState> { id ->
            if (id == null) {
                flowOf(CourseDetailUiState.Loading)
            } else {
                combine<Course?, List<Document>, CourseDetailUiState>(
                    courseRepository.getCourseById(id),
                    documentRepository.getDocumentsByCourse(id)
                ) { course, documents ->
                    if (course == null) CourseDetailUiState.NotFound
                    else CourseDetailUiState.Success(course, documents)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailUiState.Loading)

    fun load(id: String) {
        courseId.value = id
    }
}

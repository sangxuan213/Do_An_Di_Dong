package com.example.uthdocument.ui.courses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.uthdocument.data.repository.LocalCourseFollowRepository
import com.example.uthdocument.domain.repository.CourseFollowRepository
import kotlinx.coroutines.flow.StateFlow

/** Dùng chung cho mọi màn hình có nút theo dõi môn. */
class CourseFollowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CourseFollowRepository = LocalCourseFollowRepository.getInstance(application)

    val followedIds: StateFlow<Set<String>> = repository.followedIds

    fun toggle(id: String) = repository.toggle(id)
}

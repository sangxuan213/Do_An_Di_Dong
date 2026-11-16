package com.example.uthdocument.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface CourseFollowRepository {
    /** Tập id các môn đang theo dõi. */
    val followedIds: StateFlow<Set<String>>

    fun toggle(id: String)
}

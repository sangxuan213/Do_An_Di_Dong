package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.repository.CourseFollowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lưu danh sách môn đang theo dõi trên máy (SharedPreferences), giống LocalBookmarkRepository.
 * TODO (phối hợp Bạn B): đồng bộ theo tài khoản bằng Firestore nếu cần.
 */
class LocalCourseFollowRepository private constructor(context: Context) : CourseFollowRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _followedIds = MutableStateFlow(
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()
    )
    override val followedIds: StateFlow<Set<String>> = _followedIds.asStateFlow()

    override fun toggle(id: String) {
        _followedIds.update { current -> if (id in current) current - id else current + id }
        prefs.edit().putStringSet(KEY_IDS, _followedIds.value.toSet()).apply()
    }

    companion object {
        private const val PREFS_NAME = "followed_courses"
        private const val KEY_IDS = "ids"

        @Volatile
        private var instance: LocalCourseFollowRepository? = null

        fun getInstance(context: Context): LocalCourseFollowRepository =
            instance ?: synchronized(this) {
                instance ?: LocalCourseFollowRepository(context.applicationContext).also { instance = it }
            }
    }
}

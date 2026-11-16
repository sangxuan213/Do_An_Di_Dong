package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Lưu bookmark ngay trên máy bằng SharedPreferences (không cần thư viện ngoài, dùng được offline).
 * Là singleton để Trang chủ / Thư viện / Chi tiết luôn thấy cùng một trạng thái.
 * TODO (phối hợp Bạn B): nếu muốn đồng bộ theo tài khoản, viết thêm bản FirestoreBookmarkRepository
 * cùng implement BookmarkRepository rồi đổi chỗ khởi tạo trong BookmarkViewModel.
 */
class LocalBookmarkRepository private constructor(context: Context) : BookmarkRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _bookmarkedIds = MutableStateFlow(
        prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()
    )
    override val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    override fun toggle(id: String) {
        _bookmarkedIds.update { current -> if (id in current) current - id else current + id }
        // putStringSet: luôn đưa vào một bản copy mới, không sửa trực tiếp set đã lưu
        prefs.edit().putStringSet(KEY_IDS, _bookmarkedIds.value.toSet()).apply()
    }

    companion object {
        private const val PREFS_NAME = "bookmarks"
        private const val KEY_IDS = "ids"

        @Volatile
        private var instance: LocalBookmarkRepository? = null

        fun getInstance(context: Context): LocalBookmarkRepository =
            instance ?: synchronized(this) {
                instance ?: LocalBookmarkRepository(context.applicationContext).also { instance = it }
            }
    }
}

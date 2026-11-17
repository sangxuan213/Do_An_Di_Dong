package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.model.ReadingProgress
import com.example.uthdocument.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

/**
 * Lưu tiến độ đọc ngay trên máy (SharedPreferences), mỗi tài liệu một dòng "trang|tổng trang|tiến độ|thời điểm".
 * Là singleton để Trang chủ, Thư viện và trình xem PDF luôn thấy cùng một dữ liệu.
 */
class LocalReadingProgressRepository private constructor(context: Context) : ReadingProgressRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(loadAll())
    override val progress: StateFlow<Map<String, ReadingProgress>> = _progress.asStateFlow()

    override fun pageOf(documentId: String): Int = _progress.value[documentId]?.page ?: 0

    override fun save(documentId: String, page: Int, lastVisiblePage: Int, pageCount: Int) {
        if (pageCount <= 0) return
        val reached = ((lastVisiblePage + 1).toFloat() / pageCount).coerceIn(0f, 1f)
        val old = _progress.value[documentId]

        val entry = ReadingProgress(
            documentId = documentId,
            page = page.coerceIn(0, pageCount - 1),
            pageCount = pageCount,
            fraction = max(old?.fraction ?: 0f, reached),
            lastReadAt = System.currentTimeMillis()
        )
        _progress.update { it + (documentId to entry) }
        prefs.edit()
            .putString(KEY_PREFIX + documentId, "${entry.page}|${entry.pageCount}|${entry.fraction}|${entry.lastReadAt}")
            .apply()
    }

    private fun loadAll(): Map<String, ReadingProgress> {
        val result = mutableMapOf<String, ReadingProgress>()
        for ((key, value) in prefs.all) {
            if (!key.startsWith(KEY_PREFIX) || value !is String) continue
            val id = key.removePrefix(KEY_PREFIX)
            val parts = value.split('|')
            if (parts.size != 4) continue
            val page = parts[0].toIntOrNull() ?: continue
            val count = parts[1].toIntOrNull() ?: continue
            val fraction = parts[2].toFloatOrNull() ?: continue
            val time = parts[3].toLongOrNull() ?: continue
            result[id] = ReadingProgress(id, page, count, fraction, time)
        }
        return result
    }

    companion object {
        private const val PREFS_NAME = "reading_progress"
        private const val KEY_PREFIX = "p_"

        @Volatile
        private var instance: LocalReadingProgressRepository? = null

        fun getInstance(context: Context): LocalReadingProgressRepository =
            instance ?: synchronized(this) {
                instance ?: LocalReadingProgressRepository(context.applicationContext).also { instance = it }
            }
    }
}

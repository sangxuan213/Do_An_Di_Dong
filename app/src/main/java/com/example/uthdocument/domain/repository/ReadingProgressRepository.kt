package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.ReadingProgress
import kotlinx.coroutines.flow.StateFlow

interface ReadingProgressRepository {
    /** Tiến độ của mọi tài liệu, theo id. */
    val progress: StateFlow<Map<String, ReadingProgress>>

    /** Trang đang đọc dở của tài liệu (0 nếu chưa đọc). */
    fun pageOf(documentId: String): Int

    /**
     * page: trang đầu tiên đang hiện trên màn hình; lastVisiblePage: trang cuối cùng đang hiện.
     * Tiến độ chỉ tăng, không giảm khi người dùng cuộn ngược lại.
     */
    fun save(documentId: String, page: Int, lastVisiblePage: Int, pageCount: Int)
}

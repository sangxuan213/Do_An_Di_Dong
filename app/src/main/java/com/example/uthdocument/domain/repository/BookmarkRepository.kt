package com.example.uthdocument.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface BookmarkRepository {
    /** Tập id các tài liệu đã lưu. */
    val bookmarkedIds: StateFlow<Set<String>>

    /** Đang lưu -> bỏ lưu, chưa lưu -> lưu. */
    fun toggle(id: String)
}

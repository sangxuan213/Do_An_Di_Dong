package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.Comment
import com.example.uthdocument.domain.model.DocumentStats
import kotlinx.coroutines.flow.Flow

interface InteractionRepository {
    fun getStats(documentId: String): Flow<DocumentStats>

    // Số liệu của mọi tài liệu (để sắp xếp theo đánh giá)
    fun getAllStats(): Flow<Map<String, DocumentStats>>

    // Bình luận mới nhất đứng đầu
    fun getComments(documentId: String): Flow<List<Comment>>

    // Đang thích -> bỏ thích, chưa thích -> thích
    suspend fun toggleLike(documentId: String)

    // stars = 1..5; stars = 0 nghĩa là bỏ đánh giá của mình
    suspend fun setRating(documentId: String, stars: Int)

    suspend fun addComment(documentId: String, author: String, content: String)
}

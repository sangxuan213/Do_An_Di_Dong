package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Comment
import com.example.uthdocument.domain.model.DocumentStats
import com.example.uthdocument.domain.repository.InteractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

class FakeInteractionRepositoryImpl : InteractionRepository {

    override fun getStats(documentId: String): Flow<DocumentStats> =
        store.map { (it[documentId] ?: DocState()).toStats() }

    override fun getAllStats(): Flow<Map<String, DocumentStats>> =
        store.map { map -> map.mapValues { (_, state) -> state.toStats() } }

    override fun getComments(documentId: String): Flow<List<Comment>> =
        store.map { (it[documentId] ?: DocState()).comments.sortedByDescending { c -> c.timestamp } }

    override suspend fun toggleLike(documentId: String) =
        update(documentId) { it.copy(liked = !it.liked) }

    override suspend fun setRating(documentId: String, stars: Int) =
        update(documentId) { s ->
            val old = s.myRating
            s.copy(
                ratingSum = s.ratingSum - old + stars,
                ratingCount = s.ratingCount - (if (old > 0) 1 else 0) + (if (stars > 0) 1 else 0),
                myRating = stars
            )
        }

    override suspend fun addComment(documentId: String, author: String, content: String) =
        update(documentId) { s ->
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                author = author,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            s.copy(comments = s.comments + comment)
        }

    private fun update(documentId: String, transform: (DocState) -> DocState) {
        store.update { map -> map + (documentId to transform(map[documentId] ?: DocState())) }
    }

    /** Trạng thái tương tác của một tài liệu (phần của người khác + phần của mình). */
    private data class DocState(
        val baseLikes: Int = 0,        // lượt thích của người khác
        val liked: Boolean = false,    // mình đã thích
        val ratingSum: Int = 0,        // tổng số sao (gồm cả của mình)
        val ratingCount: Int = 0,
        val myRating: Int = 0,
        val comments: List<Comment> = emptyList()
    ) {
        fun toStats() = DocumentStats(
            likeCount = baseLikes + if (liked) 1 else 0,
            isLiked = liked,
            ratingAverage = if (ratingCount == 0) 0.0 else ratingSum.toDouble() / ratingCount,
            ratingCount = ratingCount,
            myRating = myRating
        )
    }

    private companion object {
        private const val HOUR = 3_600_000L

        // Dùng chung giữa mọi instance, chỉ nằm trong bộ nhớ -> tắt app là mất.
        // TODO (phối hợp Bạn B): thay bằng Firestore (collection comments, likes, ratings).
        val store = MutableStateFlow<Map<String, DocState>>(
            mapOf(
                "1" to DocState(
                    baseLikes = 12, ratingSum = 36, ratingCount = 8,
                    comments = listOf(
                        Comment("k1", "1", "Minh Anh", "Tài liệu rất đầy đủ, cảm ơn bạn đã chia sẻ!", System.currentTimeMillis() - 3 * HOUR),
                        Comment("k2", "1", "Trần Hoàng", "Phần chương 3 hơi khó, ai có bài tập kèm theo không?", System.currentTimeMillis() - 26 * HOUR)
                    )
                ),
                "2" to DocState(baseLikes = 5, ratingSum = 12, ratingCount = 3),
                "3" to DocState(
                    baseLikes = 2, ratingSum = 4, ratingCount = 1,
                    comments = listOf(
                        Comment("k3", "3", "Lan Phương", "Nội dung cập nhật theo luật mới, rất hữu ích.", System.currentTimeMillis() - 50 * HOUR)
                    )
                ),
                "4" to DocState(baseLikes = 20, ratingSum = 66, ratingCount = 15)
            )
        )
    }
}

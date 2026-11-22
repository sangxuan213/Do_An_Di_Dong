package com.example.uthdocument.domain.model

/** Một bình luận dưới tài liệu. */
data class Comment(
    val id: String,
    val documentId: String,
    val author: String,
    val content: String,
    val timestamp: Long // mili-giây
)

/** Số liệu tương tác của một tài liệu, đã tính sẵn cho người dùng hiện tại. */
data class DocumentStats(
    val likeCount: Int = 0,
    val isLiked: Boolean = false,       // người dùng hiện tại đã thích chưa
    val ratingAverage: Double = 0.0,    // điểm trung bình 1..5 (0 nếu chưa có ai đánh giá)
    val ratingCount: Int = 0,
    val myRating: Int = 0               // số sao người dùng hiện tại đã chấm (0 = chưa chấm)
)

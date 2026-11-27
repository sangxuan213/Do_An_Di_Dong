package com.example.uthdocument.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uthdocument.domain.model.Comment
import com.example.uthdocument.domain.model.DocumentStats
import com.example.uthdocument.ui.theme.UthBlue
import com.example.uthdocument.ui.util.formatTimeAgo

private val StarColor = Color(0xFFF5A623)
private val LikeColor = Color(0xFFE5484D)

/** Thanh nhỏ: nút thích + số lượt thích + điểm đánh giá trung bình. */
@Composable
internal fun DocumentActionBar(
    stats: DocumentStats,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleLike) {
            Icon(
                imageVector = if (stats.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (stats.isLiked) "Bỏ thích" else "Thích",
                tint = if (stats.isLiked) LikeColor else Color.Gray
            )
        }
        Text(text = "${stats.likeCount}", fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = StarColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (stats.ratingCount == 0) {
                "Chưa có đánh giá"
            } else {
                "${"%.1f".format(stats.ratingAverage)} (${stats.ratingCount})"
            },
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

/** Tab "Nhận xét": tổng quan đánh giá, ô chấm sao của mình, ô viết bình luận, danh sách bình luận. */
@Composable
internal fun ReviewsTab(
    stats: DocumentStats,
    comments: List<Comment>,
    onRate: (Int) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    onDeleteComment: (String) -> Unit = {}
) {
    var draft by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.imePadding(), // đẩy nội dung lên khi bàn phím mở
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (stats.ratingCount == 0) "–" else "%.1f".format(stats.ratingAverage),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            StarRow(value = stats.ratingAverage)
                            Text(text = "${stats.ratingCount} đánh giá", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(text = "Đánh giá của bạn", fontWeight = FontWeight.SemiBold)
                    RatingInput(myRating = stats.myRating, onRate = onRate)
                    if (stats.myRating > 0) {
                        Text(text = "Bấm lại đúng số sao đã chọn để bỏ đánh giá.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Viết nhận xét...") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi nhận xét", tint = UthBlue)
                }
            }
        }

        if (comments.isEmpty()) {
            item {
                Text(text = "Chưa có nhận xét nào. Hãy là người đầu tiên!", color = Color.Gray)
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    canDelete = currentUserId != null && comment.authorId == currentUserId,
                    onDelete = { onDeleteComment(comment.id) }
                )
            }
        }
    }
}

/** Hiển thị điểm trung bình bằng 5 ngôi sao (có nửa sao). */
@Composable
private fun StarRow(value: Double, iconSize: Dp = 18.dp) {
    Row {
        for (i in 1..5) {
            val icon = when {
                value >= i -> Icons.Default.Star
                value >= i - 0.5 -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(imageVector = icon, contentDescription = null, tint = StarColor, modifier = Modifier.size(iconSize))
        }
    }
}

/** 5 ngôi sao để người dùng chấm điểm. */
@Composable
private fun RatingInput(myRating: Int, onRate: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            IconButton(onClick = { onRate(i) }) {
                Icon(
                    imageVector = if (i <= myRating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Chấm $i sao",
                    tint = if (i <= myRating) StarColor else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment, canDelete: Boolean = false, onDelete: () -> Unit = {}) {
    var confirmDelete by remember { mutableStateOf(false) }
    val initial = comment.author.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE6EDFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = UthBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = comment.author, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = formatTimeAgo(comment.timestamp), fontSize = 12.sp, color = Color.Gray)
            }
            Text(text = comment.content)
        }
        if (canDelete) {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa nhận xét", tint = Color.Gray)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa nhận xét") },
            text = { Text("Bạn muốn xóa nhận xét này?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Hủy") } }
        )
    }
}

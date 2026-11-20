package com.example.uthdocument.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.ui.components.DocumentItem

private val UthNavy = Color(0xFF002D58)
private val UthTeal = Color(0xFF007B7A)
private val UthBlue = Color(0xFF3B71F7)
private val UthBackground = Color(0xFFF8FAFC)

@Composable
fun HomeScreen(
    onDocumentClick: (String) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val recent = viewModel.recentDocuments.collectAsState().value
    val latest = viewModel.latestDocuments.collectAsState().value
    val bookmarkedIds = bookmarkViewModel.bookmarkedIds.collectAsState().value

    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }

    if (recent == null || latest == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UthBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = UthTeal)
        }
        return
    }

    val filteredLatest = remember(latest, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) latest
        else latest.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UthBackground),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner UTH
        item {
            HomeHeaderBanner(onNavigateToChat = onNavigateToChat)
        }

        // 2. Danh mục Nhanh (Filter Chips)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Danh mục tài liệu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = UthNavy
                    )
                    if (selectedCategoryFilter != null) {
                        Text(
                            text = "Xóa lọc",
                            fontSize = 12.sp,
                            color = UthBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { selectedCategoryFilter = null }
                                .padding(4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        CategoryChip(
                            label = "Tất cả",
                            isSelected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null }
                        )
                    }
                    items(Category.entries.toTypedArray()) { category ->
                        CategoryChip(
                            label = category.displayName,
                            isSelected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = category }
                        )
                    }
                }
            }
        }

        // 3. Mục "Tiếp tục học" (Carousel Card Ngang)
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = UthTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tiếp tục học",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = UthNavy
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = UthTeal.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${recent.size}",
                            color = UthTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (recent.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "Bạn chưa có tài liệu nào đang đọc dở. Hãy bắt đầu học ngay nhé!",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recent, key = { "recent_${it.id}" }) { doc ->
                            InProgessDocumentCard(
                                document = doc,
                                isBookmarked = doc.id in bookmarkedIds,
                                onBookmarkClick = { bookmarkViewModel.toggle(doc.id) },
                                onClick = { onDocumentClick(doc.id) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Mục "Tài liệu mới nhất" (Danh sách Dọc)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedCategoryFilter == null) "Tài liệu mới nhất" else "Kho tài liệu ${selectedCategoryFilter?.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = UthNavy
                    )
                }
            }
        }

        if (filteredLatest.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Không tìm thấy tài liệu trong danh mục này.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredLatest, key = { "latest_${it.id}" }) { doc ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DocumentItem(
                        document = doc,
                        isBookmarked = doc.id in bookmarkedIds,
                        onBookmarkClick = { bookmarkViewModel.toggle(doc.id) },
                        onClick = { onDocumentClick(doc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderBanner(
    onNavigateToChat: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(UthNavy, UthTeal)
                )
            )
            .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Xin chào Sinh viên UTH 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Hệ thống quản lý & học tập tài liệu UTH",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onNavigateToChat) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Trò chuyện",
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search Banner Mockup
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Tìm kiếm bài giảng, đề thi, giáo trình UTH...",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) UthTeal else Color.White,
        shadowElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else UthNavy,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun InProgessDocumentCard(
    document: Document,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (document.type) {
                        DocumentType.PDF -> Color(0xFFFFEBEE)
                        DocumentType.DOCX -> Color(0xFFE3F2FD)
                        DocumentType.PPTX -> Color(0xFFFFF3E0)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (document.type) {
                                DocumentType.PDF -> Icons.Default.PictureAsPdf
                                DocumentType.DOCX -> Icons.Default.Description
                                DocumentType.PPTX -> Icons.Default.Slideshow
                            },
                            contentDescription = null,
                            tint = when (document.type) {
                                DocumentType.PDF -> Color(0xFFD32F2F)
                                DocumentType.DOCX -> Color(0xFF1976D2)
                                DocumentType.PPTX -> Color(0xFFE65100)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = document.type.extension,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (document.type) {
                                DocumentType.PDF -> Color(0xFFD32F2F)
                                DocumentType.DOCX -> Color(0xFF1976D2)
                                DocumentType.PPTX -> Color(0xFFE65100)
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) UthTeal else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = document.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = UthNavy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.category.displayName,
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Đã học ${(document.progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UthTeal
                )
                Text(
                    text = "${"%.1f".format(document.sizeInMb)} MB",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { document.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = UthTeal,
                trackColor = UthTeal.copy(alpha = 0.15f)
            )
        }
    }
}

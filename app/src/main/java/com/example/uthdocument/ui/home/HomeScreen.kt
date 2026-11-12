package com.example.uthdocument.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.ui.components.DocumentItem

@Composable
fun HomeScreen(
    onDocumentClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val recent = viewModel.recentDocuments.collectAsState().value
    val latest = viewModel.latestDocuments.collectAsState().value
    val bookmarkedIds = bookmarkViewModel.bookmarkedIds.collectAsState().value

    if (recent == null || latest == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // bottom 88dp để item cuối không bị FAB/thanh điều hướng che
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Trang chủ", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            Text(text = "Tiếp tục học", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (recent.isEmpty()) {
            item { Text(text = "Bạn chưa đọc tài liệu nào.", color = Color.Gray) }
        } else {
            // key có tiền tố vì cùng một tài liệu có thể xuất hiện ở cả 2 danh sách
            items(recent, key = { "recent_${it.id}" }) { doc ->
                DocumentItem(
                    document = doc,
                    isBookmarked = doc.id in bookmarkedIds,
                    onBookmarkClick = { bookmarkViewModel.toggle(doc.id) },
                    onClick = { onDocumentClick(doc.id) }
                )
            }
        }

        item {
            Text(text = "Tài liệu mới nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (latest.isEmpty()) {
            item { Text(text = "Chưa có tài liệu.", color = Color.Gray) }
        } else {
            items(latest, key = { "latest_${it.id}" }) { doc ->
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

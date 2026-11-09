package com.example.uthdocument.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.ui.components.DocumentItem

@Composable
fun LibraryScreen(
    onDocumentClick: (String) -> Unit = {},
    viewModel: LibraryViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val documents = viewModel.documents.collectAsState().value
    val query = viewModel.query.collectAsState().value
    val selected = viewModel.selectedCategory.collectAsState().value
    val onlyBookmarked = viewModel.onlyBookmarked.collectAsState().value
    val bookmarkedIds = bookmarkViewModel.bookmarkedIds.collectAsState().value
    val shown = if (onlyBookmarked && documents != null) {
        documents.filter { it.id in bookmarkedIds }
    } else {
        documents
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Thư viện tài liệu",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Tìm tài liệu...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selected == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("Tất cả") }
                )
            }
            item {
                FilterChip(
                    selected = onlyBookmarked,
                    onClick = viewModel::toggleOnlyBookmarked,
                    label = { Text("Đã lưu") },
                    leadingIcon = {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                    }
                )
            }
            items(Category.entries) { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category.displayName) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                shown == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                shown.isEmpty() -> Text(
                    text = if (onlyBookmarked) "Bạn chưa lưu tài liệu nào." else "Không tìm thấy tài liệu phù hợp.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown, key = { it.id }) { doc ->
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
}

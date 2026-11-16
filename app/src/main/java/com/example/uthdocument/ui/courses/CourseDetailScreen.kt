package com.example.uthdocument.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.ui.components.DocumentItem
import com.example.uthdocument.ui.theme.UthBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    onDocumentClick: (String) -> Unit = {},
    viewModel: CourseDetailViewModel = viewModel(),
    followViewModel: CourseFollowViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    LaunchedEffect(courseId) { viewModel.load(courseId) }
    val state = viewModel.uiState.collectAsState().value
    val isFollowed = courseId in followViewModel.followedIds.collectAsState().value
    val bookmarkedIds = bookmarkViewModel.bookmarkedIds.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? CourseDetailUiState.Success)?.course?.name ?: "Môn học",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state is CourseDetailUiState.Success) {
                        IconButton(onClick = { followViewModel.toggle(courseId) }) {
                            Icon(
                                imageVector = if (isFollowed) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isFollowed) "Bỏ theo dõi" else "Theo dõi môn",
                                tint = if (isFollowed) UthBlue else Color.Gray
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                CourseDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                CourseDetailUiState.NotFound -> Text(
                    text = "Không tìm thấy môn học.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )

                is CourseDetailUiState.Success -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "${state.course.code} • ${state.course.category.displayName} • ${state.documents.size} tài liệu",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    if (state.documents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Chưa có tài liệu cho môn này.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.documents, key = { it.id }) { doc ->
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
    }
}

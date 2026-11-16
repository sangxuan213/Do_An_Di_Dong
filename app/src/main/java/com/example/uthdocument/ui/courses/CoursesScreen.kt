package com.example.uthdocument.ui.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.ui.theme.UthBlue

@Composable
fun CoursesScreen(
    onCourseClick: (String) -> Unit = {},
    viewModel: CoursesViewModel = viewModel(),
    followViewModel: CourseFollowViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val query = viewModel.query.collectAsState().value
    val onlyFollowed = viewModel.onlyFollowed.collectAsState().value
    val followedIds = followViewModel.followedIds.collectAsState().value

    val shown = if (onlyFollowed && state != null) {
        state.courses.filter { it.id in followedIds }
    } else {
        state?.courses
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Môn học",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Tìm môn học hoặc mã môn...") },
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

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !onlyFollowed,
                onClick = { viewModel.setOnlyFollowed(false) },
                label = { Text("Tất cả") }
            )
            FilterChip(
                selected = onlyFollowed,
                onClick = { viewModel.setOnlyFollowed(true) },
                label = { Text("Đang theo dõi") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (state == null || shown == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (shown.isEmpty()) {
                Text(
                    text = if (onlyFollowed) "Bạn chưa theo dõi môn nào." else "Không tìm thấy môn học phù hợp.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown, key = { it.id }) { course ->
                        CourseCard(
                            course = course,
                            documentCount = state.documentCounts[course.id] ?: 0,
                            isFollowed = course.id in followedIds,
                            onToggleFollow = { followViewModel.toggle(course.id) },
                            onClick = { onCourseClick(course.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    documentCount: Int,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = course.code, color = UthBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = course.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${course.category.displayName} • $documentCount tài liệu",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onToggleFollow) {
                Icon(
                    imageVector = if (isFollowed) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFollowed) "Bỏ theo dõi" else "Theo dõi môn",
                    tint = if (isFollowed) UthBlue else Color.Gray
                )
            }
        }
    }
}

package com.example.uthdocument.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.AppNotification
import com.example.uthdocument.domain.model.NotificationType
import com.example.uthdocument.ui.theme.UthBlue
import com.example.uthdocument.ui.util.formatTimeAgo

/** Màn hình của chuông ở trang chủ: bài mới của người đang theo dõi và tin nhắn chưa đọc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val items = viewModel.notifications.collectAsState().value
    // Chụp mốc "đã xem" lúc vừa vào để các dòng mới vẫn được tô sáng trong lúc đang xem
    val seenAtOnEnter = remember { viewModel.seenAt.value }

    DisposableEffect(Unit) {
        onDispose { viewModel.markAllSeen() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (items.isEmpty()) {
                Text(
                    text = "Chưa có thông báo nào.\nHãy theo dõi bạn bè để biết khi họ đăng tài liệu mới.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { n ->
                        NotificationRow(
                            notification = n,
                            isNew = n.time > seenAtOnEnter,
                            onClick = {
                                when (n.type) {
                                    NotificationType.NEW_DOCUMENT -> onDocumentClick(n.targetId)
                                    NotificationType.MESSAGE -> onChatClick(n.targetId)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, isNew: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isNew) Color(0xFFE6EDFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(UthBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (notification.type == NotificationType.MESSAGE) Icons.AutoMirrored.Filled.Chat
                else Icons.Default.Description,
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                fontWeight = if (isNew) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification.text,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (notification.time > 0) {
                Text(text = formatTimeAgo(notification.time), fontSize = 12.sp, color = UthBlue)
            }
        }
    }
}

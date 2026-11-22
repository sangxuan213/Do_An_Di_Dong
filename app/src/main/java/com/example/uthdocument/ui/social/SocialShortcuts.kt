package com.example.uthdocument.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.ui.chat.UnreadMessagesViewModel

/** Các lối tắt ở màn Cá nhân: tài liệu của tôi, đã tải về, tin nhắn (có huy hiệu số tin chưa đọc). */
@Composable
fun SocialShortcuts(
    onMyDocuments: () -> Unit,
    onDownloads: () -> Unit,
    onMessages: () -> Unit,
    unreadViewModel: UnreadMessagesViewModel = viewModel()
) {
    val unread = unreadViewModel.unreadCount.collectAsState().value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ShortcutRow(title = "Tài liệu của tôi", onClick = onMyDocuments)
        HorizontalDivider()
        ShortcutRow(title = "Đã tải về", onClick = onDownloads)
        HorizontalDivider()
        ShortcutRow(title = "Tin nhắn", badge = unread, onClick = onMessages)
    }
}

@Composable
private fun ShortcutRow(title: String, onClick: () -> Unit, badge: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        if (badge > 0) {
            Text(
                text = if (badge > 9) "9+" else "$badge",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE5484D))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

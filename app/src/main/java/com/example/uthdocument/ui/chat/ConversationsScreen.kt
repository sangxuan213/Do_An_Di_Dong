package com.example.uthdocument.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Conversation
import com.example.uthdocument.ui.theme.UthBlue
import com.example.uthdocument.ui.util.formatTimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ConversationsViewModel = viewModel(),
    chatViewModel: RepositoryChatViewModel = viewModel()
) {
    val conversations = viewModel.conversations.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                conversations == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                conversations.isEmpty() -> Text(
                    text = "Chưa có cuộc trò chuyện nào.\nMở trang cá nhân của người đăng tài liệu để nhắn tin cho họ.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            myId = chatViewModel.myId,
                            onClick = { onOpenChat(conversation.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, myId: String?, onClick: () -> Unit) {
    val initial = conversation.otherName.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"
    val preview = (if (conversation.lastSenderId == myId) "Bạn: " else "") + conversation.lastMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(UthBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.otherName,
                fontWeight = if (conversation.unread) FontWeight.Bold else FontWeight.SemiBold
            )
            Text(
                text = preview,
                fontSize = 13.sp,
                color = if (conversation.unread) Color.Black else Color.Gray,
                fontWeight = if (conversation.unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (conversation.updatedAt > 0) {
                Text(formatTimeAgo(conversation.updatedAt), fontSize = 11.sp, color = Color.Gray)
            }
            if (conversation.unread) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(UthBlue))
            }
        }
    }
}

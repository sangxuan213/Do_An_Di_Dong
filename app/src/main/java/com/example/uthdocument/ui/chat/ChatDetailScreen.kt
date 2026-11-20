package com.example.uthdocument.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UthNavy = Color(0xFF002D58)
private val UthTeal = Color(0xFF007B7A)
private val GoogleMessageSenderBg = Color(0xFF007B7A)
private val GoogleMessageReceiverBg = Color(0xFFE9EEF6)
private val GoogleChatBackground = Color(0xFFF3F6FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    recipientUid: String,
    recipientName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val targetRecipientUid = remember(recipientUid) {
        recipientUid.ifBlank { "admin_uth_system" }
    }
    val targetRecipientName = remember(recipientName) {
        recipientName.ifBlank { "Giảng viên / Admin UTH" }
    }
    val effectiveChatId = remember(chatId, targetRecipientUid) {
        if (chatId.isNotBlank()) chatId else ChatViewModel.getChatId(viewModel.currentUid, targetRecipientUid)
    }

    val messagesFlow = remember(effectiveChatId) { viewModel.getMessages(effectiveChatId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Cuộn tự động xuống tin nhắn mới nhất
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            // Chấm xanh lá báo Đang hoạt động kiểu Google Messages
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = targetRecipientName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Đang hoạt động trên UTH",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Gọi Video", tint = Color.White)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Call, contentDescription = "Gọi thoại", tint = Color.White)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UthNavy)
            )
        },
        bottomBar = {
            GoogleMessagesInputBar(
                text = textInput,
                onTextChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendMessage(
                            chatId = effectiveChatId,
                            recipientUid = targetRecipientUid,
                            recipientName = targetRecipientName,
                            text = textInput
                        )
                        textInput = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GoogleChatBackground)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Cuộc trò chuyện mã hóa 1-1",
                                fontWeight = FontWeight.Bold,
                                color = UthNavy,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gửi tin nhắn đầu tiên cho $targetRecipientName.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Badge thời gian ở trên cùng
                    item {
                        DateBadge(timestamp = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis())
                    }

                    itemsIndexed(messages, key = { _, msg -> msg.id.ifBlank { msg.timestamp.toString() } }) { index, msg ->
                        GoogleMessageBubble(
                            message = msg,
                            isMe = msg.senderId == viewModel.currentUid || (viewModel.currentUid.isEmpty() && msg.senderName.contains("Sinh viên")),
                            showName = index == 0 || messages.getOrNull(index - 1)?.senderId != msg.senderId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBadge(timestamp: Long) {
    val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
    val dateText = dateFormat.format(Date(timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun GoogleMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showName: Boolean
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isMe && showName && message.senderName.isNotBlank()) {
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = UthNavy,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMe) 20.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 20.dp
            ),
            color = if (isMe) GoogleMessageSenderBg else GoogleMessageReceiverBg,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    color = if (isMe) Color.White else Color(0xFF1F1F1F)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else Color.Gray
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Đã gửi",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMessagesInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "Đính kèm",
                    tint = UthTeal
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Trò chuyện (Tin nhắn SMS/UTH)...", fontSize = 13.5.sp) },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = UthTeal,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedContainerColor = Color.White
                ),
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .background(if (text.isNotBlank()) UthTeal else Color(0xFFE0E0E0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = Color.White
                )
            }
        }
    }
}

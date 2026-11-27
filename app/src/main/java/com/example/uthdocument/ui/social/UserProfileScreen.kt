package com.example.uthdocument.ui.social

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.ui.components.DocumentItem
import com.example.uthdocument.ui.theme.UthBlue
import com.example.uthdocument.ui.util.formatTimeAgo

/** Trang cá nhân công khai của người đăng: thông tin, nút nhắn tin và các tài liệu họ đã đăng. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: UserProfileViewModel = viewModel()
) {
    LaunchedEffect(uid) { viewModel.load(uid) }
    val profile = viewModel.profile.collectAsState().value
    val loaded = viewModel.loaded.collectAsState().value
    val documents = viewModel.documents.collectAsState().value
    val error = viewModel.error.collectAsState().value
    val isFollowing = viewModel.isFollowing.collectAsState().value
    val followerCount = viewModel.followerCount.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (profile == null) {
                        Text("Không tìm thấy thông tin người dùng này.", color = Color.Gray)
                    } else {
                        val initial = profile.fullName.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(UthBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(profile.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile.role.displayName,
                            color = UthBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFE6EDFF))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        val detail = listOf(profile.faculty, profile.className).filter { it.isNotBlank() }.joinToString(" • ")
                        if (detail.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(detail, color = Color.Gray, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("$followerCount người theo dõi", color = Color.Gray, fontSize = 13.sp)
                        if (!viewModel.isMe) {
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isFollowing) {
                                    OutlinedButton(onClick = viewModel::toggleFollow) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Đang theo dõi")
                                    }
                                } else {
                                    Button(
                                        onClick = viewModel::toggleFollow,
                                        colors = ButtonDefaults.buttonColors(containerColor = UthBlue)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Theo dõi")
                                    }
                                }
                                OutlinedButton(onClick = { viewModel.startChat(onOpenChat) }) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Nhắn tin")
                                }
                            }
                        }
                        if (error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Tài liệu đã đăng" + if (documents != null) " (${documents.size})" else "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (documents == null) {
                item { CircularProgressIndicator() }
            } else if (documents.isEmpty()) {
                item { Text("Chưa đăng tài liệu nào.", color = Color.Gray) }
            } else {
                items(documents, key = { it.id }) { doc ->
                    DocumentItem(
                        document = doc,
                        extraInfo = "${doc.viewCount} lượt xem" +
                            if (doc.uploadedAt > 0) " • ${formatTimeAgo(doc.uploadedAt)}" else "",
                        onClick = { onDocumentClick(doc.id) }
                    )
                }
            }
        }
    }
}

/** Dòng "Người đăng" ở trang chi tiết tài liệu: hiện tên thật, bấm vào để xem trang cá nhân của họ. */
@Composable
fun UploaderRow(
    uploaderId: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFFE8F1FF))

    if (uploaderId == null) {
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF002D58), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Người đăng: không rõ", fontSize = 12.sp, color = Color(0xFF002D58))
        }
        return
    }

    val viewModel: UserProfileViewModel = viewModel(key = "uploader_$uploaderId")
    LaunchedEffect(uploaderId) { viewModel.load(uploaderId) }
    val profile = viewModel.profile.collectAsState().value
    val loaded = viewModel.loaded.collectAsState().value

    Row(
        modifier = rowModifier
            .clickable { onClick(uploaderId) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF002D58), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        val label = when {
            profile != null -> "Người đăng: ${profile.fullName} • ${profile.role.displayName}"
            loaded -> "Người đăng: người dùng UTH"
            else -> "Người đăng: ..."
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF002D58),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF002D58))
    }
}

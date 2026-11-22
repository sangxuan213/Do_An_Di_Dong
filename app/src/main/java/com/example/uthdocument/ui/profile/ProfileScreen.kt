package com.example.uthdocument.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.UserProfile
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.ui.social.SocialShortcuts
import com.example.uthdocument.ui.theme.UthBlue

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToMyDocuments: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val profile = viewModel.profile.collectAsState().value
    val bookmarkCount = bookmarkViewModel.bookmarkedIds.collectAsState().value.size

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var notificationsOn by rememberSaveable { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(profile)

        Spacer(modifier = Modifier.height(24.dp))

        SectionCard {
            InfoRow("Mã số", profile.studentId.ifBlank { "Chưa cập nhật" })
            HorizontalDivider()
            InfoRow("Khoa", profile.faculty.ifBlank { "Chưa cập nhật" })
            HorizontalDivider()
            InfoRow("Lớp", profile.className.ifBlank { "Chưa cập nhật" })
            HorizontalDivider()
            InfoRow("Tài liệu đã lưu", "$bookmarkCount")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SocialShortcuts(
            onMyDocuments = onNavigateToMyDocuments,
            onDownloads = onNavigateToDownloads,
            onMessages = onNavigateToMessages
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Thông báo văn bản mới", modifier = Modifier.weight(1f))
                // TODO (phối hợp Bạn B): nối với Firebase Cloud Messaging và lưu lựa chọn
                Switch(checked = notificationsOn, onCheckedChange = { notificationsOn = it })
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPasswordDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Đổi mật khẩu", modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
            }
            if (profile.role == UserRole.ADMIN) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAdmin() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛠️ Trang quản trị Admin",
                        fontWeight = FontWeight.SemiBold,
                        color = UthBlue,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = UthBlue)
                }
            }
            HorizontalDivider()
            InfoRow("Phiên bản", "1.0")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng xuất", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            state = viewModel.passwordState.collectAsState().value,
            onConfirm = viewModel::changePassword,
            onDismiss = {
                showPasswordDialog = false
                viewModel.resetPasswordState()
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc muốn đăng xuất khỏi tài khoản?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun Header(profile: UserProfile) {
    val initial = profile.fullName.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(UthBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initial, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = profile.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
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
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = profile.email, color = Color.Gray, fontSize = 13.sp)
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(verticalArrangement = Arrangement.Center) { content() }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChangePasswordDialog(
    state: PasswordChangeUiState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "Đã đổi mật khẩu", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; localError = null },
                    label = { Text("Mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = null },
                    label = { Text("Nhập lại mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                val error = localError ?: state.error
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.isLoading,
                onClick = {
                    when {
                        newPassword.length < 6 -> localError = "Mật khẩu phải có ít nhất 6 ký tự."
                        newPassword != confirmPassword -> localError = "Hai mật khẩu không khớp."
                        else -> onConfirm(newPassword)
                    }
                }
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Đổi")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isLoading) { Text("Hủy") } }
    )
}

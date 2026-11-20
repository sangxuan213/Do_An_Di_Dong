package com.example.uthdocument.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsState().value

    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    // Đăng nhập thành công thì chuyển sang màn hình chính
    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoginSuccess() }

    // Sử dụng màu trích xuất từ logo UTH (bạn có thể thay đổi sau trong theme)
    val uthTeal = Color(0xFF007B7A)
    val uthNavy = Color(0xFF002D58)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_uth),
            contentDescription = "UTH Documents Logo",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "ĐĂNG NHẬP", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = uthNavy)
        Text(text = "Hệ thống quản lý tài liệu UTH", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // Ô nhập Mã sinh viên / Cán bộ (hoặc email)
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Mã số hoặc email") },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "User Icon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô nhập Mật khẩu
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.login(identifier, password) })
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = state.error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Đăng nhập
        Button(
            onClick = { viewModel.login(identifier, password) },
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = uthTeal)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(text = "Đăng Nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút Quên mật khẩu
        TextButton(onClick = { showResetDialog = true }) {
            Text(text = "Quên mật khẩu?", color = uthTeal)
        }

        // Nút chuyển sang màn hình Đăng ký
        TextButton(onClick = onRegisterClick) {
            Text(text = "Chưa có tài khoản? Đăng ký", color = uthTeal)
        }
    }

    if (showResetDialog) {
        ResetPasswordDialog(
            initialValue = identifier,
            isSending = state.isSendingReset,
            message = state.resetMessage,
            isSuccess = state.resetSuccess,
            onSend = viewModel::sendPasswordReset,
            onDismiss = {
                showResetDialog = false
                viewModel.clearResetMessage()
            }
        )
    }
}

@Composable
private fun ResetPasswordDialog(
    initialValue: String,
    isSending: Boolean,
    message: String?,
    isSuccess: Boolean,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quên mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nhập mã số hoặc email, hệ thống sẽ gửi liên kết đặt lại mật khẩu vào email của bạn.", fontSize = 13.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Mã số hoặc email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null) {
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        color = if (isSuccess) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(value) }, enabled = !isSending && !isSuccess) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Gửi")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

package com.example.uthdocument.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uthdocument.R

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Sử dụng màu trích xuất từ logo UTH (bạn có thể thay đổi sau trong theme)
    val uthTeal = Color(0xFF007B7A) 
    val uthNavy = Color(0xFF002D58)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        // Lưu ý: Đảm bảo bạn đã thêm file logo_uth vào thư mục res/drawable
        Image(
            painter = painterResource(id = R.drawable.logo_uth), 
            contentDescription = "UTH Documents Logo",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ĐĂNG NHẬP",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = uthNavy
        )
        
        Text(
            text = "Hệ thống quản lý tài liệu UTH",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Ô nhập Mã sinh viên / Cán bộ
        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("Mã sinh viên / Mã cán bộ") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = "User Icon")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô nhập Mật khẩu
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Đăng nhập
        Button(
            onClick = { onLoginClick(studentId, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = uthTeal)
        ) {
            Text(text = "Đăng Nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút Quên mật khẩu
        TextButton(onClick = { /* TODO: Chức năng quên mật khẩu */ }) {
            Text(text = "Quên mật khẩu?", color = uthTeal)
        }
    }
}

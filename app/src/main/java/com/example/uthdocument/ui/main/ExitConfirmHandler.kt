package com.example.uthdocument.ui.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Đặt vào màn hình gốc (Chính, Đăng nhập): bấm Back thì hỏi "Bạn muốn thoát ứng dụng?" thay vì thoát ngay.
 * Chọn "Thoát" để đóng app, "Ở lại" để tiếp tục dùng.
 */
@Composable
fun ExitConfirmHandler() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    BackHandler { showDialog = true }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Thoát ứng dụng") },
            text = { Text("Bạn muốn thoát ứng dụng?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    context.findActivity()?.finish()
                }) { Text("Thoát") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Ở lại") }
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

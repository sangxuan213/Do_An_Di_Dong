package com.example.uthdocument.ui.main

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.uthdocument.data.AppContainer

/** Nếu tài khoản đang đăng nhập đã bị admin khóa thì đăng xuất ngay và quay về màn hình đăng nhập. */
@Composable
fun AccountLockGuard(onLocked: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val session = AppContainer.sessionRepository
        if (session.isAccountLocked()) {
            session.logout()
            Toast.makeText(context, "Tài khoản của bạn đã bị khóa. Hãy liên hệ quản trị viên.", Toast.LENGTH_LONG).show()
            onLocked()
        }
    }
}

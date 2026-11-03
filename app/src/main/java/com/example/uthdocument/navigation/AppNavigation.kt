package com.example.uthdocument.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uthdocument.ui.login.LoginScreen
import com.example.uthdocument.ui.main.MainScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        
        // Màn hình Đăng nhập
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { studentId, password ->
                    // TODO: Gọi API kiểm tra đăng nhập thực tế ở đây
                    // Giả lập đăng nhập thành công
                    if (studentId.isNotBlank() && password.isNotBlank()) {
                        navController.navigate(Routes.MAIN) {
                            // Xóa lịch sử điều hướng, không cho user bấm Back quay lại trang Login
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Màn hình Chính (chứa Bottom Navigation)
        composable(Routes.MAIN) {
            MainScreen()
        }
    }
}

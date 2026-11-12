package com.example.uthdocument.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uthdocument.ui.add.AddDocumentScreen
import com.example.uthdocument.ui.detail.DocumentDetailScreen
import com.example.uthdocument.ui.login.LoginScreen
import com.example.uthdocument.ui.main.MainScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val DOCUMENT = "document/{id}"
    const val ADD_DOCUMENT = "add_document"

    fun document(id: String) = "document/$id"
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
            MainScreen(
                onDocumentClick = { id -> navController.navigate(Routes.document(id)) },
                onAddDocumentClick = { navController.navigate(Routes.ADD_DOCUMENT) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình thêm tài liệu (nút FAB)
        composable(Routes.ADD_DOCUMENT) {
            AddDocumentScreen(onBack = { navController.popBackStack() })
        }

        // Màn hình chi tiết / xem tài liệu
        composable(
            route = Routes.DOCUMENT,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            DocumentDetailScreen(
                documentId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

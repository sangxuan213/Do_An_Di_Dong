package com.example.uthdocument.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.ui.add.AddDocumentScreen
import com.example.uthdocument.ui.admin.AdminScreen
import com.example.uthdocument.ui.chat.ChatDetailScreen
import com.example.uthdocument.ui.chat.ChatListScreen
import com.example.uthdocument.ui.courses.CourseDetailScreen
import com.example.uthdocument.ui.detail.DocumentDetailScreen
import com.example.uthdocument.ui.login.LoginScreen
import com.example.uthdocument.ui.login.RegisterScreen
import com.example.uthdocument.ui.main.MainScreen
import com.example.uthdocument.ui.mydocuments.MyDocumentsScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val ADMIN = "admin"
    const val CHAT_LIST = "chat_list"
    const val CHAT_DETAIL = "chat_detail?chatId={chatId}&recipientUid={recipientUid}&recipientName={recipientName}"
    const val MY_DOCUMENTS = "my_documents"
    const val DOCUMENT = "document/{id}"
    const val ADD_DOCUMENT = "add_document"
    const val COURSE = "course/{id}"

    fun document(id: String) = "document/$id"
    fun course(id: String) = "course/$id"
    fun chatDetail(chatId: String = "", recipientUid: String = "", recipientName: String = "") =
        "chat_detail?chatId=$chatId&recipientUid=$recipientUid&recipientName=$recipientName"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    // Đã đăng nhập từ lần trước thì vào thẳng màn hình chính
    val startDestination = remember {
        if (AppContainer.sessionRepository.isLoggedIn()) Routes.MAIN else Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        
        // Màn hình Đăng nhập
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        // Xóa lịch sử điều hướng, không cho user bấm Back quay lại trang Login
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        // Màn hình Đăng ký (tạo xong tự đăng nhập và vào màn hình chính)
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // Màn hình Chính (chứa Bottom Navigation)
        composable(Routes.MAIN) {
            MainScreen(
                onDocumentClick = { id -> navController.navigate(Routes.document(id)) },
                onAddDocumentClick = { navController.navigate(Routes.ADD_DOCUMENT) },
                onCourseClick = { id -> navController.navigate(Routes.course(id)) },
                onNavigateToAdmin = { navController.navigate(Routes.ADMIN) },
                onNavigateToChat = { navController.navigate(Routes.CHAT_LIST) },
                onNavigateToMyDocuments = { navController.navigate(Routes.MY_DOCUMENTS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình tài liệu của tôi
        composable(Routes.MY_DOCUMENTS) {
            MyDocumentsScreen(
                onBack = { navController.popBackStack() },
                onDocumentClick = { id -> navController.navigate(Routes.document(id)) }
            )
        }

        // Màn hình danh sách nhắn tin
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { chatId, recipientUid, recipientName ->
                    navController.navigate(Routes.chatDetail(chatId, recipientUid, recipientName))
                }
            )
        }

        // Màn hình chi tiết nhắn tin 1-1
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType; defaultValue = "" },
                navArgument("recipientUid") { type = NavType.StringType; defaultValue = "" },
                navArgument("recipientName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            ChatDetailScreen(
                chatId = entry.arguments?.getString("chatId").orEmpty(),
                recipientUid = entry.arguments?.getString("recipientUid").orEmpty(),
                recipientName = entry.arguments?.getString("recipientName").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        // Màn hình Quản trị Admin
        composable(Routes.ADMIN) {
            AdminScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAddDocument = { navController.navigate(Routes.ADD_DOCUMENT) }
            )
        }

        // Màn hình thêm tài liệu (nút FAB)
        composable(Routes.ADD_DOCUMENT) {
            AddDocumentScreen(onBack = { navController.popBackStack() })
        }

        // Màn hình chi tiết môn học (danh sách tài liệu của môn)
        composable(
            route = Routes.COURSE,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            CourseDetailScreen(
                courseId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                onDocumentClick = { id -> navController.navigate(Routes.document(id)) }
            )
        }

        // Màn hình chi tiết / xem tài liệu
        composable(
            route = Routes.DOCUMENT,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            DocumentDetailScreen(
                documentId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenChat = { recipientUid, recipientName ->
                    navController.navigate(Routes.chatDetail("", recipientUid, recipientName))
                }
            )
        }
    }
}

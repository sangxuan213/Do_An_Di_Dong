package com.example.uthdocument

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.fcm.ChatNotifier
import com.example.uthdocument.fcm.FollowNotificationWorker
import com.example.uthdocument.navigation.AppNavigation
import com.example.uthdocument.navigation.Routes
import com.example.uthdocument.ui.theme.UTHDocumentTheme

class MainActivity : ComponentActivity() {

    /** Màn hình cần mở khi người dùng bấm vào thông báo (null = không có). */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)
        FollowNotificationWorker.schedule(applicationContext)
        ChatNotifier.start(applicationContext)
        pendingRoute = routeFrom(intent)
        enableEdgeToEdge()
        setContent {
            UTHDocumentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        pendingRoute = pendingRoute,
                        onPendingRouteHandled = { pendingRoute = null }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ChatNotifier.appVisible = true
    }

    override fun onStop() {
        ChatNotifier.appVisible = false
        super.onStop()
    }

    // App đang mở sẵn mà bấm thông báo thì vào đây (manifest dùng launchMode="singleTop")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeFrom(intent)?.let { pendingRoute = it }
    }

    private fun routeFrom(intent: Intent?): String? {
        val chatId = intent?.getStringExtra(ChatNotifier.EXTRA_CHAT_ID)?.takeIf { it.isNotBlank() }
        if (chatId != null) return Routes.chat(chatId)
        val documentId = intent?.getStringExtra(FollowNotificationWorker.EXTRA_DOCUMENT_ID)?.takeIf { it.isNotBlank() }
        if (documentId != null) return Routes.document(documentId)
        return null
    }
}

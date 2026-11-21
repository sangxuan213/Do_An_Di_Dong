package com.example.uthdocument.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.ui.components.DocumentItem
import com.example.uthdocument.ui.theme.UthBlue
import com.example.uthdocument.ui.util.formatTimeAgo

/** Danh sách tài liệu mình đã đăng, kèm lượt xem và nút xóa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDocumentsScreen(
    onBack: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onAddDocument: () -> Unit,
    viewModel: MyDocumentsViewModel = viewModel()
) {
    val documents = viewModel.documents.collectAsState().value
    val error = viewModel.error.collectAsState().value
    var pendingDelete by remember { mutableStateOf<Document?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tài liệu của tôi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                documents == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                documents.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Bạn chưa đăng tài liệu nào.", color = Color.Gray)
                    Button(onClick = onAddDocument, colors = ButtonDefaults.buttonColors(containerColor = UthBlue)) {
                        Text("Đăng tài liệu")
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${documents.size} tài liệu • ${documents.sumOf { it.viewCount }} lượt xem",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        if (error != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::clearError) { Text("Đóng") }
                            }
                        }
                    }
                    items(documents, key = { it.id }) { doc ->
                        Column {
                            DocumentItem(
                                document = doc,
                                extraInfo = "${doc.viewCount} lượt xem" +
                                    if (doc.uploadedAt > 0) " • đăng ${formatTimeAgo(doc.uploadedAt)}" else "",
                                onClick = { onDocumentClick(doc.id) }
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { pendingDelete = doc }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa tài liệu") },
            text = { Text("Xóa \"${doc.title}\"? Mọi người sẽ không còn thấy tài liệu này và không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(doc.id)
                    pendingDelete = null
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Hủy") } }
        )
    }
}

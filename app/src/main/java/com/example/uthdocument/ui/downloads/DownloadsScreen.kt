package com.example.uthdocument.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
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
import com.example.uthdocument.ui.util.formatBytes

/** Các tài liệu đã tải về máy: đọc offline, lưu ra thư mục Tải xuống, hoặc xóa để giải phóng bộ nhớ. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onDocumentClick: (String) -> Unit,
    viewModel: DownloadsViewModel = viewModel()
) {
    val ui = viewModel.ui.collectAsState().value
    val exporter = rememberDocumentExporter()
    var pendingRemove by remember { mutableStateOf<Document?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đã tải về") },
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
                ui == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                ui.documents.isEmpty() -> Text(
                    text = "Chưa có tài liệu nào được tải về.\nMở một tài liệu để tải về đọc khi không có mạng.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${ui.documents.size} tài liệu • chiếm ${formatBytes(ui.totalBytes)} trên máy",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    items(ui.documents, key = { it.id }) { doc ->
                        Column {
                            DocumentItem(
                                document = doc,
                                extraInfo = "Trên máy: ${formatBytes(ui.sizes[doc.id] ?: 0L)}",
                                onClick = { onDocumentClick(doc.id) }
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = { exporter.export(doc) },
                                    enabled = exporter.workingId != doc.id
                                ) {
                                    if (exporter.workingId == doc.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lưu vào Tải xuống")
                                }
                                TextButton(onClick = { pendingRemove = doc }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Xóa khỏi máy", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRemove?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("Xóa bản tải về") },
            text = {
                Text(
                    "Xóa bản tải về của \"${doc.title}\" khỏi máy? Tài liệu vẫn còn trên hệ thống và tải lại được " +
                        "khi có mạng. Tài liệu không có bản trực tuyến sẽ không tải lại được."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(doc.id)
                    pendingRemove = null
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text("Hủy") } }
        )
    }
}

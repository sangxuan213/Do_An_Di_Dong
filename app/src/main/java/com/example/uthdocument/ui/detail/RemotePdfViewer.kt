package com.example.uthdocument.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.ui.theme.UthBlue

/** Tải PDF từ Firebase Storage về máy (có thanh tiến độ) rồi hiển thị bằng PdfViewer. */
@Composable
fun RemotePdfViewer(
    document: Document,
    modifier: Modifier = Modifier,
    viewModel: DocumentDownloadViewModel = viewModel()
) {
    LaunchedEffect(document.id) { viewModel.load(document) }
    val state = viewModel.state.collectAsState().value

    when (state) {
        is DownloadUiState.Ready -> PdfViewer(file = state.file, modifier = modifier, documentId = document.id)

        is DownloadUiState.Downloading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                val progress = state.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = UthBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "Đang tải tài liệu... ${(progress * 100).toInt()}%", color = Color.Gray)
                } else {
                    CircularProgressIndicator()
                    Text(text = "Đang tải tài liệu...", color = Color.Gray)
                }
            }
        }

        is DownloadUiState.Error -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Button(onClick = viewModel::retry) { Text("Thử lại") }
            }
        }
    }
}

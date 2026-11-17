package com.example.uthdocument.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import com.example.uthdocument.domain.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBack: () -> Unit,
    viewModel: DocumentDetailViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    interactionViewModel: DocumentInteractionViewModel = viewModel()
) {
    LaunchedEffect(documentId) {
        viewModel.load(documentId)
        interactionViewModel.load(documentId)
    }
    val state = viewModel.uiState.collectAsState().value
    val isBookmarked = documentId in bookmarkViewModel.bookmarkedIds.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? DetailUiState.Success)?.document?.title ?: "Tài liệu",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state is DetailUiState.Success) {
                        IconButton(onClick = { bookmarkViewModel.toggle(documentId) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Bỏ lưu" else "Lưu tài liệu"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                DetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailUiState.NotFound -> Text(
                    text = "Không tìm thấy tài liệu.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
                is DetailUiState.Success -> DocumentContent(state.document, interactionViewModel)
            }
        }
    }
}

@Composable
private fun DocumentContent(
    document: Document,
    interactionViewModel: DocumentInteractionViewModel
) {
    val stats = interactionViewModel.stats.collectAsState().value
    val comments = interactionViewModel.comments.collectAsState().value
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${document.type.extension} • ${"%.1f".format(document.sizeInMb)} MB • ${document.category.displayName}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        DocumentActionBar(stats = stats, onToggleLike = interactionViewModel::toggleLike)

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Nội dung") }
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Nhận xét (${comments.size})") }
            )
        }

        val contentModifier = Modifier
            .weight(1f)
            .fillMaxWidth()

        if (selectedTab == 1) {
            ReviewsTab(
                stats = stats,
                comments = comments,
                onRate = interactionViewModel::rate,
                onSend = interactionViewModel::addComment,
                modifier = contentModifier
            )
        } else {
            when (document.type) {
                DocumentType.PDF -> {
                    if (document.filePath != null) {
                        // Tài liệu người dùng tự thêm: mở đúng tệp đã chọn
                        PdfViewer(file = File(document.filePath), modifier = contentModifier, documentId = document.id)
                    } else if (document.fileUrl != null) {
                        // Tài liệu trên Firebase: tải về máy (chỉ tải một lần) rồi xem
                        RemotePdfViewer(document = document, modifier = contentModifier)
                    } else {
                        SamplePdfViewer(documentId = document.id, modifier = contentModifier)
                    }
                }

                else -> Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = "Chưa hỗ trợ xem trực tiếp định dạng ${document.type.extension}.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * TẠM THỜI: chép assets/sample.pdf ra cache rồi hiển thị.
 * TODO (phối hợp Bạn B): thay bằng tệp tải từ Firebase Storage về cacheDir -> truyền File đó vào PdfViewer.
 */
@Composable
private fun SamplePdfViewer(documentId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val out = File(context.cacheDir, "sample.pdf")
                context.assets.open("sample.pdf").use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                out
            }
        }.onSuccess { file = it }
            .onFailure { failed = true }
    }

    val current = file
    when {
        current != null -> PdfViewer(file = current, modifier = modifier, documentId = documentId)
        failed -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "Không tải được tệp PDF.", color = Color.Gray)
        }
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

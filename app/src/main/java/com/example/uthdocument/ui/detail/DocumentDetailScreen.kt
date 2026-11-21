package com.example.uthdocument.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.data.AppConfig
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.ui.bookmark.BookmarkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBack: () -> Unit,
    onOpenChat: (recipientUid: String, recipientName: String) -> Unit = { _, _ -> },
    viewModel: DocumentDetailViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    interactionViewModel: DocumentInteractionViewModel = viewModel()
) {
    LaunchedEffect(documentId) {
        viewModel.load(documentId)
        interactionViewModel.load(documentId)
    }
    val state by viewModel.uiState.collectAsState()
    val uploaderInfo by viewModel.uploaderInfo.collectAsState()
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
            when (val currentState = state) {
                DetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailUiState.NotFound -> Text(
                    text = "Không tìm thấy tài liệu.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
                is DetailUiState.Success -> DocumentContent(
                    document = currentState.document,
                    uploaderInfo = uploaderInfo,
                    interactionViewModel = interactionViewModel,
                    onOpenChat = onOpenChat
                )
            }
        }
    }
}

@Composable
private fun DocumentContent(
    document: Document,
    uploaderInfo: UploaderInfo?,
    interactionViewModel: DocumentInteractionViewModel,
    onOpenChat: (recipientUid: String, recipientName: String) -> Unit
) {
    val stats by interactionViewModel.stats.collectAsState()
    val comments by interactionViewModel.comments.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val resolvedUploaderUid = document.uploadedBy.trim().ifEmpty { uploaderInfo?.uid.orEmpty() }
    val resolvedUploaderName = listOf(
        document.uploaderName.trim(),
        uploaderInfo?.fullName.orEmpty(),
        uploaderInfo?.email?.substringBefore('@').orEmpty()
    ).firstOrNull { it.isNotEmpty() } ?: "Giảng viên / Admin UTH"

    val uploaderMeta = listOfNotNull(
        uploaderInfo?.faculty?.takeIf { it.isNotBlank() },
        uploaderInfo?.email?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            onClick = {
                onOpenChat(resolvedUploaderUid, resolvedUploaderName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1FF))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF002D58),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Người đăng tải",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = resolvedUploaderName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002D58),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uploaderMeta.isNotEmpty()) {
                        Text(
                            text = uploaderMeta,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF007B7A)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Nhắn tin",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nhắn tin",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Text(
            text = "${document.type.extension} • ${"%.1f".format(document.sizeInMb)} MB • ${document.category.displayName}",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                        PdfViewer(file = File(document.filePath), modifier = contentModifier, documentId = document.id)
                    } else if (document.fileUrl != null) {
                        RemotePdfViewer(document = document, modifier = contentModifier)
                    } else {
                        SamplePdfViewer(documentId = document.id, modifier = contentModifier)
                    }
                }

                else -> OfficeDocumentViewer(document = document, modifier = contentModifier)
            }
        }
    }
}

@Composable
private fun SamplePdfViewer(documentId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                val local = File(File(context.filesDir, "documents"), "$documentId.pdf")
                if (local.exists() && local.length() > 0) return@withContext local
                if (!AppConfig.USE_FAKE_DOCUMENTS) throw IllegalStateException("Tài liệu chưa có tệp")
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
            Text(text = "Tài liệu này chưa có tệp đính kèm.", color = Color.Gray)
        }
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
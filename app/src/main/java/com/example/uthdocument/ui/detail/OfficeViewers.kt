package com.example.uthdocument.ui.detail

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.data.AppConfig
import com.example.uthdocument.data.office.DocBlock
import com.example.uthdocument.data.office.DocxParser
import com.example.uthdocument.data.office.PptxParser
import com.example.uthdocument.data.office.SlideContent
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.ui.theme.UthBlue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.File

// ======================= Điểm vào =======================

/**
 * Xem tài liệu Word / PowerPoint ngay trong app (chỉ phần chữ) + nút mở bằng ứng dụng khác để xem đầy đủ.
 * Nguồn tệp: tệp người dùng tự thêm -> tệp tải từ Firebase -> tệp mẫu (khi dùng dữ liệu giả).
 */
@Composable
fun OfficeDocumentViewer(document: Document, modifier: Modifier = Modifier) {
    when {
        document.filePath != null ->
            OfficeFileContent(File(document.filePath), document, modifier)

        document.fileUrl != null ->
            RemoteFileContent(document = document, modifier = modifier) { file ->
                OfficeFileContent(file, document, Modifier.fillMaxSize())
            }

        else -> SampleOfficeContent(document, modifier)
    }
}

@Composable
private fun OfficeFileContent(file: File, document: Document, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        OpenExternalBar(file = file, type = document.type)

        val contentModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        when (document.type) {
            DocumentType.DOCX -> DocxViewer(file = file, documentId = document.id, modifier = contentModifier)
            DocumentType.PPTX -> PptxViewer(file = file, documentId = document.id, modifier = contentModifier)
            DocumentType.PDF -> PdfViewer(file = file, modifier = contentModifier, documentId = document.id)
        }
    }
}

/** TẠM THỜI cho dữ liệu giả: chép tệp mẫu trong assets ra cache rồi hiển thị. */
@Composable
private fun SampleOfficeContent(document: Document, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var file by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(document.type) {
        val assetName = when (document.type) {
            DocumentType.DOCX -> "sample.docx"
            DocumentType.PPTX -> "sample.pptx"
            DocumentType.PDF -> "sample.pdf"
        }
        try {
            file = withContext(Dispatchers.IO) {
                // tệp đã có sẵn trên máy này (do chính mình đăng tải lên)
                val local = File(File(context.filesDir, "documents"), "${document.id}.${document.type.extension.lowercase()}")
                if (local.exists() && local.length() > 0) return@withContext local
                // tệp mẫu chỉ dùng khi chạy dữ liệu giả
                if (!AppConfig.USE_FAKE_DOCUMENTS) throw IllegalStateException("Tài liệu chưa có tệp")
                val out = File(context.cacheDir, assetName)
                context.assets.open(assetName).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                out
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    val current = file
    when {
        current != null -> OfficeFileContent(current, document, modifier)
        failed -> CenteredMessage("Tài liệu này chưa có tệp đính kèm.", modifier)
        else -> CenteredLoading(modifier)
    }
}

// ======================= Word =======================

@Composable
fun DocxViewer(file: File, documentId: String?, modifier: Modifier = Modifier) {
    when (val state = rememberOfficeContent(file, DocxParser::parse)) {
        OfficeLoad.Loading -> CenteredLoading(modifier)
        is OfficeLoad.Error -> CenteredMessage(state.message, modifier)
        is OfficeLoad.Success -> DocxContent(state.data, documentId, modifier)
    }
}

@Composable
private fun DocxContent(blocks: List<DocBlock>, documentId: String?, modifier: Modifier) {
    // +1 vì dòng ghi chú ở đầu danh sách cũng là một phần tử
    val listState = rememberTrackedListState(documentId, blocks.size + 1)

    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { LimitationNote() }
            items(blocks) { block ->
                when (block) {
                    is DocBlock.Paragraph -> ParagraphText(block)
                    is DocBlock.TableRow -> TableRowView(block)
                }
            }
        }
    }
}

@Composable
private fun ParagraphText(paragraph: DocBlock.Paragraph) {
    val text = remember(paragraph) {
        buildAnnotatedString {
            paragraph.spans.forEach { span ->
                withStyle(
                    SpanStyle(
                        fontWeight = if (span.bold || paragraph.headingLevel > 0) FontWeight.Bold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        textDecoration = if (span.underline) TextDecoration.Underline else null
                    )
                ) { append(span.text) }
            }
        }
    }
    val size = when (paragraph.headingLevel) {
        0 -> 15.sp
        1 -> 24.sp
        2 -> 20.sp
        3 -> 18.sp
        else -> 16.sp
    }
    Row {
        if (paragraph.isBullet) Text(text = "•  ", fontSize = size)
        Text(text = text, fontSize = size, lineHeight = size * 1.4f, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TableRowView(row: DocBlock.TableRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F5FA), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        row.cells.forEach { cell -> Text(text = cell, fontSize = 13.sp, modifier = Modifier.weight(1f)) }
    }
}

// ======================= PowerPoint =======================

@Composable
fun PptxViewer(file: File, documentId: String?, modifier: Modifier = Modifier) {
    when (val state = rememberOfficeContent(file, PptxParser::parse)) {
        OfficeLoad.Loading -> CenteredLoading(modifier)
        is OfficeLoad.Error -> CenteredMessage(state.message, modifier)
        is OfficeLoad.Success -> PptxContent(state.data, documentId, modifier)
    }
}

@Composable
private fun PptxContent(slides: List<SlideContent>, documentId: String?, modifier: Modifier) {
    val listState = rememberTrackedListState(documentId, slides.size + 1)

    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LimitationNote() }
            items(slides, key = { it.number }) { slide -> SlideCard(slide, total = slides.size) }
        }
    }
}

@Composable
private fun SlideCard(slide: SlideContent, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "Slide ${slide.number}/$total", fontSize = 11.sp, color = Color.Gray)
            if (slide.title != null) {
                Text(text = slide.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = UthBlue)
            }
            slide.paragraphs.forEach { paragraph ->
                Text(
                    text = "•  ${paragraph.text}",
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = (paragraph.level * 16).dp)
                )
            }
            if (slide.title == null && slide.paragraphs.isEmpty()) {
                Text(text = "(Slide không có văn bản)", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

// ======================= Thành phần dùng chung =======================

internal sealed interface OfficeLoad<out T> {
    data object Loading : OfficeLoad<Nothing>
    data class Success<T>(val data: T) : OfficeLoad<T>
    data class Error(val message: String) : OfficeLoad<Nothing>
}

/** Đọc tệp ở luồng nền, lỗi (tệp hỏng, quá lớn, hết bộ nhớ...) thì trả về thông báo. */
@Composable
private fun <T> rememberOfficeContent(file: File, load: (File) -> T): OfficeLoad<T> {
    val state = produceState<OfficeLoad<T>>(initialValue = OfficeLoad.Loading, key1 = file) {
        value = withContext(Dispatchers.IO) {
            try {
                OfficeLoad.Success(load(file))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                OfficeLoad.Error("Không mở được tệp: ${e.message ?: "tệp bị lỗi"}")
            }
        }
    }
    return state.value
}

/** Mở lại đúng chỗ đang đọc dở và ghi tiến độ khi cuộn (dùng chung với "Tiếp tục học"). */
@Composable
internal fun rememberTrackedListState(documentId: String?, itemCount: Int): LazyListState {
    val progressViewModel: ReadingProgressViewModel = viewModel()
    val initial = remember(documentId, itemCount) {
        if (documentId != null) progressViewModel.pageOf(documentId) else 0
    }
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = initial.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    )
    if (documentId != null && itemCount > 0) {
        LaunchedEffect(state, documentId, itemCount) {
            snapshotFlow {
                state.firstVisibleItemIndex to (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            }
                .distinctUntilChanged()
                .collect { (first, last) -> progressViewModel.save(documentId, first, last, itemCount) }
        }
    }
    return state
}

@Composable
private fun LimitationNote() {
    Text(
        text = "Đang hiển thị phần chữ của tài liệu. Hình ảnh, biểu đồ và định dạng chi tiết hãy xem bằng nút \"Mở bằng ứng dụng khác\".",
        fontSize = 12.sp,
        color = Color.Gray
    )
}

@Composable
private fun OpenExternalBar(file: File, type: DocumentType) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { openExternally(context, file, type) }) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mở bằng ứng dụng khác")
        }
    }
}

private fun openExternally(context: Context, file: File, type: DocumentType) {
    val mime = when (type) {
        DocumentType.PDF -> "application/pdf"
        DocumentType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        DocumentType.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Mở bằng"))
    } catch (e: Exception) {
        Toast.makeText(context, "Không có ứng dụng nào mở được tệp này.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun CenteredMessage(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

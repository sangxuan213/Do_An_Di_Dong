package com.example.uthdocument.ui.detail

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bọc PdfRenderer của Android (không cần thư viện ngoài).
 * PdfRenderer chỉ cho mở 1 trang tại một thời điểm và không thread-safe -> dùng Mutex.
 */
class PdfPageRenderer(file: File) : AutoCloseable {
    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)
    private val mutex = Mutex()
    val pageCount: Int = renderer.pageCount

    suspend fun render(index: Int, widthPx: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val heightPx = (widthPx.toFloat() / page.width * page.height).toInt()
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(AndroidColor.WHITE) // PDF trong suốt -> tô trắng
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    override fun close() {
        // chờ trang đang render (nếu có) xong rồi mới đóng
        runBlocking {
            mutex.withLock {
                renderer.close()
                fd.close()
            }
        }
    }
}

@Composable
fun PdfViewer(file: File, modifier: Modifier = Modifier) {
    val renderer = remember(file) { runCatching { PdfPageRenderer(file) }.getOrNull() }

    if (renderer == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "Không mở được tệp PDF.", color = Color.Gray)
        }
        return
    }

    DisposableEffect(renderer) { onDispose { renderer.close() } }

    BoxWithConstraints(modifier = modifier.background(Color(0xFFEEEEEE))) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(renderer.pageCount) { index ->
                PdfPage(renderer = renderer, index = index, widthPx = widthPx)
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfPageRenderer, index: Int, widthPx: Int) {
    var bitmap by remember(renderer, index, widthPx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, index, widthPx) {
        bitmap = renderer.render(index, widthPx)
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = "Trang ${index + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

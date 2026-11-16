package com.example.uthdocument.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.ui.theme.UthBlue

private fun DocumentType.icon(): ImageVector = when (this) {
    DocumentType.PDF -> Icons.Default.PictureAsPdf
    DocumentType.DOCX -> Icons.Default.Description
    DocumentType.PPTX -> Icons.Default.Slideshow
}

@Composable
fun DocumentItem(
    document: Document,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onBookmarkClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = document.type.icon(),
                contentDescription = document.type.extension,
                tint = UthBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.type.extension} • ${"%.1f".format(document.sizeInMb)} MB • ${document.category.displayName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (document.progress > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { document.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = UthBlue
                    )
                }
            }
            if (document.isDownloaded) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Đã tải xuống",
                    tint = Color(0xFF2E9E5B)
                )
            }
            if (onBookmarkClick != null) {
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Bỏ lưu" else "Lưu tài liệu",
                        tint = if (isBookmarked) UthBlue else Color.Gray
                    )
                }
            }
        }
    }
}

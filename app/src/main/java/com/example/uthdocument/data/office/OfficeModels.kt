package com.example.uthdocument.data.office

/** Một đoạn chữ cùng kiểu định dạng trong tài liệu Word. */
data class TextSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

sealed interface DocBlock {
    /** headingLevel: 0 = đoạn thường, 1..6 = tiêu đề. */
    data class Paragraph(
        val spans: List<TextSpan>,
        val headingLevel: Int = 0,
        val isBullet: Boolean = false
    ) : DocBlock

    /** Một hàng của bảng, mỗi ô là một chuỗi chữ. */
    data class TableRow(val cells: List<String>) : DocBlock
}

data class SlideParagraph(val text: String, val level: Int)

data class SlideContent(
    val number: Int,
    val title: String?,
    val paragraphs: List<SlideParagraph>
)

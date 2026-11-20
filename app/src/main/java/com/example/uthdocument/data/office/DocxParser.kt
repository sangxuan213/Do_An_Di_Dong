package com.example.uthdocument.data.office

import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Đọc chữ từ tệp .docx (thực chất là tệp zip chứa word/document.xml).
 * Lấy được: đoạn văn, tiêu đề, in đậm/nghiêng/gạch chân, danh sách, bảng (dạng từng hàng).
 * Không lấy: hình ảnh, phông chữ, màu, căn lề.
 */
object DocxParser {
    private const val NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val MAX_XML_BYTES = 50L * 1024 * 1024
    private const val MAX_BLOCKS = 20_000

    fun parse(file: File): List<DocBlock> = ZipFile(file).use { zip ->
        val entry = zip.getEntry("word/document.xml")
            ?: throw IOException("Tệp DOCX không hợp lệ (thiếu word/document.xml).")
        if (entry.size > MAX_XML_BYTES) throw IOException("Tài liệu quá lớn để xem trực tiếp.")
        zip.getInputStream(entry).use { parseDocument(it) }
    }

    private fun parseDocument(input: InputStream): List<DocBlock> {
        val parser = newXmlParser(input)
        val blocks = mutableListOf<DocBlock>()

        var spans = mutableListOf<TextSpan>()
        var headingLevel = 0
        var isBullet = false
        var bold = false
        var italic = false
        var underline = false
        var inText = false
        var inTabs = false // w:tabs chứa các mốc tab, không phải ký tự tab trong chữ
        var tableDepth = 0
        var rowCells: MutableList<String>? = null
        var cell: StringBuilder? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && blocks.size < MAX_BLOCKS) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.namespace == NS_W) {
                        when (parser.name) {
                            "p" -> {
                                spans = mutableListOf()
                                headingLevel = 0
                                isBullet = false
                            }
                            "pStyle" -> {
                                val style = parser.getAttributeValue(null, "val").orEmpty()
                                headingLevel = headingLevelOf(style)
                                if (style.contains("List", ignoreCase = true)) isBullet = true
                            }
                            "numPr" -> isBullet = true
                            "r" -> {
                                bold = false
                                italic = false
                                underline = false
                            }
                            "b" -> bold = isOn(parser)
                            "i" -> italic = isOn(parser)
                            "u" -> underline = parser.getAttributeValue(null, "val") != "none"
                            "t" -> inText = true
                            "tabs" -> inTabs = true
                            "tab" -> if (!inTabs) spans.add(TextSpan("    "))
                            "br", "cr" -> spans.add(TextSpan("\n"))
                            "tbl" -> tableDepth++
                            "tr" -> if (tableDepth == 1) rowCells = mutableListOf()
                            "tc" -> if (tableDepth == 1) cell = StringBuilder()
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inText) spans.add(TextSpan(parser.text, bold, italic, underline))
                }

                XmlPullParser.END_TAG -> {
                    if (parser.namespace == NS_W) {
                        when (parser.name) {
                            "t" -> inText = false
                            "tabs" -> inTabs = false
                            "p" -> {
                                val text = spans.joinToString("") { it.text }
                                val currentCell = cell
                                if (tableDepth > 0) {
                                    // đoạn nằm trong ô bảng: gom vào ô
                                    if (currentCell != null && text.isNotBlank()) {
                                        if (currentCell.isNotEmpty()) currentCell.append(' ')
                                        currentCell.append(text.trim())
                                    }
                                } else if (text.isNotBlank()) {
                                    blocks.add(DocBlock.Paragraph(spans.toList(), headingLevel, isBullet))
                                }
                            }
                            "tc" -> if (tableDepth == 1) {
                                cell?.let { rowCells?.add(it.toString()) }
                                cell = null
                            }
                            "tr" -> if (tableDepth == 1) {
                                rowCells?.let { if (it.isNotEmpty()) blocks.add(DocBlock.TableRow(it.toList())) }
                                rowCells = null
                            }
                            "tbl" -> tableDepth--
                        }
                    }
                }
            }
            event = parser.next()
        }
        return blocks
    }

    /** "Heading1" -> 1, "Title" -> 1, "Subtitle" -> 2, còn lại 0. */
    private fun headingLevelOf(style: String): Int {
        if (style.equals("Title", ignoreCase = true)) return 1
        if (style.equals("Subtitle", ignoreCase = true)) return 2
        val match = Regex("heading\\s*(\\d)", RegexOption.IGNORE_CASE).find(style) ?: return 0
        return (match.groupValues[1].toIntOrNull() ?: 0).coerceIn(1, 6)
    }

    /** <w:b/> là bật; <w:b w:val="0"/> hoặc "false" là tắt. */
    private fun isOn(parser: XmlPullParser): Boolean {
        val value = parser.getAttributeValue(null, "val")
        return value == null || (value != "0" && !value.equals("false", ignoreCase = true))
    }
}

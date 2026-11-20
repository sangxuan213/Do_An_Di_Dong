package com.example.uthdocument.data.office

import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Đọc chữ từ tệp .pptx (tệp zip chứa ppt/slides/slideN.xml), theo đúng thứ tự trình chiếu.
 * Lấy được: tiêu đề và các dòng chữ (kèm cấp thụt lề) của từng slide, chữ trong bảng.
 * Không lấy: hình ảnh, biểu đồ, hiệu ứng, ghi chú.
 */
object PptxParser {
    private const val NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main"
    private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val MAX_SLIDE_BYTES = 10L * 1024 * 1024
    private const val MAX_SLIDES = 500
    private val SLIDE_REGEX = Regex("ppt/slides/slide(\\d+)\\.xml")

    fun parse(file: File): List<SlideContent> = ZipFile(file).use { zip ->
        val paths = orderedSlidePaths(zip).take(MAX_SLIDES)
        if (paths.isEmpty()) throw IOException("Tệp PPTX không có slide nào.")
        paths.mapIndexed { index, path -> parseSlide(zip, path, index + 1) }
    }

    // ---------- thứ tự slide ----------

    private fun orderedSlidePaths(zip: ZipFile): List<String> {
        val fallback = zip.entries().asSequence()
            .map { it.name }
            .filter { SLIDE_REGEX.matches(it) }
            .sortedBy { SLIDE_REGEX.matchEntire(it)!!.groupValues[1].toInt() }
            .toList()
        return try {
            val relationships = readRelationships(zip)
            val ordered = readSlideOrder(zip)
                .mapNotNull { relationships[it] }
                .map { target -> target.trimStart('/').let { if (it.startsWith("ppt/")) it else "ppt/$it" } }
                .filter { zip.getEntry(it) != null }
            ordered.ifEmpty { fallback }
        } catch (e: Exception) {
            fallback
        }
    }

    /** rId -> đường dẫn tương đối của slide. */
    private fun readRelationships(zip: ZipFile): Map<String, String> {
        val entry = zip.getEntry("ppt/_rels/presentation.xml.rels") ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        zip.getInputStream(entry).use { input ->
            val parser = newXmlParser(input)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) map[id] = target
                }
                event = parser.next()
            }
        }
        return map
    }

    /** Danh sách rId theo thứ tự trình chiếu (p:sldIdLst). */
    private fun readSlideOrder(zip: ZipFile): List<String> {
        val entry = zip.getEntry("ppt/presentation.xml") ?: return emptyList()
        val ids = mutableListOf<String>()
        zip.getInputStream(entry).use { input ->
            val parser = newXmlParser(input)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.namespace == NS_P && parser.name == "sldId") {
                    parser.getAttributeValue(NS_R, "id")?.let { ids.add(it) }
                }
                event = parser.next()
            }
        }
        return ids
    }

    // ---------- một slide ----------

    private fun parseSlide(zip: ZipFile, path: String, number: Int): SlideContent {
        val entry = zip.getEntry(path) ?: return SlideContent(number, null, emptyList())
        if (entry.size > MAX_SLIDE_BYTES) return SlideContent(number, "(Slide quá lớn để hiển thị)", emptyList())

        return zip.getInputStream(entry).use { input ->
            val parser = newXmlParser(input)
            var title: String? = null
            val body = mutableListOf<SlideParagraph>()

            var inShape = false
            var shapeIsTitle = false
            var shapeSkip = false // ô số trang, chân trang, ngày tháng
            val shapeParagraphs = mutableListOf<SlideParagraph>()

            val current = StringBuilder()
            var level = 0
            var inText = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val ns = parser.namespace
                        val name = parser.name
                        when {
                            ns == NS_P && name == "sp" -> {
                                inShape = true
                                shapeIsTitle = false
                                shapeSkip = false
                                shapeParagraphs.clear()
                            }
                            ns == NS_P && name == "ph" -> {
                                when (parser.getAttributeValue(null, "type")) {
                                    "title", "ctrTitle" -> shapeIsTitle = true
                                    "sldNum", "ftr", "dt" -> shapeSkip = true
                                }
                            }
                            ns == NS_A && name == "p" -> {
                                current.clear()
                                level = 0
                            }
                            ns == NS_A && name == "pPr" ->
                                level = parser.getAttributeValue(null, "lvl")?.toIntOrNull() ?: 0
                            ns == NS_A && name == "t" -> inText = true
                            ns == NS_A && name == "br" -> current.append('\n')
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (inText) current.append(parser.text)
                    }

                    XmlPullParser.END_TAG -> {
                        val ns = parser.namespace
                        val name = parser.name
                        when {
                            ns == NS_A && name == "t" -> inText = false
                            ns == NS_A && name == "p" -> {
                                val text = current.toString().trim()
                                if (text.isNotEmpty()) {
                                    val paragraph = SlideParagraph(text, level)
                                    if (inShape) shapeParagraphs.add(paragraph) else body.add(paragraph)
                                }
                            }
                            ns == NS_P && name == "sp" -> {
                                if (!shapeSkip) {
                                    if (shapeIsTitle && title == null) {
                                        title = shapeParagraphs.joinToString(" ") { it.text }
                                    } else {
                                        body.addAll(shapeParagraphs)
                                    }
                                }
                                inShape = false
                            }
                        }
                    }
                }
                event = parser.next()
            }
            SlideContent(number, title?.ifBlank { null }, body)
        }
    }
}

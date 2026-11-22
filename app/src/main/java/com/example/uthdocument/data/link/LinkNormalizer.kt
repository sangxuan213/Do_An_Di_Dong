package com.example.uthdocument.data.link

import android.net.Uri
import com.example.uthdocument.domain.model.DocumentType

/** Đổi các loại liên kết "chia sẻ" quen thuộc thành liên kết tải trực tiếp mà app đọc được. */
object LinkNormalizer {

    data class NormalizedLink(
        val url: String,            // liên kết tải trực tiếp
        val typeHint: DocumentType?, // đoán được loại tệp thì điền sẵn, không thì null
        val source: String          // tên nguồn để báo cho người dùng, vd "Google Drive"
    )

    /** Trả về null nếu không phải liên kết https hợp lệ hoặc không nhận dạng được. */
    fun normalize(input: String): NormalizedLink? {
        val raw = input.trim()
        if (raw.isEmpty() || raw.any { it.isWhitespace() }) return null

        val uri = Uri.parse(raw)
        if (uri.scheme?.lowercase() != "https") return null // http bị Android chặn mặc định
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path.orEmpty()

        return when {
            host == "drive.google.com" -> driveFile(uri, path)
            host == "docs.google.com" -> googleDocs(path)
            host == "github.com" -> github(raw, path)
            host.endsWith("dropbox.com") -> dropbox(raw, path)
            else -> NormalizedLink(raw, typeFromPath(path), "Liên kết trực tiếp")
        }
    }

    // Google Drive: https://drive.google.com/file/d/<ID>/view  hoặc  .../open?id=<ID>
    private fun driveFile(uri: Uri, path: String): NormalizedLink? {
        val id = Regex("/file/d/([\\w-]+)").find(path)?.groupValues?.get(1)
            ?: uri.getQueryParameter("id")
            ?: return null
        return NormalizedLink(
            url = "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t",
            typeHint = null,
            source = "Google Drive"
        )
    }

    // Google Docs / Slides gốc: xuất ra .docx / .pptx
    private fun googleDocs(path: String): NormalizedLink? {
        Regex("/document/d/([\\w-]+)").find(path)?.groupValues?.get(1)?.let { id ->
            return NormalizedLink("https://docs.google.com/document/d/$id/export?format=docx", DocumentType.DOCX, "Google Docs")
        }
        Regex("/presentation/d/([\\w-]+)").find(path)?.groupValues?.get(1)?.let { id ->
            return NormalizedLink("https://docs.google.com/presentation/d/$id/export/pptx", DocumentType.PPTX, "Google Slides")
        }
        return null
    }

    // GitHub: .../blob/<nhánh>/<đường dẫn> -> raw.githubusercontent.com
    private fun github(raw: String, path: String): NormalizedLink {
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.size >= 5 && parts[2] == "blob") {
            val rest = parts.drop(4).joinToString("/")
            val url = "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/${parts[3]}/$rest"
            return NormalizedLink(url, typeFromPath(rest), "GitHub")
        }
        return NormalizedLink(raw, typeFromPath(path), "GitHub")
    }

    // Dropbox: dl=0 (xem trước) -> dl=1 (tải tệp)
    private fun dropbox(raw: String, path: String): NormalizedLink {
        val url = when {
            "dl=0" in raw -> raw.replace("dl=0", "dl=1")
            "dl=1" in raw -> raw
            else -> raw + (if ('?' in raw) "&dl=1" else "?dl=1")
        }
        return NormalizedLink(url, typeFromPath(path), "Dropbox")
    }

    private fun typeFromPath(path: String): DocumentType? {
        val p = path.lowercase()
        return when {
            p.endsWith(".pdf") -> DocumentType.PDF
            p.endsWith(".docx") -> DocumentType.DOCX
            p.endsWith(".pptx") -> DocumentType.PPTX
            else -> null
        }
    }
}

package com.example.uthdocument.ui.add

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AddDocumentUiState(
    val uri: Uri? = null,
    val fileName: String = "",
    val type: DocumentType? = null,
    val sizeBytes: Long = 0L,
    val title: String = "",
    val category: Category = Category.ECONOMICS,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class AddDocumentViewModel(application: Application) : AndroidViewModel(application) {

    // TODO (phối hợp Bạn B): đổi sang bản Firebase (upload lên Storage + ghi metadata vào Firestore)
    private val repository: DocumentRepository = FakeDocumentRepositoryImpl()

    private val _uiState = MutableStateFlow(AddDocumentUiState())
    val uiState: StateFlow<AddDocumentUiState> = _uiState.asStateFlow()

    fun onFilePicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        var name = ""
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) name = cursor.getString(nameIdx).orEmpty()
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }

        val type = typeFromName(name)
        if (type == null) {
            _uiState.update {
                it.copy(uri = null, fileName = name, type = null, error = "Chỉ hỗ trợ tệp PDF, DOCX hoặc PPTX.")
            }
            return
        }
        _uiState.update {
            it.copy(
                uri = uri,
                fileName = name,
                type = type,
                sizeBytes = size,
                // gợi ý tiêu đề từ tên tệp nếu người dùng chưa nhập
                title = it.title.ifBlank { name.substringBeforeLast('.') },
                error = null
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun save() {
        val state = _uiState.value
        val uri = state.uri ?: return
        val type = state.type ?: return
        if (state.title.isBlank() || state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val app = getApplication<Application>()
                val id = UUID.randomUUID().toString()
                // Chép tệp vào bộ nhớ riêng của app để còn mở lại được sau này
                // (quyền đọc URI do hệ thống cấp chỉ là tạm thời).
                val file = withContext(Dispatchers.IO) {
                    val dir = File(app.filesDir, "documents").apply { mkdirs() }
                    val out = File(dir, "$id.${type.extension.lowercase()}")
                    val input = app.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Không đọc được tệp đã chọn")
                    input.use { src -> out.outputStream().use { dst -> src.copyTo(dst) } }
                    out
                }
                repository.addDocument(
                    Document(
                        id = id,
                        title = state.title.trim(),
                        type = type,
                        category = state.category,
                        sizeInMb = file.length() / 1024.0 / 1024.0,
                        isDownloaded = true,
                        progress = 0f,
                        filePath = file.absolutePath
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = "Không lưu được tài liệu: ${e.message}") }
            }
        }
    }

    private fun typeFromName(name: String): DocumentType? =
        when (name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> DocumentType.PDF
            "docx" -> DocumentType.DOCX
            "pptx" -> DocumentType.PPTX
            else -> null
        }
}

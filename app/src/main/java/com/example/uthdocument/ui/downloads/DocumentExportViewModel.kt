package com.example.uthdocument.ui.downloads

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.download.DocumentExporter
import com.example.uthdocument.data.repository.DocumentFileRepository
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface ExportState {
    data object Idle : ExportState
    data class Working(val documentId: String) : ExportState
    /** Chỉ Android 9 trở xuống: cần người dùng chọn nơi lưu. */
    data class NeedsLocation(val suggestedName: String) : ExportState
    data class Done(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}

/** Tải tài liệu về máy (nếu chưa có) rồi lưu một bản vào thư mục Tải xuống để người dùng tự quản lý. */
class DocumentExportViewModel(application: Application) : AndroidViewModel(application) {

    private val files = DocumentFileRepository(application)
    private val exporter = DocumentExporter(application)

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    private var pendingFile: File? = null

    fun export(document: Document) {
        if (_state.value is ExportState.Working || _state.value is ExportState.NeedsLocation) return
        viewModelScope.launch {
            _state.value = ExportState.Working(document.id)

            val fileResult = files.getFile(document)
            val file = fileResult.getOrNull()
            if (file == null) {
                _state.value = ExportState.Error(fileResult.exceptionOrNull()?.message ?: "Không tải được tệp.")
                return@launch
            }

            val fileName = "${sanitize(document.title)}.${document.type.extension.lowercase()}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exporter.exportToDownloads(file, fileName, mimeOf(document.type)).fold(
                    onSuccess = { _state.value = ExportState.Done("Đã lưu vào Tải xuống/UTH Documents: $fileName") },
                    onFailure = { _state.value = ExportState.Error(it.message ?: "Không lưu được tệp.") }
                )
            } else {
                pendingFile = file
                _state.value = ExportState.NeedsLocation(fileName)
            }
        }
    }

    /** Android 9 trở xuống: người dùng đã chọn nơi lưu. */
    fun saveToUri(uri: Uri) {
        val file = pendingFile ?: run { _state.value = ExportState.Idle; return }
        viewModelScope.launch {
            _state.value = ExportState.Working("")
            exporter.copyToUri(file, uri).fold(
                onSuccess = { _state.value = ExportState.Done("Đã lưu tệp.") },
                onFailure = { _state.value = ExportState.Error(it.message ?: "Không lưu được tệp.") }
            )
            pendingFile = null
        }
    }

    fun cancel() {
        pendingFile = null
        _state.value = ExportState.Idle
    }

    fun consume() {
        _state.value = ExportState.Idle
    }

    private fun mimeOf(type: DocumentType): String = when (type) {
        DocumentType.PDF -> "application/pdf"
        DocumentType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        DocumentType.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    }

    /** Bỏ các ký tự không được phép trong tên tệp. */
    private fun sanitize(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "tai-lieu" }.take(100)
}

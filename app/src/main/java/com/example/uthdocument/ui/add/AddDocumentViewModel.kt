package com.example.uthdocument.ui.add

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppConfig
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.data.link.LinkChecker
import com.example.uthdocument.data.link.LinkNormalizer
import com.example.uthdocument.data.repository.FakeCourseRepositoryImpl
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.CourseRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class SourceMode { DEVICE, LINK }

/** Tệp chọn từ máy bị giới hạn khi lưu trong Firestore (chưa có Storage). */
const val MAX_DEVICE_FILE_BYTES = 10L * 1024 * 1024

data class AddDocumentUiState(
    val mode: SourceMode = SourceMode.DEVICE,
    // --- chọn tệp từ máy ---
    val uri: Uri? = null,
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    // --- dán đường dẫn ---
    val link: String = "",
    val linkSource: String? = null,
    // --- chung ---
    val type: DocumentType = DocumentType.PDF,
    val title: String = "",
    val category: Category = Category.ECONOMICS,
    val courseInput: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = !isSaving && title.isNotBlank() &&
            if (mode == SourceMode.DEVICE) uri != null else link.isNotBlank()
}

/**
 * Hai cách thêm tài liệu:
 *  - Từ thiết bị: chọn tệp trong máy (PDF, DOCX, PPTX). Khi chưa có Storage thì tệp ≤ 10 MB được lưu trong Firestore.
 *  - Dán đường dẫn: Drive, GitHub, Dropbox... cho tệp lớn; không tốn dung lượng Firebase.
 */
class AddDocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository = AppContainer.documentRepository
    private val courseRepository: CourseRepository = FakeCourseRepositoryImpl()
    private val linkChecker = LinkChecker()

    /** Danh sách môn để gợi ý khi nhập. */
    val courses: StateFlow<List<Course>> = courseRepository.getCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(AddDocumentUiState())
    val uiState: StateFlow<AddDocumentUiState> = _uiState.asStateFlow()

    fun onModeSelected(mode: SourceMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    // ---------------- chọn tệp từ máy ----------------

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
            _uiState.update { it.copy(uri = null, fileName = name, error = "Chỉ hỗ trợ tệp PDF, DOCX hoặc PPTX.") }
            return
        }
        if (!AppConfig.USE_FIREBASE_STORAGE && size > MAX_DEVICE_FILE_BYTES) {
            _uiState.update {
                it.copy(
                    uri = null, fileName = name, sizeBytes = size,
                    error = "Tệp lớn hơn 10 MB. Hãy chọn \"Dán đường dẫn\" (Google Drive, GitHub...) cho tệp lớn."
                )
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

    // ---------------- dán đường dẫn ----------------

    fun onLinkChange(value: String) {
        val normalized = LinkNormalizer.normalize(value)
        _uiState.update { state ->
            state.copy(
                link = value,
                linkSource = normalized?.source,
                type = normalized?.typeHint ?: state.type,
                title = if (state.title.isBlank() && normalized != null) titleFromLink(normalized.url) else state.title,
                error = null
            )
        }
    }

    fun onTypeSelected(type: DocumentType) {
        _uiState.update { it.copy(type = type, error = null) }
    }

    // ---------------- thông tin chung ----------------

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onCourseInputChange(value: String) {
        _uiState.update { it.copy(courseInput = value) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề.") }
            return
        }

        // "KTVT • Kinh tế vận tải" hoặc "ktvt" -> mã môn viết hoa "KTVT"
        val courseCode = state.courseInput.substringBefore('•').trim().uppercase()
        val matchedCourse = courses.value.find {
            it.code.equals(courseCode, ignoreCase = true) || it.id.equals(courseCode, ignoreCase = true)
        }
        val category = matchedCourse?.category ?: state.category

        when (state.mode) {
            SourceMode.DEVICE -> saveFromDevice(state, courseCode, category)
            SourceMode.LINK -> saveFromLink(state, courseCode, category)
        }
    }

    private fun saveFromDevice(state: AddDocumentUiState, courseCode: String, category: Category) {
        val uri = state.uri
        if (uri == null) {
            _uiState.update { it.copy(error = "Vui lòng chọn tệp.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val app = getApplication<Application>()
                val id = UUID.randomUUID().toString()
                // Chép tệp vào bộ nhớ riêng của app (cũng là bản lưu sẵn trên máy người đăng)
                val file = withContext(Dispatchers.IO) {
                    val dir = File(app.filesDir, "documents").apply { mkdirs() }
                    val out = File(dir, "$id.${state.type.extension.lowercase()}")
                    val input = app.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Không đọc được tệp đã chọn")
                    input.use { src -> out.outputStream().use { dst -> src.copyTo(dst) } }
                    out
                }
                repository.addDocument(
                    Document(
                        id = id,
                        title = state.title.trim(),
                        type = state.type,
                        category = category,
                        sizeInMb = file.length() / 1024.0 / 1024.0,
                        isDownloaded = true,
                        filePath = file.absolutePath,
                        courseId = courseCode.ifBlank { null },
                        uploadedAt = System.currentTimeMillis(),
                        uploaderId = AppContainer.userRepository.currentUserId()
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isSaving = false, error = "Không lưu được tài liệu: ${e.message}") }
            }
        }
    }

    private fun saveFromLink(state: AddDocumentUiState, courseCode: String, category: Category) {
        val normalized = LinkNormalizer.normalize(state.link)
        if (normalized == null) {
            _uiState.update {
                it.copy(error = "Đường dẫn chưa hợp lệ. Cần liên kết https của Google Drive, GitHub, Dropbox hoặc liên kết tải trực tiếp.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val check = linkChecker.check(normalized.url, state.type)
            if (check.isFailure) {
                _uiState.update { it.copy(isSaving = false, error = check.exceptionOrNull()?.message) }
                return@launch
            }
            val sizeBytes = check.getOrDefault(0L)

            runCatching {
                repository.addDocument(
                    Document(
                        id = UUID.randomUUID().toString(),
                        title = state.title.trim(),
                        type = state.type,
                        category = category,
                        sizeInMb = sizeBytes / 1024.0 / 1024.0,
                        courseId = courseCode.ifBlank { null },
                        uploadedAt = System.currentTimeMillis(),
                        fileUrl = normalized.url,
                        uploaderId = AppContainer.userRepository.currentUserId()
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                if (e is CancellationException) throw e
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

    /** "…/Bai-giang-KTVT.pdf" -> "Bai giang KTVT"; liên kết không có tên tệp thì để trống. */
    private fun titleFromLink(url: String): String {
        val name = Uri.parse(url).lastPathSegment.orEmpty()
        return if ('.' in name) Uri.decode(name).substringBeforeLast('.').replace('_', ' ').replace('-', ' ') else ""
    }
}

package com.example.uthdocument.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.DocumentFileRepository
import com.example.uthdocument.domain.model.Document
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface DownloadUiState {
    /** progress = null khi chưa biết dung lượng tổng. */
    data class Downloading(val progress: Float?) : DownloadUiState
    data class Ready(val file: File) : DownloadUiState
    data class Error(val message: String) : DownloadUiState
}

class DocumentDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val fileRepository = DocumentFileRepository(application)

    private val _state = MutableStateFlow<DownloadUiState>(DownloadUiState.Downloading(null))
    val state: StateFlow<DownloadUiState> = _state.asStateFlow()

    private var currentDocument: Document? = null
    private var job: Job? = null

    fun load(document: Document) {
        if (currentDocument?.id == document.id && _state.value is DownloadUiState.Ready) return
        currentDocument = document
        start(document)
    }

    fun retry() {
        currentDocument?.let { start(it) }
    }

    private fun start(document: Document) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = DownloadUiState.Downloading(null)
            fileRepository.getFile(document) { progress ->
                _state.value = DownloadUiState.Downloading(progress)
            }
                .onSuccess { _state.value = DownloadUiState.Ready(it) }
                .onFailure { _state.value = DownloadUiState.Error(it.message ?: "Không tải được tệp.") }
        }
    }
}

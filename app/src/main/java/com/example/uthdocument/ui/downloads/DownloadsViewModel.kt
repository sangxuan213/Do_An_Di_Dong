package com.example.uthdocument.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.DownloadsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val documents: List<Document>,
    val sizes: Map<String, Long>, // id -> dung lượng trên máy (byte)
    val totalBytes: Long
)

class DownloadsViewModel(
    documentRepository: DocumentRepository = AppContainer.documentRepository,
    private val downloadsRepository: DownloadsRepository = AppContainer.downloadsRepository
) : ViewModel() {

    /** null = đang tải */
    val ui: StateFlow<DownloadsUiState?> = combine(
        documentRepository.getDocumentsByCategory(null),
        downloadsRepository.downloaded
    ) { docs, sizes ->
        val list = docs.filter { it.id in sizes }.sortedBy { it.title.lowercase() }
        DownloadsUiState(list, sizes, list.sumOf { sizes[it.id] ?: 0L })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(documentId: String) {
        viewModelScope.launch { downloadsRepository.remove(documentId) }
    }
}

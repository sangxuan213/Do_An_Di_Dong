package com.example.uthdocument.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Success(val document: Document) : DetailUiState
}

class DocumentDetailViewModel(
    private val repository: DocumentRepository = FakeDocumentRepositoryImpl()
) : ViewModel() {

    private val documentId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = documentId
        .flatMapLatest<String?, DetailUiState> { id ->
            if (id == null) {
                flowOf(DetailUiState.Loading)
            } else {
                repository.getDocumentById(id).map<Document?, DetailUiState> { doc ->
                    if (doc == null) DetailUiState.NotFound else DetailUiState.Success(doc)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    fun load(id: String) {
        documentId.value = id
    }
}

package com.example.uthdocument.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyDocumentsViewModel(
    userRepository: UserRepository = AppContainer.userRepository,
    private val documentRepository: DocumentRepository = AppContainer.documentRepository
) : ViewModel() {

    /** Tài liệu do chính mình đăng; null = đang tải. */
    val documents: StateFlow<List<Document>?> = documentRepository
        .getDocumentsByUploader(userRepository.currentUserId().orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun delete(documentId: String) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(documentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message ?: "Không xóa được tài liệu."
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

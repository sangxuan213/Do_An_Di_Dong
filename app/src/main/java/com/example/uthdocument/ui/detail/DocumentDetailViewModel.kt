package com.example.uthdocument.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UploaderInfo(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val faculty: String = "",
    val role: String = ""
)

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Success(val document: Document) : DetailUiState
}

class DocumentDetailViewModel(
    private val repository: DocumentRepository = AppContainer.documentRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val documentId = MutableStateFlow<String?>(null)
    private val _uploaderInfo = MutableStateFlow<UploaderInfo?>(null)
    val uploaderInfo: StateFlow<UploaderInfo?> = _uploaderInfo

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = documentId
        .flatMapLatest<String?, DetailUiState> { id ->
            if (id == null) {
                flowOf(DetailUiState.Loading)
            } else {
                repository.getDocumentById(id).map<Document?, DetailUiState> { doc ->
                    if (doc == null) {
                        DetailUiState.NotFound
                    } else {
                        fetchUploaderIfNeeded(doc)
                        DetailUiState.Success(doc)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    private var viewCounted = false

    fun load(id: String) {
        documentId.value = id
        if (!viewCounted) {
            viewCounted = true
            viewModelScope.launch { repository.incrementViewCount(id) }
        }
    }

    private fun fetchUploaderIfNeeded(document: Document) {
        val uid = document.uploadedBy.trim()
        if (uid.isEmpty()) {
            _uploaderInfo.value = null
            return
        }
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).get().await()
                if (snap.exists()) {
                    _uploaderInfo.value = UploaderInfo(
                        uid = uid,
                        fullName = snap.getString("fullName").orEmpty().trim(),
                        email = snap.getString("email").orEmpty().trim(),
                        faculty = snap.getString("faculty").orEmpty().trim(),
                        role = snap.getString("role").orEmpty().trim()
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
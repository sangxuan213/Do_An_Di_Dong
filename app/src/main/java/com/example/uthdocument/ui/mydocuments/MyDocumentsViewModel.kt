package com.example.uthdocument.ui.mydocuments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyDocumentsViewModel(
    private val repository: DocumentRepository = AppContainer.documentRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    val currentUid: String
        get() = auth.currentUser?.uid.orEmpty()

    val myDocuments: StateFlow<List<Document>?> = repository
        .getDocumentsByUploader(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }
}

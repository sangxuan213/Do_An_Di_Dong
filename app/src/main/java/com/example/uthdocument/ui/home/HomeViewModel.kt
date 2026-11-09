package com.example.uthdocument.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * null = đang tải, list rỗng = tải xong nhưng không có dữ liệu.
 * Sau này đổi FakeDocumentRepositoryImpl thành bản Firebase của Bạn B là xong, UI không phải sửa.
 */
class HomeViewModel(
    repository: DocumentRepository = FakeDocumentRepositoryImpl()
) : ViewModel() {

    val recentDocuments: StateFlow<List<Document>?> = repository
        .getRecentDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestDocuments: StateFlow<List<Document>?> = repository
        .getDocumentsByCategory(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

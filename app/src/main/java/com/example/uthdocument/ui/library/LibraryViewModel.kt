package com.example.uthdocument.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.Normalizer

class LibraryViewModel(
    private val repository: DocumentRepository = FakeDocumentRepositoryImpl()
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _onlyBookmarked = MutableStateFlow(false)
    val onlyBookmarked: StateFlow<Boolean> = _onlyBookmarked.asStateFlow()

    /** null = đang tải */
    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<Document>?> = _selectedCategory
        .flatMapLatest { category -> repository.getDocumentsByCategory(category) }
        .combine(_query) { docs, q ->
            val key = q.trim().normalized()
            if (key.isEmpty()) docs else docs.filter { it.title.normalized().contains(key) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onCategorySelected(category: Category?) {
        _selectedCategory.value = category
    }

    fun toggleOnlyBookmarked() {
        _onlyBookmarked.update { !it }
    }

    // Bỏ dấu + chữ thường để gõ "kinh te" vẫn ra "Kinh tế"
    private fun String.normalized(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
}

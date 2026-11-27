package com.example.uthdocument.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentSort
import com.example.uthdocument.domain.model.DocumentStats
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.InteractionRepository
import com.example.uthdocument.ui.util.normalizedForSearch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LibraryViewModel(
    private val repository: DocumentRepository = AppContainer.documentRepository,
    interactionRepository: InteractionRepository = AppContainer.interactionRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _onlyBookmarked = MutableStateFlow(false)
    val onlyBookmarked: StateFlow<Boolean> = _onlyBookmarked.asStateFlow()

    private val _sort = MutableStateFlow(DocumentSort.NEWEST)
    val sort: StateFlow<DocumentSort> = _sort.asStateFlow()

    /** Rỗng = không lọc theo loại tệp (hiện tất cả). */
    private val _types = MutableStateFlow<Set<DocumentType>>(emptySet())
    val types: StateFlow<Set<DocumentType>> = _types.asStateFlow()

    private val allStats = interactionRepository.getAllStats()

    /** Số liệu tương tác (điểm sao, số đánh giá) theo id tài liệu, để hiện trên card. */
    val stats: StateFlow<Map<String, DocumentStats>> = allStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** null = đang tải */
    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<Document>?> = combine(
        _selectedCategory.flatMapLatest { category -> repository.getDocumentsByCategory(category) },
        allStats,
        _query,
        _sort,
        _types
    ) { docs, statsMap, q, sort, types ->
        val key = q.trim().normalizedForSearch()
        docs
            .filter { doc ->
                (key.isEmpty() || doc.title.normalizedForSearch().contains(key) ||
                    doc.courseId?.normalizedForSearch()?.contains(key) == true) &&
                    (types.isEmpty() || doc.type in types)
            }
            .sortedWith(comparatorFor(sort, statsMap))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onCategorySelected(category: Category?) {
        _selectedCategory.value = category
    }

    /** Bỏ mọi bộ lọc (từ khóa, danh mục, loại tệp, đã lưu); giữ cách sắp xếp. */
    fun clearFilters() {
        _query.value = ""
        _selectedCategory.value = null
        _types.value = emptySet()
        _onlyBookmarked.value = false
    }

    fun toggleOnlyBookmarked() {
        _onlyBookmarked.update { !it }
    }

    fun onSortSelected(sort: DocumentSort) {
        _sort.value = sort
    }

    fun toggleType(type: DocumentType) {
        _types.update { current -> if (type in current) current - type else current + type }
    }

    private fun comparatorFor(sort: DocumentSort, statsMap: Map<String, DocumentStats>): Comparator<Document> =
        when (sort) {
            DocumentSort.NEWEST -> compareByDescending<Document> { it.uploadedAt }
            DocumentSort.MOST_VIEWED -> compareByDescending<Document> { it.viewCount }
            // Điểm trung bình cao trước; bằng điểm thì ai nhiều lượt đánh giá hơn đứng trước
            DocumentSort.TOP_RATED -> compareByDescending<Document> { statsMap[it.id]?.ratingAverage ?: 0.0 }
                .thenByDescending { statsMap[it.id]?.ratingCount ?: 0 }
        }
}

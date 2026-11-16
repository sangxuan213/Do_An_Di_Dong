package com.example.uthdocument.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeInteractionRepositoryImpl
import com.example.uthdocument.data.repository.FakeProfileRepositoryImpl
import com.example.uthdocument.domain.model.Comment
import com.example.uthdocument.domain.model.DocumentStats
import com.example.uthdocument.domain.repository.InteractionRepository
import com.example.uthdocument.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Thích, đánh giá sao và bình luận của một tài liệu. Gọi load(id) khi mở màn hình. */
class DocumentInteractionViewModel(
    private val repository: InteractionRepository = FakeInteractionRepositoryImpl(),
    private val profileRepository: ProfileRepository = FakeProfileRepositoryImpl()
) : ViewModel() {

    private val documentId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<DocumentStats> = documentId
        .flatMapLatest<String?, DocumentStats> { id ->
            if (id == null) flowOf(DocumentStats()) else repository.getStats(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentStats())

    @OptIn(ExperimentalCoroutinesApi::class)
    val comments: StateFlow<List<Comment>> = documentId
        .flatMapLatest<String?, List<Comment>> { id ->
            if (id == null) flowOf(emptyList()) else repository.getComments(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: String) {
        documentId.value = id
    }

    fun toggleLike() {
        val id = documentId.value ?: return
        viewModelScope.launch { repository.toggleLike(id) }
    }

    /** Bấm lại đúng số sao đang chấm thì bỏ đánh giá. */
    fun rate(stars: Int) {
        val id = documentId.value ?: return
        val newValue = if (stars == stats.value.myRating) 0 else stars
        viewModelScope.launch { repository.setRating(id, newValue) }
    }

    fun addComment(content: String) {
        val id = documentId.value ?: return
        val text = content.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val author = profileRepository.getCurrentUser().first().fullName
            repository.addComment(id, author, text)
        }
    }
}

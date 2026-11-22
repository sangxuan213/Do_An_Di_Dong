package com.example.uthdocument.ui.bookmark

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.uthdocument.data.repository.LocalBookmarkRepository
import com.example.uthdocument.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.StateFlow

/** Dùng chung cho mọi màn hình cần nút bookmark: `bookmarkViewModel: BookmarkViewModel = viewModel()`. */
class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BookmarkRepository = LocalBookmarkRepository.getInstance(application)

    val bookmarkedIds: StateFlow<Set<String>> = repository.bookmarkedIds

    fun toggle(id: String) = repository.toggle(id)
}

package com.example.uthdocument.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.uthdocument.data.repository.LocalReadingProgressRepository
import com.example.uthdocument.domain.repository.ReadingProgressRepository

/** Cầu nối để trình xem PDF đọc và ghi tiến độ đọc. */
class ReadingProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReadingProgressRepository = LocalReadingProgressRepository.getInstance(application)

    fun pageOf(documentId: String): Int = repository.pageOf(documentId)

    fun save(documentId: String, page: Int, lastVisiblePage: Int, pageCount: Int) =
        repository.save(documentId, page, lastVisiblePage, pageCount)
}

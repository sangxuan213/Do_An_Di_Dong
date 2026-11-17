package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Bọc ngoài repository tài liệu (giả hay Firebase đều được): gắn tiến độ đọc lưu trên máy vào từng tài liệu,
 * và tự tạo danh sách "Tiếp tục học" (đang đọc dở, mới đọc gần nhất đứng đầu).
 */
class ProgressAwareDocumentRepository(
    private val delegate: DocumentRepository,
    private val progressRepository: ReadingProgressRepository
) : DocumentRepository {

    override fun getRecentDocuments(): Flow<List<Document>> =
        combine(delegate.getDocumentsByCategory(null), progressRepository.progress) { docs, progressMap ->
            docs
                .map { doc -> doc.withProgress(progressMap) }
                .filter { it.progress > 0f && it.progress < 1f } // đọc xong rồi thì không cần "tiếp tục"
                .sortedByDescending { progressMap[it.id]?.lastReadAt ?: 0L }
        }

    override fun getDocumentsByCategory(category: Category?): Flow<List<Document>> =
        delegate.getDocumentsByCategory(category).withProgress()

    override fun getDocumentsByCourse(courseId: String): Flow<List<Document>> =
        delegate.getDocumentsByCourse(courseId).withProgress()

    // Màn chi tiết không hiện tiến độ nên không gắn vào, tránh phát lại mỗi lần lật trang
    override fun getDocumentById(id: String): Flow<Document?> = delegate.getDocumentById(id)

    override suspend fun incrementViewCount(id: String) = delegate.incrementViewCount(id)

    override suspend fun addDocument(document: Document) = delegate.addDocument(document)

    private fun Flow<List<Document>>.withProgress(): Flow<List<Document>> =
        combine(this, progressRepository.progress) { docs, progressMap ->
            docs.map { doc -> doc.withProgress(progressMap) }
        }

    // Có tiến độ thật thì dùng, chưa có thì giữ nguyên giá trị sẵn có (dữ liệu giả có sẵn vài mức)
    private fun Document.withProgress(progressMap: Map<String, com.example.uthdocument.domain.model.ReadingProgress>): Document =
        progressMap[id]?.let { copy(progress = it.fraction) } ?: this
}

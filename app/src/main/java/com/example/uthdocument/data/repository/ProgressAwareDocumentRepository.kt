package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.ReadingProgress
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.DownloadsRepository
import com.example.uthdocument.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Bọc ngoài repository tài liệu (giả hay Firebase đều được): gắn vào từng tài liệu tiến độ đọc
 * và trạng thái "đã tải về máy" (đều lưu trên máy), đồng thời tự tạo danh sách "Tiếp tục học".
 */
class ProgressAwareDocumentRepository(
    private val delegate: DocumentRepository,
    private val progressRepository: ReadingProgressRepository,
    private val downloadsRepository: DownloadsRepository
) : DocumentRepository {

    override fun getRecentDocuments(): Flow<List<Document>> =
        combine(
            delegate.getDocumentsByCategory(null),
            progressRepository.progress,
            downloadsRepository.downloaded
        ) { docs, progressMap, downloaded ->
            docs
                .map { doc -> doc.withLocalState(progressMap, downloaded) }
                .filter { it.progress > 0f && it.progress < 1f } // đọc xong rồi thì không cần "tiếp tục"
                .sortedByDescending { progressMap[it.id]?.lastReadAt ?: 0L }
        }

    override fun getDocumentsByCategory(category: Category?): Flow<List<Document>> =
        delegate.getDocumentsByCategory(category).withLocalState()

    override fun getDocumentsByCourse(courseId: String): Flow<List<Document>> =
        delegate.getDocumentsByCourse(courseId).withLocalState()

    override fun getDocumentsByUploader(uploaderId: String): Flow<List<Document>> =
        delegate.getDocumentsByUploader(uploaderId).withLocalState()

    override suspend fun deleteDocument(id: String) {
        delegate.deleteDocument(id)
        downloadsRepository.remove(id) // xóa luôn bản tải về trên máy này
    }

    // Màn chi tiết không hiện tiến độ nên không gắn vào, tránh phát lại mỗi lần lật trang
    override fun getDocumentById(id: String): Flow<Document?> = delegate.getDocumentById(id)

    override suspend fun incrementViewCount(id: String) = delegate.incrementViewCount(id)

    override suspend fun addDocument(document: Document) {
        delegate.addDocument(document)
        downloadsRepository.refresh() // tệp vừa đăng đã nằm sẵn trên máy người đăng
    }

    private fun Flow<List<Document>>.withLocalState(): Flow<List<Document>> =
        combine(this, progressRepository.progress, downloadsRepository.downloaded) { docs, progressMap, downloaded ->
            docs.map { doc -> doc.withLocalState(progressMap, downloaded) }
        }

    // Có tiến độ thật thì dùng, chưa có thì giữ giá trị sẵn có (dữ liệu giả có sẵn vài mức)
    private fun Document.withLocalState(
        progressMap: Map<String, ReadingProgress>,
        downloaded: Map<String, Long>
    ): Document = copy(
        progress = progressMap[id]?.fraction ?: progress,
        isDownloaded = id in downloaded
    )
}

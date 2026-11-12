package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeDocumentRepositoryImpl : DocumentRepository {

    override fun getRecentDocuments(): Flow<List<Document>> =
        // Trả về những tài liệu đang học dở
        store.map { docs -> docs.filter { it.progress > 0f } }

    override fun getDocumentsByCategory(category: Category?): Flow<List<Document>> =
        store.map { docs -> if (category == null) docs else docs.filter { it.category == category } }

    override fun getDocumentById(id: String): Flow<Document?> =
        store.map { docs -> docs.find { it.id == id } }

    override suspend fun addDocument(document: Document) {
        // Tài liệu mới đứng đầu danh sách
        store.update { listOf(document) + it }
    }

    private companion object {
        // Dùng chung giữa mọi instance: thêm ở màn này thì Trang chủ / Thư viện thấy ngay.
        // Chỉ lưu trong bộ nhớ -> tắt app là mất (bản Firebase của Bạn B sẽ thay thế).
        val store = MutableStateFlow(
            listOf(
                Document("1", "Bài giảng Kinh tế vận tải", DocumentType.PDF, Category.ECONOMICS, 8.6, true, 0.5f),
                Document("2", "Logistics cơ bản", DocumentType.PDF, Category.LOGISTICS, 12.6, false, 0.2f),
                Document("3", "Pháp luật hàng hải", DocumentType.DOCX, Category.LAW, 2.1, false, 0.0f),
                Document("4", "Bài tập Xác suất thống kê", DocumentType.PDF, Category.ECONOMICS, 6.7, true, 0.8f)
            )
        )
    }
}

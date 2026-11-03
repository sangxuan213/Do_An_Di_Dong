package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeDocumentRepositoryImpl : DocumentRepository {

    private val fakeDocs = listOf(
        Document("1", "Bài giảng Kinh tế vận tải", DocumentType.PDF, Category.ECONOMICS, 8.6, true, 0.5f),
        Document("2", "Logistics cơ bản", DocumentType.PDF, Category.LOGISTICS, 12.6, false, 0.2f),
        Document("3", "Pháp luật hàng hải", DocumentType.DOCX, Category.LAW, 2.1, false, 0.0f),
        Document("4", "Bài tập Xác suất thống kê", DocumentType.PDF, Category.ECONOMICS, 6.7, true, 0.8f)
    )

    override fun getRecentDocuments(): Flow<List<Document>> = flow {
        // Trả về những tài liệu đang học dở
        emit(fakeDocs.filter { it.progress > 0f })
    }

    override fun getDocumentsByCategory(category: Category?): Flow<List<Document>> = flow {
        if (category == null) {
            emit(fakeDocs)
        } else {
            emit(fakeDocs.filter { it.category == category })
        }
    }
}
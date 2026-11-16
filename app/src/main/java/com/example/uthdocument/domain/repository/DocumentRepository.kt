package com.example.uthdocument.domain.repository

import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import kotlinx.coroutines.flow.Flow

/**
 * Interface trừu tượng hóa các thao tác với Document.
 * Tầng UI (ViewModel) sẽ phụ thuộc vào Interface này (Dependency Inversion),
 * thay vì phụ thuộc trực tiếp vào class thực thi.
 */
interface DocumentRepository {
    // Lấy danh sách tài liệu đang học dở (cho trang chủ)
    fun getRecentDocuments(): Flow<List<Document>>

    // Lấy tất cả tài liệu, có thể lọc theo danh mục
    fun getDocumentsByCategory(category: Category? = null): Flow<List<Document>>

    // Lấy một tài liệu theo id (null nếu không tồn tại)
    fun getDocumentById(id: String): Flow<Document?>

    // Thêm một tài liệu mới
    suspend fun addDocument(document: Document)
}
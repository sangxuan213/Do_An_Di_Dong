package com.example.uthdocument.data.repository

import android.net.Uri
import android.util.Log
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.ui.util.normalizedForSearch
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Đọc tài liệu từ Firestore (collection "documents") theo đúng các trường mà Bạn B đã định nghĩa
 * (title, fileUrl, fileName, fileType, fileSize, category, timestamp, courseCode...).
 * Nghe thay đổi theo thời gian thực: có tài liệu mới là danh sách tự cập nhật.
 *
 * Quy ước: courseId của app = courseCode trên Firestore (viết hoa), ví dụ "KTVT".
 */
class FirebaseDocumentRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : DocumentRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Một listener dùng chung cho mọi màn hình; không còn ai xem 5 giây thì tự ngắt. */
    private val allDocuments: Flow<List<Document>> = callbackFlow {
        val registration = db.collection(COLLECTION).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            // Lần đầu có thể nhận bản cache rỗng trước khi dữ liệu máy chủ về -> chờ, tránh nháy "không có tài liệu"
            if (snapshot.metadata.isFromCache && snapshot.isEmpty) return@addSnapshotListener

            trySend(
                snapshot.documents
                    .mapNotNull { it.toDocument() }
                    .sortedByDescending { it.uploadedAt }
            )
        }
        awaitClose { registration.remove() }
    }
        .catch { e ->
            // Thường do Firestore rules chặn đọc hoặc chưa đăng nhập. Xem Logcat với tag FirebaseDocs.
            Log.e(TAG, "Không đọc được collection '$COLLECTION'", e)
            emit(emptyList())
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    // Chưa có dữ liệu tiến độ đọc từ Firebase nên danh sách này sẽ rỗng cho tới khi làm tính năng lưu tiến độ.
    override fun getRecentDocuments(): Flow<List<Document>> =
        allDocuments.map { docs -> docs.filter { it.progress > 0f } }

    override fun getDocumentsByCategory(category: Category?): Flow<List<Document>> =
        allDocuments.map { docs -> if (category == null) docs else docs.filter { it.category == category } }

    override fun getDocumentById(id: String): Flow<Document?> =
        allDocuments.map { docs -> docs.find { it.id == id } }

    override fun getDocumentsByCourse(courseId: String): Flow<List<Document>> =
        allDocuments.map { docs -> docs.filter { it.courseId.equals(courseId, ignoreCase = true) } }

    override suspend fun incrementViewCount(id: String) {
        try {
            db.collection(COLLECTION).document(id)
                .update("viewCount", FieldValue.increment(1))
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Không có quyền ghi hoặc mất mạng thì bỏ qua, không làm hỏng việc xem tài liệu
            Log.w(TAG, "Không tăng được lượt xem của $id", e)
        }
    }

    /** Tải tệp (document.filePath) lên Storage rồi ghi thông tin vào Firestore. Lỗi sẽ được ném ra cho giao diện báo. */
    override suspend fun addDocument(document: Document) {
        val localPath = document.filePath ?: throw IllegalArgumentException("Tài liệu chưa có tệp để tải lên")
        val localFile = File(localPath)
        val extension = document.type.extension.lowercase()

        val storageRef = storage.reference.child("study_materials/${document.category.name}/${document.id}.$extension")
        storageRef.putFile(Uri.fromFile(localFile)).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        // Cùng cấu trúc với DocumentItem của Bạn B, thêm viewCount
        val data = hashMapOf<String, Any>(
            "id" to document.id,
            "title" to document.title,
            "description" to "",
            "fileUrl" to downloadUrl,
            "fileName" to "${document.title}.$extension",
            "fileType" to document.type.name,
            "fileSize" to localFile.length(),
            "category" to document.category.name,
            "uploadedBy" to auth.currentUser?.uid.orEmpty(),
            "timestamp" to if (document.uploadedAt > 0) document.uploadedAt else System.currentTimeMillis(),
            "faculty" to "",
            "courseCode" to document.courseId.orEmpty(),
            "qrCodeHash" to "UTH_DOC_${document.id}",
            "viewCount" to 0
        )
        db.collection(COLLECTION).document(document.id).set(data).await()
    }

    // ---------- chuyển một document Firestore thành Document của app ----------

    private fun DocumentSnapshot.toDocument(): Document? {
        val title = getString("title")?.takeIf { it.isNotBlank() } ?: return null
        val sizeBytes = (get("fileSize") as? Number)?.toLong() ?: 0L

        return Document(
            id = id,
            title = title,
            type = parseType(getString("fileType").orEmpty(), getString("fileName").orEmpty()),
            category = parseCategory(getString("category").orEmpty()),
            sizeInMb = sizeBytes / 1024.0 / 1024.0,
            courseId = getString("courseCode")?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
            uploadedAt = parseTime(get("timestamp")),
            viewCount = (get("viewCount") as? Number)?.toInt() ?: 0,
            fileUrl = getString("fileUrl")?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseType(fileType: String, fileName: String): DocumentType {
        val key = fileType.trim().ifEmpty { fileName.substringAfterLast('.', "") }.lowercase()
        return when (key) {
            "docx", "doc" -> DocumentType.DOCX
            "pptx", "ppt" -> DocumentType.PPTX
            else -> DocumentType.PDF
        }
    }

    /** Khớp theo tên enum hoặc tên hiển thị (không phân biệt hoa thường, dấu); không khớp thì vào "Khác". */
    private fun parseCategory(raw: String): Category {
        val key = raw.trim().normalizedForSearch()
        return Category.entries.firstOrNull {
            it.name.lowercase() == key || it.displayName.normalizedForSearch() == key
        } ?: Category.OTHER
    }

    /** timestamp có thể là số (mili-giây) hoặc kiểu Timestamp của Firestore tùy cách nhập dữ liệu. */
    private fun parseTime(raw: Any?): Long = when (raw) {
        is Number -> raw.toLong()
        is Timestamp -> raw.toDate().time
        else -> 0L
    }

    private companion object {
        const val COLLECTION = "documents"
        const val TAG = "FirebaseDocs"
    }
}

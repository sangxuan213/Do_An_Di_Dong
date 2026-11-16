package com.example.uthdocument.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.example.uthdocument.model.DocumentItem
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DocumentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun getDocumentsByFaculty(faculty: String): List<DocumentItem> {
        val query = if (faculty.isBlank()) {
            db.collection("documents").orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            db.collection("documents").whereEqualTo("faculty", faculty)
        }
        return query.get().await().toObjects(DocumentItem::class.java)
    }

    suspend fun searchDocuments(keyword: String): List<DocumentItem> {
        val allDocs = db.collection("documents").get().await().toObjects(DocumentItem::class.java)
        return allDocs.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                    it.courseCode.contains(keyword, ignoreCase = true)
        }
    }

    suspend fun getDocumentByQrCode(qrHash: String): DocumentItem? {
        val snapshot = db.collection("documents")
            .whereEqualTo("qrCodeHash", qrHash)
            .limit(1)
            .get()
            .await()
        return snapshot.toObjects(DocumentItem::class.java).firstOrNull()
    }

    suspend fun uploadDocument(fileUri: Uri, document: DocumentItem): Result<DocumentItem> {
        return try {
            val fileId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("study_materials/${document.faculty}/$fileId")
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val newDoc = document.copy(
                id = fileId,
                fileUrl = downloadUrl,
                qrCodeHash = "UTH_DOC_$fileId"
            )
            db.collection("documents").document(fileId).set(newDoc).await()
            Result.success(newDoc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
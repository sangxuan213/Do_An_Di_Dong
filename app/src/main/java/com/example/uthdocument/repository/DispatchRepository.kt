package com.example.uthdocument.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.uthdocument.model.OfficialDispatch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DispatchRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun getDispatches(type: String? = null): List<OfficialDispatch> {
        val query = if (type != null) {
            db.collection("dispatches").whereEqualTo("type", type)
        } else {
            db.collection("dispatches")
        }
        return query.get().await().toObjects(OfficialDispatch::class.java)
    }

    suspend fun updateDispatchProgress(dispatchId: String, newStatus: String, note: String) {
        val docRef = db.collection("dispatches").document(dispatchId)
        docRef.update("status", newStatus).await()
        if (note.isNotBlank()) {
            docRef.update("annotations", FieldValue.arrayUnion(note)).await()
        }
    }

    suspend fun signAndApproveDispatch(dispatchId: String, signedPdfUri: Uri, signerNote: String): Result<String> {
        return try {
            val fileName = "signed_${dispatchId}_${UUID.randomUUID()}.pdf"
            val storageRef = storage.reference.child("official_dispatches/signed/$fileName")
            storageRef.putFile(signedPdfUri).await()
            val signedUrl = storageRef.downloadUrl.await().toString()

            val updates = mutableMapOf<String, Any>(
                "isSigned" to true,
                "status" to "APPROVED",
                "signedFileUrl" to signedUrl
            )
            db.collection("dispatches").document(dispatchId).update(updates).await()
            if (signerNote.isNotBlank()) {
                db.collection("dispatches").document(dispatchId)
                    .update("annotations", FieldValue.arrayUnion("Đã ký duyệt: $signerNote")).await()
            }
            Result.success(signedUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
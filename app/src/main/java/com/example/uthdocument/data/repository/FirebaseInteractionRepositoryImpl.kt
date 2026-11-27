package com.example.uthdocument.data.repository

import android.util.Log
import com.example.uthdocument.domain.model.Comment
import com.example.uthdocument.domain.model.DocumentStats
import com.example.uthdocument.domain.repository.InteractionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await

/**
 * Thích, đánh giá sao và bình luận qua Firestore:
 *   documents/{docId}                  likeCount, ratingSum, ratingCount   (số liệu gộp, cập nhật bằng transaction)
 *   documents/{docId}/comments/{id}    author, authorId, content, timestamp
 *   users/{uid}/interactions/{docId}   liked, rating                        (phần riêng của từng người)
 */
class FirebaseInteractionRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : InteractionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private class Counters(val likeCount: Int, val ratingSum: Int, val ratingCount: Int)
    private class Mine(val liked: Boolean, val rating: Int)

    private fun currentUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    private val counters: Flow<Map<String, Counters>> = callbackFlow {
        val registration = db.collection(DOCUMENTS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            trySend(
                snapshot.documents.associate { d ->
                    d.id to Counters(
                        likeCount = (d.get("likeCount") as? Number)?.toInt() ?: 0,
                        ratingSum = (d.get("ratingSum") as? Number)?.toInt() ?: 0,
                        ratingCount = (d.get("ratingCount") as? Number)?.toInt() ?: 0
                    )
                }
            )
        }
        awaitClose { registration.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mine: Flow<Map<String, Mine>> = currentUid().flatMapLatest<String?, Map<String, Mine>> { uid ->
        if (uid == null) {
            flowOf<Map<String, Mine>>(emptyMap())
        } else {
            callbackFlow<Map<String, Mine>> {
                val registration = db.collection(USERS).document(uid).collection(INTERACTIONS)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener
                        trySend(
                            snapshot.documents.associate { d ->
                                d.id to Mine(
                                    liked = d.getBoolean("liked") == true,
                                    rating = (d.get("rating") as? Number)?.toInt() ?: 0
                                )
                            }
                        )
                    }
                awaitClose { registration.remove() }
            }
        }
    }

    /** Một listener dùng chung cho mọi màn hình; không còn ai xem 5 giây thì tự ngắt. */
    private val allStats: Flow<Map<String, DocumentStats>> = combine(counters, mine) { c, m ->
        c.mapValues { (id, counter) ->
            val my = m[id]
            DocumentStats(
                likeCount = counter.likeCount.coerceAtLeast(0),
                isLiked = my?.liked == true,
                ratingAverage = if (counter.ratingCount > 0) counter.ratingSum.toDouble() / counter.ratingCount else 0.0,
                ratingCount = counter.ratingCount.coerceAtLeast(0),
                myRating = my?.rating ?: 0
            )
        }
    }
        .catch { e ->
            // Thường do Firestore rules chặn đọc. Xem Logcat với tag FirebaseInteraction.
            Log.e(TAG, "Không đọc được số liệu tương tác", e)
            emit(emptyMap())
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun getStats(documentId: String): Flow<DocumentStats> =
        allStats.map { it[documentId] ?: DocumentStats() }.distinctUntilChanged()

    override fun getAllStats(): Flow<Map<String, DocumentStats>> = allStats

    override fun getComments(documentId: String): Flow<List<Comment>> = callbackFlow {
        val registration = db.collection(DOCUMENTS).document(documentId).collection(COMMENTS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Không đọc được bình luận của $documentId", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(
                    snapshot.documents.map { d ->
                        Comment(
                            id = d.id,
                            documentId = documentId,
                            author = d.getString("author").orEmpty().ifBlank { "Người dùng" },
                            content = d.getString("content").orEmpty(),
                            timestamp = (d.get("timestamp") as? Number)?.toLong() ?: 0L,
                            authorId = d.getString("authorId").orEmpty()
                        )
                    }
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun toggleLike(documentId: String) {
        val uid = requireMe()
        val docRef = db.collection(DOCUMENTS).document(documentId)
        val myRef = db.collection(USERS).document(uid).collection(INTERACTIONS).document(documentId)
        db.runTransaction<Void?> { tx ->
            val liked = tx.get(myRef).getBoolean("liked") == true
            tx.set(myRef, mapOf("liked" to !liked), SetOptions.merge())
            tx.update(docRef, "likeCount", FieldValue.increment(if (liked) -1L else 1L))
            null
        }.await()
    }

    override suspend fun setRating(documentId: String, stars: Int) {
        val uid = requireMe()
        val newStars = stars.coerceIn(0, 5)
        val docRef = db.collection(DOCUMENTS).document(documentId)
        val myRef = db.collection(USERS).document(uid).collection(INTERACTIONS).document(documentId)
        db.runTransaction<Void?> { tx ->
            val old = (tx.get(myRef).get("rating") as? Number)?.toInt() ?: 0
            val rating: Any = if (newStars > 0) newStars else FieldValue.delete()
            tx.set(myRef, mapOf("rating" to rating), SetOptions.merge())
            tx.update(
                docRef,
                "ratingSum", FieldValue.increment((newStars - old).toLong()),
                "ratingCount", FieldValue.increment(
                    ((if (newStars > 0) 1 else 0) - (if (old > 0) 1 else 0)).toLong()
                )
            )
            null
        }.await()
    }

    override suspend fun addComment(documentId: String, author: String, content: String) {
        val uid = requireMe()
        db.collection(DOCUMENTS).document(documentId).collection(COMMENTS)
            .add(
                mapOf(
                    "author" to author,
                    "authorId" to uid,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            .await()
    }

    override suspend fun deleteComment(documentId: String, commentId: String) {
        requireMe()
        db.collection(DOCUMENTS).document(documentId).collection(COMMENTS).document(commentId).delete().await()
    }

    private fun requireMe(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Bạn chưa đăng nhập.")

    private companion object {
        const val TAG = "FirebaseInteraction"
        const val DOCUMENTS = "documents"
        const val USERS = "users"
        const val INTERACTIONS = "interactions"
        const val COMMENTS = "comments"
    }
}

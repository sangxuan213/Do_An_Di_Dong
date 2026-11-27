package com.example.uthdocument.data.repository

import android.util.Log
import com.example.uthdocument.domain.repository.FollowRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await

/**
 * Theo dõi qua Firestore:
 *   users/{me}/following/{uid}     followedAt   (mình theo dõi ai; thông báo chỉ tính bài đăng SAU followedAt)
 *   users/{uid}/followers/{me}     followedAt   (bản sao để đếm người theo dõi)
 */
class FirebaseFollowRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FollowRepository {

    private fun currentUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun followingIds(): Flow<Set<String>> = currentUid().flatMapLatest<String?, Set<String>> { me ->
        if (me == null) {
            flowOf(emptySet())
        } else {
            callbackFlow<Set<String>> {
                val registration = db.collection(USERS).document(me).collection(FOLLOWING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Không đọc được danh sách đang theo dõi", error)
                            trySend(emptySet())
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener
                        trySend(snapshot.documents.map { it.id }.toSet())
                    }
                awaitClose { registration.remove() }
            }
        }
    }

    override fun followerCount(uid: String): Flow<Int> = callbackFlow {
        val registration = db.collection(USERS).document(uid).collection(FOLLOWERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Không đếm được người theo dõi của $uid", error)
                    trySend(0)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(snapshot.size())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun follow(uid: String) {
        val me = requireMe()
        require(uid != me) { "Không thể tự theo dõi chính mình." }
        val data = mapOf("followedAt" to System.currentTimeMillis())
        db.runBatch { batch ->
            batch.set(db.collection(USERS).document(me).collection(FOLLOWING).document(uid), data)
            batch.set(db.collection(USERS).document(uid).collection(FOLLOWERS).document(me), data)
        }.await()
    }

    override suspend fun unfollow(uid: String) {
        val me = requireMe()
        db.runBatch { batch ->
            batch.delete(db.collection(USERS).document(me).collection(FOLLOWING).document(uid))
            batch.delete(db.collection(USERS).document(uid).collection(FOLLOWERS).document(me))
        }.await()
    }

    private fun requireMe(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Bạn chưa đăng nhập.")

    private companion object {
        const val TAG = "FirebaseFollow"
        const val USERS = "users"
        const val FOLLOWING = "following"
        const val FOLLOWERS = "followers"
    }
}

package com.example.uthdocument.data.repository

import android.util.Log
import com.example.uthdocument.data.AppContainer
import com.example.uthdocument.domain.model.ChatMessage
import com.example.uthdocument.domain.model.Conversation
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.ProfileRepository
import com.example.uthdocument.fcm.PushNotifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Tin nhắn 1-1 qua Firestore:
 *   chats/{idA_idB}            participants, participantNames, lastMessage, lastSenderId, updatedAt, lastReadAt{uid}
 *   chats/{idA_idB}/messages   senderId, text, timestamp
 * Id cuộc trò chuyện là hai uid sắp theo thứ tự rồi nối bằng "_" nên mỗi cặp người chỉ có một cuộc trò chuyện.
 */
class FirebaseChatRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val profileRepository: ProfileRepository = AppContainer.profileRepository
) : ChatRepository {

    private fun requireMe(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Bạn chưa đăng nhập.")

    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val me = auth.currentUser?.uid
        if (me == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = db.collection(CHATS)
            .whereArrayContains("participants", me)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Không đọc được danh sách trò chuyện", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(
                    snapshot.documents
                        .mapNotNull { it.toConversation(me) }
                        .sortedByDescending { it.updatedAt }
                )
            }
        awaitClose { registration.remove() }
    }

    override fun getConversation(chatId: String): Flow<Conversation?> = callbackFlow {
        val me = auth.currentUser?.uid
        if (me == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val registration = db.collection(CHATS).document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Không đọc được cuộc trò chuyện $chatId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toConversation(me))
            }
        awaitClose { registration.remove() }
    }

    override fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection(CHATS).document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Không đọc được tin nhắn của $chatId", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(
                    snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId").orEmpty(),
                            senderName = doc.getString("senderName").orEmpty(),
                            text = text,
                            timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L
                        )
                    }.reversed() // đang lấy mới -> cũ, đảo lại thành cũ -> mới
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun openChat(otherUserId: String, otherName: String): String {
        val me = requireMe()
        val chatId = listOf(me, otherUserId).sorted().joinToString("_")
        val myName = profileRepository.getCurrentUser().first().fullName
        try {
            db.collection(CHATS).document(chatId).set(
                mapOf(
                    "participants" to listOf(me, otherUserId),
                    "participantNames" to mapOf(me to myName, otherUserId to otherName)
                ),
                SetOptions.merge()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Không tạo được cuộc trò chuyện", e)
            throw IllegalStateException("Không mở được cuộc trò chuyện. Hãy kiểm tra mạng và Firestore rules cho collection chats.", e)
        }
        return chatId
    }

    override suspend fun sendMessage(chatId: String, text: String) {
        val me = requireMe()
        val now = System.currentTimeMillis()
        val chatRef = db.collection(CHATS).document(chatId)
        val messageRef = chatRef.collection("messages").document()
        try {
            db.runBatch { batch ->
                batch.set(messageRef, mapOf("senderId" to me, "text" to text, "timestamp" to now))
                batch.update(
                    chatRef,
                    mapOf(
                        "lastMessage" to text.take(200),
                        "lastSenderId" to me,
                        "updatedAt" to now,
                        "lastReadAt.$me" to now
                    )
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gửi tin nhắn thất bại", e)
            throw IllegalStateException("Không gửi được tin nhắn. Hãy kiểm tra mạng và Firestore rules.", e)
        }
        // Nhờ Worker đẩy thông báo tới người nhận (chạy ngầm, lỗi không ảnh hưởng việc gửi tin)
        PushNotifier.notifyChat(chatId)
    }

    override suspend fun markRead(chatId: String) {
        val me = auth.currentUser?.uid ?: return
        try {
            db.collection(CHATS).document(chatId).update("lastReadAt.$me", System.currentTimeMillis()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Không đánh dấu đã đọc được", e)
        }
    }

    private fun DocumentSnapshot.toConversation(me: String): Conversation? {
        if (!exists()) return null
        val participants = (get("participants") as? List<*>)?.filterIsInstance<String>() ?: return null
        val otherId = participants.firstOrNull { it != me } ?: return null
        val names = get("participantNames") as? Map<*, *>
        val lastRead = ((get("lastReadAt") as? Map<*, *>)?.get(me) as? Number)?.toLong() ?: 0L
        val updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0L
        val lastSender = getString("lastSenderId").orEmpty()

        return Conversation(
            id = id,
            otherUserId = otherId,
            otherName = (names?.get(otherId) as? String).orEmpty().ifBlank { "Người dùng" },
            lastMessage = getString("lastMessage").orEmpty(),
            lastSenderId = lastSender,
            updatedAt = updatedAt,
            unread = lastSender.isNotEmpty() && lastSender != me && updatedAt > lastRead
        )
    }

    private companion object {
        const val CHATS = "chats"
        const val TAG = "FirebaseChat"
    }
}

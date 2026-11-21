package com.example.uthdocument.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.domain.model.ChatMessage
import com.example.uthdocument.domain.model.ChatRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    val currentUid: String
        get() = auth.currentUser?.uid.orEmpty()

    val currentEmail: String
        get() = auth.currentUser?.email.orEmpty()

    val chatRooms: StateFlow<List<ChatRoom>> = callbackFlow {
        val listenerRegistration = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid.orEmpty()
            if (uid.isBlank()) {
                trySend(emptyList())
            }
        }
        auth.addAuthStateListener(listenerRegistration)

        val uid = currentUid
        if (uid.isBlank()) {
            trySend(emptyList())
            awaitClose { auth.removeAuthStateListener(listenerRegistration) }
            return@callbackFlow
        }

        val chatListener = firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Lỗi lấy danh sách chat: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val rooms = snapshot.documents.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    val names = doc.get("participantNames") as? Map<*, *>
                    ChatRoom(
                        id = doc.id,
                        participants = participants?.mapNotNull { it?.toString() }.orEmpty(),
                        lastMessage = doc.getString("lastMessage").orEmpty(),
                        lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                        participantNames = names?.entries
                            ?.associate { it.key.toString() to it.value.toString() }
                            .orEmpty()
                    )
                }.sortedByDescending { it.lastMessageTimestamp }

                trySend(rooms)
            }

        awaitClose {
            chatListener.remove()
            auth.removeAuthStateListener(listenerRegistration)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val cleanChatId = chatId.trim()
        if (cleanChatId.isEmpty()) {
            Log.w("ChatViewModel", "getMessages: chatId rỗng")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(cleanChatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Lỗi đọc tin nhắn phòng $cleanChatId:${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val msgs = snapshot.documents.mapNotNull { doc ->
                    ChatMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId").orEmpty(),
                        senderName = doc.getString("senderName").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }
                trySend(msgs)
            }

        awaitClose { listener.remove() }
    }

    fun sendMessage(chatId: String, recipientUid: String, recipientName: String, text: String) {
        val messageText = text.trim()
        if (messageText.isEmpty()) return

        val myUid = currentUid.trim()
        if (myUid.isEmpty()) {
            Log.e("ChatViewModel", "Không gửi được tin nhắn: người dùng chưa đăng nhập")
            return
        }

        val cleanRecipientUid = recipientUid.trim()
        val targetChatId = resolveChatId(
            rawChatId = chatId,
            myUid = myUid,
            recipientUid = cleanRecipientUid
        )
        val effectiveRecipientUid = cleanRecipientUid.ifEmpty {
            targetChatId.split("_").firstOrNull { it.isNotBlank() && it != myUid }.orEmpty()
        }

        if (effectiveRecipientUid.isEmpty()) {
            Log.e("ChatViewModel", "Không gửi được tin nhắn: thiếu recipientUid")
            return
        }

        val targetRecipientName = recipientName.trim().ifEmpty { "Người dùng UTH" }

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(myUid).get().await()
                val profileName = userDoc.getString("fullName").orEmpty().trim()
                val myName = profileName.ifEmpty {
                    currentEmail.substringBefore('@').trim().ifEmpty { "Sinh viên UTH" }
                }

                val timestamp = System.currentTimeMillis()
                val msgData = hashMapOf(
                    "senderId" to myUid,
                    "senderName" to myName,
                    "text" to messageText,
                    "timestamp" to timestamp
                )

                firestore.collection("chats")
                    .document(targetChatId)
                    .collection("messages")
                    .add(msgData)
                    .await()

                val roomData = hashMapOf(
                    "participants" to listOf(myUid, effectiveRecipientUid).distinct().sorted(),
                    "lastMessage" to messageText,
                    "lastMessageTimestamp" to timestamp,
                    "participantNames" to mapOf(
                        myUid to myName,
                        effectiveRecipientUid to targetRecipientName
                    )
                )

                firestore.collection("chats")
                    .document(targetChatId)
                    .set(roomData, SetOptions.merge())
                    .await()

                Log.d("ChatViewModel", "Gửi tin nhắn thành công tới phòng $targetChatId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi gửi tin nhắn: ${e.message}", e)
            }
        }
    }

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            val clean1 = uid1.trim()
            val clean2 = uid2.trim()
            return listOf(clean1, clean2)
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
                .joinToString("_")
        }

        fun resolveChatId(rawChatId: String, myUid: String, recipientUid: String): String {
            val cleanRoomId = rawChatId.trim()
            if (cleanRoomId.isNotEmpty()) {
                val parts = cleanRoomId.split("_").filter { it.isNotBlank() }.distinct().sorted()
                if (parts.size == 2) {
                    return parts.joinToString("_")
                }
                return cleanRoomId
            }
            return getChatId(myUid, recipientUid)
        }
    }
}
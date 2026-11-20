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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // 1. Danh sách cuộc trò chuyện của người dùng hiện tại
    val chatRooms: StateFlow<List<ChatRoom>> = callbackFlow {
        if (currentUid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Lỗi lấy danh sách chat: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val rooms = snapshot.documents.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    val names = doc.get("participantNames") as? Map<*, *>
                    ChatRoom(
                        id = doc.id,
                        participants = participants?.mapNotNull { it.toString() }.orEmpty(),
                        lastMessage = doc.getString("lastMessage").orEmpty(),
                        lastMessageTimestamp = doc.getLong("lastMessageTimestamp") ?: 0L,
                        participantNames = names?.entries?.associate { it.key.toString() to it.value.toString() }.orEmpty()
                    )
                }.sortedByDescending { it.lastMessageTimestamp }

                trySend(rooms)
            }

        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 2. Lấy tin nhắn theo chatId (realtime)
    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    // 3. Gửi tin nhắn 1-1
    fun sendMessage(chatId: String, recipientUid: String, recipientName: String, text: String) {
        val messageText = text.trim()
        if (messageText.isEmpty()) return

        val myUid = currentUid.ifBlank { "anonymous_user" }
        val targetRecipientUid = recipientUid.ifBlank { "admin_uth_system" }
        val targetChatId = if (chatId.isNotBlank()) chatId else getChatId(myUid, targetRecipientUid)
        val myName = currentEmail.substringBefore('@').ifBlank { "Sinh viên UTH" }
        val targetRecipientName = recipientName.ifBlank { "Giảng viên / Admin UTH" }

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val msgData = hashMapOf(
                    "senderId" to myUid,
                    "senderName" to myName,
                    "text" to messageText,
                    "timestamp" to timestamp
                )

                // 1. Thêm tin nhắn vào subcollection messages
                firestore.collection("chats")
                    .document(targetChatId)
                    .collection("messages")
                    .add(msgData)
                    .await()

                // 2. Cập nhật thông tin phòng chat
                val roomData = hashMapOf(
                    "participants" to listOf(myUid, targetRecipientUid),
                    "lastMessage" to messageText,
                    "lastMessageTimestamp" to timestamp,
                    "participantNames" to mapOf(
                        myUid to myName,
                        targetRecipientUid to targetRecipientName
                    )
                )
                firestore.collection("chats")
                    .document(targetChatId)
                    .set(roomData, SetOptions.merge())
                    .await()

                Log.d("ChatViewModel", "Gửi tin nhắn thành công tới $targetChatId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi gửi tin nhắn: ${e.message}", e)
            }
        }
    }

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }
}

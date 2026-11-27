# File có vẻ thừa (không file nào mình đọc được còn dùng)

Xóa bằng Android Studio: chuột phải file > Delete > để ô "Safe delete (with usage search)" được tick.
Nếu nó báo còn nơi dùng thì GIỮ LẠI.

- ui/chat/ChatListScreen.kt, ChatDetailScreen.kt, ChatViewModel.kt  (app dùng ConversationsScreen, ChatScreen, ChatViewModels)
- ui/mydocuments/MyDocumentsScreen.kt, MyDocumentsViewModel.kt      (app dùng bản trong ui/social)

KHÔNG xóa repository/AuthRepository.kt: FirebaseProfileRepositoryImpl đang dùng.
Chưa kết luận được: repository/DispatchRepository.kt, repository/DocumentRepository.kt, model/Models.kt
(chưa đọc màn Login/Register/Admin nên không chắc có dùng không; Safe Delete sẽ cho biết).

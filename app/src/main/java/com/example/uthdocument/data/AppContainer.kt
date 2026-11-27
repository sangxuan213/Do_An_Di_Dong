package com.example.uthdocument.data

import android.content.Context
import com.example.uthdocument.data.repository.FakeChatRepositoryImpl
import com.example.uthdocument.data.repository.FakeCourseRepositoryImpl
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.data.repository.FakeFollowRepositoryImpl
import com.example.uthdocument.data.repository.FakeInteractionRepositoryImpl
import com.example.uthdocument.data.repository.FakeProfileRepositoryImpl
import com.example.uthdocument.data.repository.FakeSessionRepositoryImpl
import com.example.uthdocument.data.repository.FakeUserRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseChatRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseCourseRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseDocumentRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseFollowRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseInteractionRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseProfileRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseSessionRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseUserRepositoryImpl
import com.example.uthdocument.data.repository.LocalDownloadsRepository
import com.example.uthdocument.data.repository.NotificationSeenStore
import com.example.uthdocument.data.repository.LocalReadingProgressRepository
import com.example.uthdocument.data.repository.ProgressAwareDocumentRepository
import com.example.uthdocument.domain.repository.ChatRepository
import com.example.uthdocument.domain.repository.CourseRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.DownloadsRepository
import com.example.uthdocument.domain.repository.FollowRepository
import com.example.uthdocument.domain.repository.InteractionRepository
import com.example.uthdocument.domain.repository.ProfileRepository
import com.example.uthdocument.domain.repository.ReadingProgressRepository
import com.example.uthdocument.domain.repository.SessionRepository
import com.example.uthdocument.domain.repository.UserRepository

/** Nơi duy nhất quyết định dùng bản Firebase hay bản giả. Đổi cờ trong AppConfig là xong. */
object AppContainer {

    private lateinit var appContext: Context

    /** Gọi một lần trong MainActivity.onCreate, trước setContent. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val sessionRepository: SessionRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH) FakeSessionRepositoryImpl() else FirebaseSessionRepositoryImpl()
    }

    val profileRepository: ProfileRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH) FakeProfileRepositoryImpl() else FirebaseProfileRepositoryImpl()
    }

    val readingProgressRepository: ReadingProgressRepository by lazy {
        LocalReadingProgressRepository.getInstance(appContext)
    }

    /** Các tài liệu đã tải về máy (đọc offline). */
    val downloadsRepository: DownloadsRepository by lazy {
        LocalDownloadsRepository.getInstance(appContext)
    }

    /** Dùng chung một instance để mọi màn hình chia sẻ cùng một listener Firestore, tiến độ đọc và trạng thái tải về. */
    val documentRepository: DocumentRepository by lazy {
        val base = if (AppConfig.USE_FAKE_DOCUMENTS) FakeDocumentRepositoryImpl() else FirebaseDocumentRepositoryImpl()
        ProgressAwareDocumentRepository(base, readingProgressRepository, downloadsRepository)
    }

    /** Người dùng hiện tại và hồ sơ công khai của người khác (theo cờ USE_FAKE_AUTH). */
    val userRepository: UserRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH) FakeUserRepositoryImpl() else FirebaseUserRepositoryImpl()
    }

    /** Môn học: đọc từ Firestore (collection "courses"), trống thì dùng danh sách mặc định. */
    val courseRepository: CourseRepository by lazy {
        if (AppConfig.USE_FAKE_DOCUMENTS) FakeCourseRepositoryImpl() else FirebaseCourseRepositoryImpl()
    }

    /** Thích, đánh giá sao, bình luận. Dùng bản giả khi tài liệu hoặc đăng nhập đang là giả. */
    val interactionRepository: InteractionRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH || AppConfig.USE_FAKE_DOCUMENTS) FakeInteractionRepositoryImpl()
        else FirebaseInteractionRepositoryImpl()
    }

    /** Mốc đã xem màn Thông báo (chấm đỏ ở chuông trang chủ). */
    val notificationSeenStore: NotificationSeenStore by lazy { NotificationSeenStore(appContext) }

    /** Theo dõi người dùng (theo cờ USE_FAKE_AUTH). Thông báo bài mới do FollowNotificationWorker đảm nhiệm. */
    val followRepository: FollowRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH) FakeFollowRepositoryImpl() else FirebaseFollowRepositoryImpl()
    }

    /** Tin nhắn 1-1 (theo cờ USE_FAKE_AUTH). */
    val chatRepository: ChatRepository by lazy {
        if (AppConfig.USE_FAKE_AUTH) FakeChatRepositoryImpl() else FirebaseChatRepositoryImpl()
    }
}

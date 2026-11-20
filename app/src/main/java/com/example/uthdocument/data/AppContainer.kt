package com.example.uthdocument.data

import android.content.Context
import com.example.uthdocument.data.repository.FakeDocumentRepositoryImpl
import com.example.uthdocument.data.repository.FakeProfileRepositoryImpl
import com.example.uthdocument.data.repository.FakeSessionRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseDocumentRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseProfileRepositoryImpl
import com.example.uthdocument.data.repository.FirebaseSessionRepositoryImpl
import com.example.uthdocument.data.repository.LocalReadingProgressRepository
import com.example.uthdocument.data.repository.ProgressAwareDocumentRepository
import com.example.uthdocument.domain.repository.DocumentRepository
import com.example.uthdocument.domain.repository.ProfileRepository
import com.example.uthdocument.domain.repository.ReadingProgressRepository
import com.example.uthdocument.domain.repository.SessionRepository

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

    /** Dùng chung một instance để mọi màn hình chia sẻ cùng một listener Firestore và cùng tiến độ đọc. */
    val documentRepository: DocumentRepository by lazy {
        val base = if (AppConfig.USE_FAKE_DOCUMENTS) FakeDocumentRepositoryImpl() else FirebaseDocumentRepositoryImpl()
        ProgressAwareDocumentRepository(base, readingProgressRepository)
    }
}

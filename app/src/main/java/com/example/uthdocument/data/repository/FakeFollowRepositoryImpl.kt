package com.example.uthdocument.data.repository

import com.example.uthdocument.domain.repository.FollowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Bản giả cho chế độ chạy thử (chỉ nằm trong bộ nhớ, không có thông báo). */
class FakeFollowRepositoryImpl : FollowRepository {
    private val following = MutableStateFlow<Set<String>>(emptySet())

    override fun followingIds(): Flow<Set<String>> = following

    override fun followerCount(uid: String): Flow<Int> = following.map { if (uid in it) 1 else 0 }

    override suspend fun follow(uid: String) = following.update { it + uid }

    override suspend fun unfollow(uid: String) = following.update { it - uid }
}

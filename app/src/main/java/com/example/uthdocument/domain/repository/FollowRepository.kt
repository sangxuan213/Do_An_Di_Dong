package com.example.uthdocument.domain.repository

import kotlinx.coroutines.flow.Flow

/** Theo dõi người dùng khác: có bài mới của người được theo dõi thì app báo bằng thông báo. */
interface FollowRepository {
    /** Id những người mình đang theo dõi. */
    fun followingIds(): Flow<Set<String>>

    /** Số người đang theo dõi [uid]. */
    fun followerCount(uid: String): Flow<Int>

    suspend fun follow(uid: String)

    suspend fun unfollow(uid: String)
}

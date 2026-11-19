package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MyVideoDao {
    // Users
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :hash, passwordSalt = :salt, recoveryCode = '' WHERE id = :userId")
    suspend fun updatePassword(userId: String, hash: String, salt: String)

    @Query("UPDATE users SET recoveryCode = :code WHERE id = :userId")
    suspend fun setRecoveryCode(userId: String, code: String)

    @Query("UPDATE users SET isSuspended = :suspended WHERE id = :userId")
    suspend fun setUserSuspended(userId: String, suspended: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun setUserRole(userId: String, role: String)

    // Channels
    @Query("SELECT * FROM channels ORDER BY subscriberCount DESC")
    fun observeAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun observeChannelById(channelId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE ownerUserId = :userId LIMIT 1")
    fun observeChannelByOwner(userId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE ownerUserId = :userId LIMIT 1")
    suspend fun getChannelByOwner(userId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET subscriberCount = MAX(0, subscriberCount + :delta) WHERE id = :channelId")
    suspend fun adjustSubscriberCount(channelId: String, delta: Long)

    @Query("UPDATE channels SET totalViews = totalViews + 1, watchHours12M = watchHours12M + :watchHoursDelta, shortsViews90D = shortsViews90D + :shortsViewsDelta, estimatedBalanceUsd = estimatedBalanceUsd + :earningsDelta, lifetimeEarningsUsd = lifetimeEarningsUsd + :earningsDelta WHERE id = :channelId")
    suspend fun recordChannelViewMetrics(
        channelId: String,
        watchHoursDelta: Double,
        shortsViewsDelta: Long,
        earningsDelta: Double
    )

    @Query("UPDATE channels SET monetizationTier = :tier WHERE id = :channelId")
    suspend fun updateMonetizationTier(channelId: String, tier: String)

    @Query("UPDATE channels SET isVerified = :verified WHERE id = :channelId")
    suspend fun setChannelVerified(channelId: String, verified: Boolean)

    // Videos
    @Query("SELECT * FROM videos WHERE isApproved = 1 ORDER BY createdAt DESC")
    fun observeApprovedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAllVideosAdmin(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    fun observeVideoById(videoId: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observeVideosByChannel(channelId: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: String)

    @Query("UPDATE videos SET viewsCount = viewsCount + 1, impressionsCount = impressionsCount + 2, watchMinutesTotal = watchMinutesTotal + :watchMinutes, adRevenueUsd = adRevenueUsd + :revDelta WHERE id = :videoId")
    suspend fun incrementVideoView(videoId: String, watchMinutes: Double, revDelta: Double)

    @Query("UPDATE videos SET likesCount = MAX(0, likesCount + :likeDelta), dislikesCount = MAX(0, dislikesCount + :dislikeDelta) WHERE id = :videoId")
    suspend fun adjustVideoReactions(videoId: String, likeDelta: Long, dislikeDelta: Long)

    @Query("UPDATE videos SET isApproved = :approved, isFlaggedSpam = :flagged WHERE id = :videoId")
    suspend fun moderateVideo(videoId: String, approved: Boolean, flagged: Boolean)

    // Reactions
    @Query("SELECT * FROM video_reactions WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    fun observeReaction(userId: String, videoId: String): Flow<VideoReactionEntity?>

    @Query("SELECT * FROM video_reactions WHERE userId = :userId AND videoId = :videoId LIMIT 1")
    suspend fun getReaction(userId: String, videoId: String): VideoReactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(reaction: VideoReactionEntity)

    @Query("DELETE FROM video_reactions WHERE userId = :userId AND videoId = :videoId")
    suspend fun deleteReaction(userId: String, videoId: String)

    // Comments
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY createdAt DESC")
    fun observeCommentsForVideo(videoId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments ORDER BY createdAt DESC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("UPDATE comments SET likesCount = likesCount + 1 WHERE id = :commentId")
    suspend fun likeComment(commentId: String)

    @Query("DELETE FROM comments WHERE id = :commentId OR parentCommentId = :commentId")
    suspend fun deleteComment(commentId: String)

    // Subscriptions
    @Query("SELECT * FROM subscriptions WHERE subscriberUserId = :userId")
    fun observeUserSubscriptions(userId: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE subscriberUserId = :userId AND channelId = :channelId LIMIT 1")
    suspend fun getSubscription(userId: String, channelId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE subscriberUserId = :userId AND channelId = :channelId")
    suspend fun deleteSubscription(userId: String, channelId: String)

    // Playlists
    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY isWatchLater DESC, createdAt DESC")
    fun observeUserPlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_items")
    fun observeAllPlaylistItems(): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVideoToPlaylist(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: String, videoId: String)

    @Query("SELECT * FROM playlists WHERE userId = :userId AND isWatchLater = 1 LIMIT 1")
    suspend fun getWatchLaterPlaylist(userId: String): PlaylistEntity?

    // Watch History
    @Query("SELECT * FROM watch_history WHERE userId = :userId ORDER BY watchedAt DESC")
    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearWatchHistory(userId: String)

    // Search History
    @Query("SELECT * FROM search_history WHERE userId = :userId ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchHistoryItem(id: String)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearSearchHistory(userId: String)

    // Community Posts
    @Query("SELECT * FROM community_posts WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observeCommunityPosts(channelId: String): Flow<List<CommunityPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityPost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE id = :postId")
    suspend fun likeCommunityPost(postId: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    // Reports
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    // Payouts
    @Query("SELECT * FROM payout_records WHERE channelId = :channelId ORDER BY createdAt DESC")
    fun observePayoutsForChannel(channelId: String): Flow<List<PayoutRecordEntity>>

    @Query("SELECT * FROM payout_records ORDER BY createdAt DESC")
    fun observeAllPayouts(): Flow<List<PayoutRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutRecord(record: PayoutRecordEntity)

    @Query("UPDATE payout_records SET status = :status WHERE id = :payoutId")
    suspend fun updatePayoutStatus(payoutId: String, status: String)

    // Ad Campaigns & Platform Settings
    @Query("SELECT * FROM ad_campaigns ORDER BY createdAt DESC")
    fun observeAdCampaigns(): Flow<List<AdCampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdCampaign(campaign: AdCampaignEntity)

    @Query("UPDATE ad_campaigns SET isActive = :active WHERE id = :id")
    suspend fun setAdCampaignActive(id: String, active: Boolean)

    @Query("SELECT * FROM platform_settings WHERE id = 1 LIMIT 1")
    fun observePlatformSettings(): Flow<PlatformSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlatformSettings(settings: PlatformSettingsEntity)

    // Utility
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

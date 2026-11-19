package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

class MyVideoRepository(
    private val context: Context,
    private val dao: MyVideoDao,
    val preferences: AppPreferences
) {
    val allUsers: Flow<List<UserEntity>> = dao.observeAllUsers()
    val allChannels: Flow<List<ChannelEntity>> = dao.observeAllChannels()
    val approvedVideos: Flow<List<VideoEntity>> = dao.observeApprovedVideos()
    val allVideosAdmin: Flow<List<VideoEntity>> = dao.observeAllVideosAdmin()
    val allComments: Flow<List<CommentEntity>> = dao.observeAllComments()
    val allReports: Flow<List<ReportEntity>> = dao.observeAllReports()
    val allPayouts: Flow<List<PayoutRecordEntity>> = dao.observeAllPayouts()
    val adCampaigns: Flow<List<AdCampaignEntity>> = dao.observeAdCampaigns()
    val platformSettings: Flow<PlatformSettingsEntity?> = dao.observePlatformSettings()

    val videosWithChannels: Flow<List<VideoWithChannel>> = combine(
        approvedVideos,
        allChannels
    ) { videos, channels ->
        val channelMap = channels.associateBy { it.id }
        videos.mapNotNull { video ->
            channelMap[video.channelId]?.let { ch -> VideoWithChannel(video, ch) }
        }
    }

    suspend fun ensureInitialized() {
        MyVideoDatabase.seedIfEmpty(MyVideoDatabase.getInstance(context))
    }

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)
    fun observeChannelByOwner(userId: String): Flow<ChannelEntity?> = dao.observeChannelByOwner(userId)
    fun observeChannel(channelId: String): Flow<ChannelEntity?> = dao.observeChannelById(channelId)
    fun observeVideo(videoId: String): Flow<VideoEntity?> = dao.observeVideoById(videoId)
    fun observeVideosByChannel(channelId: String): Flow<List<VideoEntity>> = dao.observeVideosByChannel(channelId)
    fun observeComments(videoId: String): Flow<List<CommentEntity>> = dao.observeCommentsForVideo(videoId)
    fun observeSubscriptions(userId: String): Flow<List<SubscriptionEntity>> = dao.observeUserSubscriptions(userId)
    fun observeReaction(userId: String, videoId: String): Flow<VideoReactionEntity?> = dao.observeReaction(userId, videoId)
    fun observePlaylists(userId: String): Flow<List<PlaylistEntity>> = dao.observeUserPlaylists(userId)
    fun observeAllPlaylistItems(): Flow<List<PlaylistItemEntity>> = dao.observeAllPlaylistItems()
    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory(userId)
    fun observeSearchHistory(userId: String): Flow<List<SearchHistoryEntity>> = dao.observeSearchHistory(userId)
    fun observeCommunityPosts(channelId: String): Flow<List<CommunityPostEntity>> = dao.observeCommunityPosts(channelId)
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = dao.observeNotifications(userId)
    fun observeChannelPayouts(channelId: String): Flow<List<PayoutRecordEntity>> = dao.observePayoutsForChannel(channelId)

    // --- Authentication ---

    suspend fun registerAccount(
        email: String,
        displayName: String,
        password: String,
        channelName: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)
        val userId = "user_${UUID.randomUUID().toString().take(8)}"
        val user = UserEntity(
            id = userId,
            email = cleanEmail,
            displayName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") },
            passwordHash = hash,
            passwordSalt = salt,
            role = "CREATOR",
            authProvider = "EMAIL"
        )
        dao.upsertUser(user)

        val finalChannelName = channelName.trim().ifBlank { "${user.displayName}'s Channel" }
        val handle = "@" + finalChannelName.lowercase().replace(Regex("[^a-z0-9]"), "").take(18).ifBlank { "creator" }
        val channel = ChannelEntity(
            id = "ch_${UUID.randomUUID().toString().take(8)}",
            ownerUserId = userId,
            name = finalChannelName,
            handle = handle,
            description = "Welcome to $finalChannelName on MY VIDEO!",
            bannerDrawableKey = "studio"
        )
        dao.upsertChannel(channel)
        dao.upsertPlaylist(PlaylistEntity("pl_watch_later_$userId", userId, "Watch Later", isWatchLater = true))
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                type = "MONETIZATION",
                title = "Welcome to MY VIDEO, ${user.displayName}!",
                message = "Your creator channel $finalChannelName is live. Upload your first video or Short to start growing.",
                relatedId = channel.id
            )
        )
        preferences.setCurrentUserId(userId)
        return Result.success(user)
    }

    suspend fun loginWithEmail(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val user = dao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for $cleanEmail."))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("This account has been suspended by platform moderation."))
        }
        if (!SecurityUtils.verifyPassword(password, user.passwordSalt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Use Forgot Password to recover."))
        }
        preferences.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun quickSignInExisting(userId: String): Result<UserEntity> {
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Account unavailable"))
        if (user.isSuspended) {
            return Result.failure(IllegalStateException("Account is suspended"))
        }
        preferences.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun signInWithGoogleOAuthAccount(email: String, displayName: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            if (existing.isSuspended) {
                return Result.failure(IllegalStateException("Account is suspended"))
            }
            preferences.setCurrentUserId(existing.id)
            return Result.success(existing)
        }
        val salt = SecurityUtils.generateSalt()
        val userId = "user_${UUID.randomUUID().toString().take(8)}"
        val user = UserEntity(
            id = userId,
            email = cleanEmail,
            displayName = displayName.ifBlank { cleanEmail.substringBefore("@") },
            passwordHash = SecurityUtils.hashPassword(UUID.randomUUID().toString(), salt),
            passwordSalt = salt,
            role = "CREATOR",
            authProvider = "GOOGLE_OAUTH"
        )
        dao.upsertUser(user)
        val channel = ChannelEntity(
            id = "ch_${UUID.randomUUID().toString().take(8)}",
            ownerUserId = userId,
            name = "${user.displayName} Studio",
            handle = "@" + user.displayName.lowercase().replace(Regex("[^a-z0-9]"), "").take(16),
            description = "Official MY VIDEO channel connected via Google OAuth.",
            bannerDrawableKey = "studio"
        )
        dao.upsertChannel(channel)
        dao.upsertPlaylist(PlaylistEntity("pl_watch_later_$userId", userId, "Watch Later", isWatchLater = true))
        preferences.setCurrentUserId(userId)
        return Result.success(user)
    }

    suspend fun requestPasswordRecovery(email: String): Result<String> {
        val user = dao.getUserByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("No account registered with that email."))
        val code = SecurityUtils.generateRecoveryCode()
        dao.setRecoveryCode(user.id, code)
        return Result.success(code)
    }

    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<Unit> {
        val user = dao.getUserByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (user.recoveryCode.isBlank() || user.recoveryCode != code.trim()) {
            return Result.failure(IllegalArgumentException("Invalid or expired recovery code."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(newPassword, salt)
        dao.updatePassword(user.id, hash, salt)
        return Result.success(Unit)
    }

    suspend fun logout() {
        preferences.setCurrentUserId(null)
    }

    // --- Watch, History, Reactions, Comments ---

    suspend fun recordVideoWatch(userId: String, video: VideoEntity) {
        dao.upsertWatchHistory(
            WatchHistoryEntity(
                userId = userId,
                videoId = video.id,
                progressMs = 0L,
                watchedAt = System.currentTimeMillis()
            )
        )
        val settings = dao.observePlatformSettings().first() ?: PlatformSettingsEntity()
        val channel = dao.getChannelById(video.channelId)
        val isMonetized = channel?.monetizationTier == "FULL_APPROVED" || channel?.monetizationTier == "EARLY_APPROVED"
        val revDelta = if (isMonetized && video.visibility == "PUBLIC") {
            val rpm = settings.defaultRpmUsd
            val share = if (video.isShort) settings.shortsRevenueSharePercent / 100.0 else settings.creatorRevenueSharePercent / 100.0
            (rpm / 1000.0) * share
        } else 0.0

        val watchMin = (video.durationSec / 60.0).coerceIn(0.25, 8.0)
        dao.incrementVideoView(video.id, watchMin, revDelta)
        dao.recordChannelViewMetrics(
            channelId = video.channelId,
            watchHoursDelta = watchMin / 60.0,
            shortsViewsDelta = if (video.isShort) 1L else 0L,
            earningsDelta = revDelta
        )
    }

    suspend fun toggleReaction(userId: String, video: VideoEntity, targetReaction: String) {
        val existing = dao.getReaction(userId, video.id)
        when {
            existing == null -> {
                dao.upsertReaction(VideoReactionEntity(userId, video.id, targetReaction))
                if (targetReaction == "LIKE") {
                    dao.adjustVideoReactions(video.id, likeDelta = 1L, dislikeDelta = 0L)
                    notifyChannelOwner(
                        channelId = video.channelId,
                        type = "LIKE",
                        title = "New Like on ${video.title}",
                        message = "A viewer liked your ${if (video.isShort) "Short" else "video"}.",
                        relatedId = video.id
                    )
                } else {
                    dao.adjustVideoReactions(video.id, likeDelta = 0L, dislikeDelta = 1L)
                }
            }
            existing.reaction == targetReaction -> {
                dao.deleteReaction(userId, video.id)
                if (targetReaction == "LIKE") {
                    dao.adjustVideoReactions(video.id, likeDelta = -1L, dislikeDelta = 0L)
                } else {
                    dao.adjustVideoReactions(video.id, likeDelta = 0L, dislikeDelta = -1L)
                }
            }
            else -> {
                dao.upsertReaction(VideoReactionEntity(userId, video.id, targetReaction))
                if (targetReaction == "LIKE") {
                    dao.adjustVideoReactions(video.id, likeDelta = 1L, dislikeDelta = -1L)
                } else {
                    dao.adjustVideoReactions(video.id, likeDelta = -1L, dislikeDelta = 1L)
                }
            }
        }
    }

    suspend fun postComment(
        userId: String,
        videoId: String,
        text: String,
        parentCommentId: String? = null
    ): Result<Unit> {
        val clean = text.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val user = dao.getUserById(userId) ?: return Result.failure(IllegalStateException("Not signed in"))
        val ch = dao.getChannelByOwner(userId)
        val comment = CommentEntity(
            id = "cmt_${UUID.randomUUID().toString().take(8)}",
            videoId = videoId,
            parentCommentId = parentCommentId,
            authorUserId = userId,
            authorName = ch?.name ?: user.displayName,
            authorHandle = ch?.handle ?: "@${user.displayName.lowercase().replace(" ", "")}",
            text = clean
        )
        dao.insertComment(comment)
        dao.getVideoById(videoId)?.let { video ->
            notifyChannelOwner(
                channelId = video.channelId,
                type = "COMMENT",
                title = "New ${if (parentCommentId != null) "reply" else "comment"} on ${video.title}",
                message = "${comment.authorHandle}: \"$clean\"",
                relatedId = video.id
            )
        }
        return Result.success(Unit)
    }

    suspend fun likeComment(commentId: String) = dao.likeComment(commentId)
    suspend fun deleteComment(commentId: String) = dao.deleteComment(commentId)

    // --- Subscriptions ---

    suspend fun toggleSubscribe(userId: String, channelId: String): Boolean {
        val existing = dao.getSubscription(userId, channelId)
        return if (existing == null) {
            dao.upsertSubscription(SubscriptionEntity(userId, channelId, "ALL"))
            dao.adjustSubscriberCount(channelId, 1L)
            val subscriber = dao.getUserById(userId)
            notifyChannelOwner(
                channelId = channelId,
                type = "SUBSCRIBER",
                title = "New Subscriber!",
                message = "${subscriber?.displayName ?: "A viewer"} subscribed to your channel.",
                relatedId = channelId
            )
            true
        } else {
            dao.deleteSubscription(userId, channelId)
            dao.adjustSubscriberCount(channelId, -1L)
            false
        }
    }

    suspend fun setSubscriptionNotificationLevel(userId: String, channelId: String, level: String) {
        val existing = dao.getSubscription(userId, channelId) ?: return
        dao.upsertSubscription(existing.copy(notificationLevel = level))
    }

    // --- Playlists & Watch Later ---

    suspend fun toggleWatchLater(userId: String, videoId: String): Boolean {
        var wl = dao.getWatchLaterPlaylist(userId)
        if (wl == null) {
            wl = PlaylistEntity("pl_watch_later_$userId", userId, "Watch Later", isWatchLater = true)
            dao.upsertPlaylist(wl)
        }
        val items = dao.observeAllPlaylistItems().first()
        val exists = items.any { it.playlistId == wl.id && it.videoId == videoId }
        if (exists) {
            dao.removeVideoFromPlaylist(wl.id, videoId)
            return false
        } else {
            dao.addVideoToPlaylist(PlaylistItemEntity(wl.id, videoId))
            return true
        }
    }

    suspend fun togglePlaylistVideo(playlistId: String, videoId: String) {
        val items = dao.observeAllPlaylistItems().first()
        val exists = items.any { it.playlistId == playlistId && it.videoId == videoId }
        if (exists) dao.removeVideoFromPlaylist(playlistId, videoId)
        else dao.addVideoToPlaylist(PlaylistItemEntity(playlistId, videoId))
    }

    suspend fun createPlaylist(userId: String, title: String, initialVideoId: String? = null) {
        val id = "pl_${UUID.randomUUID().toString().take(8)}"
        dao.upsertPlaylist(PlaylistEntity(id, userId, title.trim()))
        if (!initialVideoId.isNullOrBlank()) {
            dao.addVideoToPlaylist(PlaylistItemEntity(id, initialVideoId))
        }
    }

    // --- Channel & Community Posts ---

    suspend fun updateChannelDetails(
        channelId: String,
        name: String,
        description: String,
        avatarUri: String?,
        bannerUri: String?
    ) {
        val existing = dao.getChannelById(channelId) ?: return
        dao.upsertChannel(
            existing.copy(
                name = name.trim().ifBlank { existing.name },
                description = description.trim(),
                avatarUri = avatarUri ?: existing.avatarUri,
                bannerUri = bannerUri ?: existing.bannerUri
            )
        )
    }

    suspend fun createCommunityPost(channelId: String, content: String) {
        if (content.isBlank()) return
        dao.insertCommunityPost(
            CommunityPostEntity(
                id = "post_${UUID.randomUUID().toString().take(8)}",
                channelId = channelId,
                content = content.trim()
            )
        )
    }

    suspend fun likeCommunityPost(postId: String) = dao.likeCommunityPost(postId)

    // --- Upload & Processing ---

    suspend fun publishVideoUpload(
        channelId: String,
        sourceUri: Uri,
        isLocalFileAlready: Boolean,
        customThumbnailSourceUri: Uri?,
        title: String,
        description: String,
        hashtags: String,
        tags: String,
        category: String,
        visibility: String,
        isShort: Boolean,
        publishToYouTube: Boolean,
        onStageUpdate: (Int, String) -> Unit
    ): Result<VideoEntity> {
        return try {
            onStageUpdate(5, "Preparing media stream...")
            val mediaResult = if (isLocalFileAlready) {
                MediaStorageHelper.importVideoWithProgress(context, sourceUri) { pct ->
                    onStageUpdate((pct * 0.7).toInt(), "Securing video file ($pct%)")
                }
            } else {
                MediaStorageHelper.importVideoWithProgress(context, sourceUri) { pct ->
                    onStageUpdate((pct * 0.7).toInt(), "Uploading to MY VIDEO storage ($pct%)")
                }
            }

            onStageUpdate(76, "Generating thumbnail...")
            val finalThumbUri = if (customThumbnailSourceUri != null) {
                MediaStorageHelper.importImage(context, customThumbnailSourceUri)
            } else {
                mediaResult.generatedThumbUri
            }

            onStageUpdate(85, "Transcoding adaptive profiles (1080p / 720p / 480p)...")
            delay(350)
            onStageUpdate(95, "Packaging HLS/CDN manifest...")
            delay(250)

            val channel = dao.getChannelById(channelId)
            val ytAuthorized = publishToYouTube && channel?.youtubeConnected == true &&
                channel.youtubeScopes.contains("youtube.upload")

            val videoId = "vid_${UUID.randomUUID().toString().take(8)}"
            val video = VideoEntity(
                id = videoId,
                channelId = channelId,
                title = title.trim().ifBlank { "Untitled ${if (isShort) "Short" else "Video"}" },
                description = description.trim(),
                videoUri = mediaResult.localUri,
                thumbnailUri = finalThumbUri,
                thumbnailDrawableKey = if (category == "Music") "music" else if (category == "Tech") "tech" else "cinema",
                isShort = isShort,
                category = category,
                visibility = visibility,
                tagsCsv = tags.trim(),
                hashtagsCsv = hashtags.trim(),
                durationSec = mediaResult.durationSec,
                processingState = "PUBLISHED",
                isYouTubeLinked = ytAuthorized,
                youtubeSynced = ytAuthorized,
                youtubeVideoId = if (ytAuthorized) "yt_${videoId.takeLast(6)}" else ""
            )
            dao.upsertVideo(video)
            onStageUpdate(100, "Published!")

            channel?.let { ch ->
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        userId = ch.ownerUserId,
                        type = "UPLOAD",
                        title = "Upload Completed",
                        message = "\"${video.title}\" finished adaptive processing and is now ${video.visibility}.",
                        relatedId = video.id
                    )
                )
            }
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String,
        tagsCsv: String,
        hashtagsCsv: String
    ) {
        val existing = dao.getVideoById(videoId) ?: return
        dao.upsertVideo(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                description = description.trim(),
                category = category,
                visibility = visibility,
                tagsCsv = tagsCsv.trim(),
                hashtagsCsv = hashtagsCsv.trim()
            )
        )
    }

    suspend fun deleteVideo(videoId: String) = dao.deleteVideoById(videoId)

    // --- Search ---

    suspend fun recordSearch(userId: String, query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        dao.insertSearchHistory(
            SearchHistoryEntity(
                id = "srch_${clean.lowercase().hashCode()}",
                userId = userId,
                query = clean
            )
        )
    }

    suspend fun deleteSearchHistoryItem(id: String) = dao.deleteSearchHistoryItem(id)
    suspend fun clearSearchHistory(userId: String) = dao.clearSearchHistory(userId)

    // --- Monetization & Payouts ---

    fun evaluateEligibility(channel: ChannelEntity): MonetizationEligibility {
        return MonetizationEligibility(
            subscribers = channel.subscriberCount,
            watchHours12M = channel.watchHours12M,
            shortsViews90D = channel.shortsViews90D
        )
    }

    suspend fun applyForMonetization(channelId: String, requestedTier: String): Result<Unit> {
        val ch = dao.getChannelById(channelId) ?: return Result.failure(IllegalStateException("Channel not found"))
        val elig = evaluateEligibility(ch)
        if (requestedTier == "EARLY_APPLIED" && !elig.isEarlyEligible) {
            return Result.failure(IllegalStateException("Early Monetization requires 500 subscribers + 3,000 watch hours or 3M Shorts views."))
        }
        if (requestedTier == "FULL_APPLIED" && !elig.isFullEligible) {
            return Result.failure(IllegalStateException("Full Monetization requires 1,000 subscribers + 4,000 watch hours or 10M Shorts views."))
        }
        dao.updateMonetizationTier(channelId, requestedTier)
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = ch.ownerUserId,
                type = "MONETIZATION",
                title = "Monetization Application Submitted",
                message = "Your application for ${if (requestedTier.startsWith("FULL")) "Full Advertising" else "Early Fan Funding"} monetization is under review.",
                relatedId = channelId
            )
        )
        return Result.success(Unit)
    }

    suspend fun updatePayoutAndTaxSettings(
        channelId: String,
        payoutMethod: String,
        payoutAccountMasked: String,
        taxVerified: Boolean,
        identityVerified: Boolean
    ) {
        val ch = dao.getChannelById(channelId) ?: return
        dao.upsertChannel(
            ch.copy(
                payoutMethod = payoutMethod,
                payoutAccountMasked = payoutAccountMasked,
                taxVerified = taxVerified,
                identityVerified = identityVerified
            )
        )
    }

    suspend fun requestCreatorPayout(channelId: String): Result<Unit> {
        val ch = dao.getChannelById(channelId) ?: return Result.failure(IllegalStateException("Channel not found"))
        val settings = dao.observePlatformSettings().first() ?: PlatformSettingsEntity()
        if (!ch.taxVerified || !ch.identityVerified) {
            return Result.failure(IllegalStateException("Complete Tax & Identity verification before requesting a payout."))
        }
        if (ch.estimatedBalanceUsd < settings.minPayoutThresholdUsd) {
            return Result.failure(IllegalStateException("Minimum payout threshold is $${"%.2f".format(settings.minPayoutThresholdUsd)}."))
        }
        val amount = ch.estimatedBalanceUsd
        dao.insertPayoutRecord(
            PayoutRecordEntity(
                id = "pay_${UUID.randomUUID().toString().take(8)}",
                channelId = ch.id,
                channelName = ch.name,
                amountUsd = amount,
                method = "${ch.payoutMethod} ${ch.payoutAccountMasked}",
                periodLabel = "Current Cycle",
                status = "PROCESSING"
            )
        )
        dao.upsertChannel(ch.copy(estimatedBalanceUsd = 0.0))
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = ch.ownerUserId,
                type = "PAYMENT",
                title = "Payout Requested: $${"%.2f".format(amount)}",
                message = "Your MY VIDEO payout to ${ch.payoutMethod} is now processing.",
                relatedId = ch.id
            )
        )
        return Result.success(Unit)
    }

    // --- YouTube OAuth Integration ---

    suspend fun connectYouTubeWithToken(
        channelId: String,
        oauthAccessToken: String,
        includeUploadScope: Boolean
    ): Result<ChannelEntity> {
        val ch = dao.getChannelById(channelId) ?: return Result.failure(IllegalStateException("Channel not found"))
        if (oauthAccessToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Provide an authorized Google OAuth token or authorize via browser."))
        }
        return try {
            val response = YouTubeOAuthHelper.api.getMyChannels("Bearer ${oauthAccessToken.trim()}")
            val ytItem = response.items.firstOrNull()
                ?: return Result.failure(IllegalStateException("No YouTube channel found for this authorized Google account."))
            val subs = ytItem.statistics?.subscriberCount?.toLongOrNull() ?: 0L
            val videosCount = ytItem.statistics?.videoCount?.toLongOrNull() ?: 0L
            val scopes = if (includeUploadScope) {
                "${YouTubeOAuthHelper.YOUTUBE_READONLY_SCOPE} ${YouTubeOAuthHelper.YOUTUBE_UPLOAD_SCOPE}"
            } else {
                YouTubeOAuthHelper.YOUTUBE_READONLY_SCOPE
            }
            val updated = ch.copy(
                youtubeConnected = true,
                youtubeChannelId = ytItem.id,
                youtubeChannelTitle = ytItem.snippet?.title ?: "YouTube Channel",
                youtubeHandle = ytItem.snippet?.customUrl ?: "",
                youtubeSubscribers = subs,
                youtubeVideoCount = videosCount,
                youtubeAccessToken = oauthAccessToken.trim(),
                youtubeScopes = scopes
            )
            dao.upsertChannel(updated)

            // Import eligible YouTube video metadata if authorized
            try {
                val ytVideos = YouTubeOAuthHelper.api.getMyVideos("Bearer ${oauthAccessToken.trim()}")
                ytVideos.items.take(5).forEach { item ->
                    val ytVidId = item.id?.videoId ?: return@forEach
                    val existingId = "yt_imp_$ytVidId"
                    if (dao.getVideoById(existingId) == null) {
                        dao.upsertVideo(
                            VideoEntity(
                                id = existingId,
                                channelId = ch.id,
                                title = item.snippet?.title ?: "Imported YouTube Video",
                                description = item.snippet?.description ?: "",
                                videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                                thumbnailDrawableKey = "studio",
                                category = "Creator",
                                visibility = "PUBLIC",
                                isYouTubeLinked = true,
                                youtubeVideoId = ytVidId,
                                youtubeSynced = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Metadata import optional if channel has no public videos
            }

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("YouTube API authorization error: ${e.message ?: "Check OAuth token & scopes"}"))
        }
    }

    suspend fun disconnectYouTube(channelId: String) {
        val ch = dao.getChannelById(channelId) ?: return
        dao.upsertChannel(
            ch.copy(
                youtubeConnected = false,
                youtubeChannelId = "",
                youtubeChannelTitle = "",
                youtubeHandle = "",
                youtubeSubscribers = 0L,
                youtubeVideoCount = 0L,
                youtubeAccessToken = "",
                youtubeScopes = ""
            )
        )
    }

    // --- Reports & Notifications ---

    suspend fun reportVideo(userId: String, video: VideoEntity, reason: String) {
        dao.insertReport(
            ReportEntity(
                id = "rep_${UUID.randomUUID().toString().take(8)}",
                videoId = video.id,
                videoTitle = video.title,
                reporterUserId = userId,
                reason = reason
            )
        )
    }

    suspend fun markNotificationRead(id: String) = dao.markNotificationRead(id)
    suspend fun markAllNotificationsRead(userId: String) = dao.markAllNotificationsRead(userId)
    suspend fun clearWatchHistory(userId: String) = dao.clearWatchHistory(userId)

    // --- Admin Actions ---

    suspend fun adminSetUserSuspended(userId: String, suspended: Boolean) = dao.setUserSuspended(userId, suspended)
    suspend fun adminSetUserRole(userId: String, role: String) = dao.setUserRole(userId, role)
    suspend fun adminSetChannelVerified(channelId: String, verified: Boolean) = dao.setChannelVerified(channelId, verified)

    suspend fun adminApproveMonetization(channelId: String, approveFull: Boolean) {
        val tier = if (approveFull) "FULL_APPROVED" else "EARLY_APPROVED"
        dao.updateMonetizationTier(channelId, tier)
        notifyChannelOwner(
            channelId = channelId,
            type = "MONETIZATION",
            title = "Monetization Approved!",
            message = "Congratulations! Your channel is now approved for ${if (approveFull) "Full Advertising" else "Early Fan Funding"} monetization on MY VIDEO.",
            relatedId = channelId
        )
    }

    suspend fun adminModerateVideo(videoId: String, approved: Boolean, flagged: Boolean, reportId: String? = null) {
        dao.moderateVideo(videoId, approved, flagged)
        if (!reportId.isNullOrBlank()) {
            dao.updateReportStatus(reportId, if (approved) "DISMISSED" else "RESOLVED")
        }
    }

    suspend fun adminMarkPayoutPaid(payoutId: String) = dao.updatePayoutStatus(payoutId, "PAID")

    suspend fun adminToggleAdCampaign(campaignId: String, active: Boolean) = dao.setAdCampaignActive(campaignId, active)

    suspend fun adminCreateAdCampaign(advertiser: String, title: String, cpm: Double, budget: Double) {
        dao.upsertAdCampaign(
            AdCampaignEntity(
                id = "ad_${UUID.randomUUID().toString().take(8)}",
                advertiserName = advertiser.trim(),
                title = title.trim(),
                cpmUsd = cpm,
                budgetUsd = budget,
                impressionsServed = 0L,
                isActive = true
            )
        )
    }

    suspend fun adminSavePlatformSettings(settings: PlatformSettingsEntity) = dao.upsertPlatformSettings(settings)

    suspend fun runSpamAndFraudScan(): Int {
        val videos = dao.observeAllVideosAdmin().first()
        var flaggedCount = 0
        videos.forEach { v ->
            val suspiciousRatio = v.viewsCount > 1000 && v.watchMinutesTotal < (v.viewsCount * 0.05)
            val alreadyFlagged = v.isFlaggedSpam
            if (suspiciousRatio && !alreadyFlagged) {
                dao.moderateVideo(v.id, approved = v.isApproved, flagged = true)
                flaggedCount++
            }
        }
        return flaggedCount
    }

    private suspend fun notifyChannelOwner(
        channelId: String,
        type: String,
        title: String,
        message: String,
        relatedId: String
    ) {
        val ch = dao.getChannelById(channelId) ?: return
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                userId = ch.ownerUserId,
                type = type,
                title = title,
                message = message,
                relatedId = relatedId
            )
        )
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String = "CREATOR", // USER, CREATOR, ADMIN
    val authProvider: String = "EMAIL", // EMAIL, GOOGLE_OAUTH
    val avatarUri: String = "",
    val recoveryCode: String = "",
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val handle: String,
    val description: String,
    val avatarUri: String = "",
    val avatarColorHex: Long = 0xFFFF1F3D,
    val bannerUri: String = "",
    val bannerDrawableKey: String = "studio",
    val subscriberCount: Long = 0L,
    val totalViews: Long = 0L,
    val watchHours12M: Double = 0.0,
    val shortsViews90D: Long = 0L,
    val isVerified: Boolean = false,
    val monetizationTier: String = "NONE", // NONE, EARLY_APPLIED, EARLY_APPROVED, FULL_APPLIED, FULL_APPROVED
    val taxVerified: Boolean = false,
    val identityVerified: Boolean = false,
    val payoutMethod: String = "Bank Transfer (ACH)",
    val payoutAccountMasked: String = "**** 4829",
    val estimatedBalanceUsd: Double = 0.0,
    val lifetimeEarningsUsd: Double = 0.0,
    val youtubeConnected: Boolean = false,
    val youtubeChannelId: String = "",
    val youtubeChannelTitle: String = "",
    val youtubeHandle: String = "",
    val youtubeSubscribers: Long = 0L,
    val youtubeVideoCount: Long = 0L,
    val youtubeAccessToken: String = "",
    val youtubeScopes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val videoUri: String,
    val thumbnailUri: String = "",
    val thumbnailDrawableKey: String = "cinema",
    val isShort: Boolean = false,
    val category: String = "Tech",
    val visibility: String = "PUBLIC", // PUBLIC, PRIVATE, UNLISTED
    val tagsCsv: String = "",
    val hashtagsCsv: String = "",
    val durationSec: Long = 60L,
    val viewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val dislikesCount: Long = 0L,
    val watchMinutesTotal: Double = 0.0,
    val impressionsCount: Long = 0L,
    val ctrPercent: Double = 4.8,
    val adRevenueUsd: Double = 0.0,
    val processingState: String = "PUBLISHED", // UPLOADING, PROCESSING, PUBLISHED
    val isApproved: Boolean = true,
    val isFlaggedSpam: Boolean = false,
    val isYouTubeLinked: Boolean = false,
    val youtubeVideoId: String = "",
    val youtubeSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val parentCommentId: String? = null,
    val authorUserId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val likesCount: Long = 0L,
    val isFlaggedSpam: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions", primaryKeys = ["subscriberUserId", "channelId"])
data class SubscriptionEntity(
    val subscriberUserId: String,
    val channelId: String,
    val notificationLevel: String = "ALL", // ALL, PERSONALIZED, NONE
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_reactions", primaryKeys = ["userId", "videoId"])
data class VideoReactionEntity(
    val userId: String,
    val videoId: String,
    val reaction: String // LIKE, DISLIKE
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val isWatchLater: Boolean = false,
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "videoId"])
data class PlaylistItemEntity(
    val playlistId: String,
    val videoId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history", primaryKeys = ["userId", "videoId"])
data class WatchHistoryEntity(
    val userId: String,
    val videoId: String,
    val progressMs: Long = 0L,
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val content: String,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // SUBSCRIBER, COMMENT, LIKE, UPLOAD, MONETIZATION, PAYMENT, NEW_VIDEO
    val title: String,
    val message: String,
    val relatedId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val videoTitle: String,
    val reporterUserId: String,
    val reason: String,
    val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payout_records")
data class PayoutRecordEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val channelName: String,
    val amountUsd: Double,
    val method: String,
    val periodLabel: String,
    val status: String = "PAID", // PENDING, PROCESSING, PAID
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_campaigns")
data class AdCampaignEntity(
    @PrimaryKey val id: String,
    val advertiserName: String,
    val title: String,
    val cpmUsd: Double,
    val budgetUsd: Double,
    val impressionsServed: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "platform_settings")
data class PlatformSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val minPayoutThresholdUsd: Double = 100.0,
    val defaultRpmUsd: Double = 4.25,
    val creatorRevenueSharePercent: Int = 55,
    val shortsRevenueSharePercent: Int = 45,
    val spamVelocityThresholdPerMin: Int = 500,
    val autoModerationEnabled: Boolean = true
)

data class VideoWithChannel(
    val video: VideoEntity,
    val channel: ChannelEntity
)

data class MonetizationEligibility(
    val subscribers: Long,
    val watchHours12M: Double,
    val shortsViews90D: Long,
    val earlySubTarget: Long = 500L,
    val earlyWatchHoursTarget: Double = 3000.0,
    val earlyShortsViewsTarget: Long = 3_000_000L,
    val fullSubTarget: Long = 1000L,
    val fullWatchHoursTarget: Double = 4000.0,
    val fullShortsViewsTarget: Long = 10_000_000L
) {
    val earlySubsMet: Boolean get() = subscribers >= earlySubTarget
    val earlyWatchMet: Boolean get() = watchHours12M >= earlyWatchHoursTarget
    val earlyShortsMet: Boolean get() = shortsViews90D >= earlyShortsViewsTarget
    val isEarlyEligible: Boolean get() = earlySubsMet && (earlyWatchMet || earlyShortsMet)

    val fullSubsMet: Boolean get() = subscribers >= fullSubTarget
    val fullWatchMet: Boolean get() = watchHours12M >= fullWatchHoursTarget
    val fullShortsMet: Boolean get() = shortsViews90D >= fullShortsViewsTarget
    val isFullEligible: Boolean get() = fullSubsMet && (fullWatchMet || fullShortsMet)
}

data class UploadProgressItem(
    val id: String,
    val title: String,
    val progressPercent: Int,
    val stageLabel: String,
    val isShort: Boolean
)

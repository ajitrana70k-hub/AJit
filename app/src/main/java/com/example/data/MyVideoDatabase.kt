package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ChannelEntity::class,
        VideoEntity::class,
        CommentEntity::class,
        SubscriptionEntity::class,
        VideoReactionEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        WatchHistoryEntity::class,
        SearchHistoryEntity::class,
        CommunityPostEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        PayoutRecordEntity::class,
        AdCampaignEntity::class,
        PlatformSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MyVideoDatabase : RoomDatabase() {
    abstract fun dao(): MyVideoDao

    companion object {
        @Volatile
        private var INSTANCE: MyVideoDatabase? = null

        fun getInstance(context: Context): MyVideoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MyVideoDatabase::class.java,
                    "my_video_platform.db"
                ).build().also { db ->
                    INSTANCE = db
                }
            }
        }

        suspend fun seedIfEmpty(db: MyVideoDatabase) {
            val dao = db.dao()
            if (dao.getUserCount() > 0) return

            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val day = 86_400_000L

            // Users
            val saltCreator = SecurityUtils.generateSalt()
            val creatorUser = UserEntity(
                id = "user_creator_1",
                email = "creator@myvideo.app",
                displayName = "Aria Vance",
                passwordHash = SecurityUtils.hashPassword("MyVideo#2026", saltCreator),
                passwordSalt = saltCreator,
                role = "CREATOR",
                authProvider = "EMAIL",
                createdAt = now - 120 * day
            )

            val saltAdmin = SecurityUtils.generateSalt()
            val adminUser = UserEntity(
                id = "user_admin_1",
                email = "admin@myvideo.app",
                displayName = "Platform Ops",
                passwordHash = SecurityUtils.hashPassword("Admin#2026", saltAdmin),
                passwordSalt = saltAdmin,
                role = "ADMIN",
                authProvider = "EMAIL",
                createdAt = now - 300 * day
            )

            val saltTech = SecurityUtils.generateSalt()
            val techUser = UserEntity(
                id = "user_creator_2",
                email = "pixel@myvideo.app",
                displayName = "Marcus Chen",
                passwordHash = SecurityUtils.hashPassword("Pixel#2026", saltTech),
                passwordSalt = saltTech,
                role = "CREATOR",
                authProvider = "EMAIL",
                createdAt = now - 90 * day
            )

            val saltMusic = SecurityUtils.generateSalt()
            val musicUser = UserEntity(
                id = "user_creator_3",
                email = "synth@myvideo.app",
                displayName = "Kaelen Nova",
                passwordHash = SecurityUtils.hashPassword("Synth#2026", saltMusic),
                passwordSalt = saltMusic,
                role = "CREATOR",
                authProvider = "EMAIL",
                createdAt = now - 60 * day
            )

            listOf(creatorUser, adminUser, techUser, musicUser).forEach { dao.upsertUser(it) }

            // Channels
            val creatorChannel = ChannelEntity(
                id = "ch_aria_studio",
                ownerUserId = creatorUser.id,
                name = "Aria Vance Films",
                handle = "@ariavance",
                description = "4K aerial cinematography, color grading breakdowns, and studio workflow documentaries.",
                avatarColorHex = 0xFFFF1F3D,
                bannerDrawableKey = "studio",
                subscriberCount = 740L,
                totalViews = 184_500L,
                watchHours12M = 3_420.5,
                shortsViews90D = 1_850_000L,
                isVerified = true,
                monetizationTier = "EARLY_APPROVED",
                taxVerified = true,
                identityVerified = true,
                payoutMethod = "Bank Transfer (ACH)",
                payoutAccountMasked = "**** 4829",
                estimatedBalanceUsd = 148.60,
                lifetimeEarningsUsd = 612.40,
                createdAt = now - 120 * day
            )

            val techChannel = ChannelEntity(
                id = "ch_pixel_lab",
                ownerUserId = techUser.id,
                name = "Pixel Forge Lab",
                handle = "@pixelforgelab",
                description = "Honest desk setups, OLED hardware reviews, and Kotlin/Android engineering deep dives.",
                avatarColorHex = 0xFF3B82F6,
                bannerDrawableKey = "tech",
                subscriberCount = 12_400L,
                totalViews = 1_420_000L,
                watchHours12M = 18_900.0,
                shortsViews90D = 4_200_000L,
                isVerified = true,
                monetizationTier = "FULL_APPROVED",
                taxVerified = true,
                identityVerified = true,
                estimatedBalanceUsd = 842.15,
                lifetimeEarningsUsd = 6_490.00,
                createdAt = now - 90 * day
            )

            val musicChannel = ChannelEntity(
                id = "ch_nova_synth",
                ownerUserId = musicUser.id,
                name = "Kaelen Nova Live",
                handle = "@kaelennova",
                description = "Live analog modular synth performances, sound design tutorials, and midnight sessions.",
                avatarColorHex = 0xFF10B981,
                bannerDrawableKey = "music",
                subscriberCount = 4_820L,
                totalViews = 890_300L,
                watchHours12M = 7_640.0,
                shortsViews90D = 11_200_000L,
                isVerified = true,
                monetizationTier = "FULL_APPROVED",
                taxVerified = true,
                identityVerified = true,
                estimatedBalanceUsd = 395.20,
                lifetimeEarningsUsd = 2_140.80,
                createdAt = now - 60 * day
            )

            val adminChannel = ChannelEntity(
                id = "ch_myvideo_official",
                ownerUserId = adminUser.id,
                name = "MY VIDEO Creators",
                handle = "@myvideoofficial",
                description = "Official platform announcements, creator monetization guides, and spotlight reels.",
                avatarColorHex = 0xFFF59E0B,
                bannerDrawableKey = "cinema",
                subscriberCount = 45_200L,
                totalViews = 3_950_000L,
                watchHours12M = 41_200.0,
                shortsViews90D = 15_400_000L,
                isVerified = true,
                monetizationTier = "FULL_APPROVED",
                taxVerified = true,
                identityVerified = true,
                createdAt = now - 300 * day
            )

            listOf(creatorChannel, techChannel, musicChannel, adminChannel).forEach { dao.upsertChannel(it) }

            // Videos (Long-form + Shorts using reliable HTTPS sample MP4 streams)
            val seededVideos = listOf(
                VideoEntity(
                    id = "vid_1",
                    channelId = creatorChannel.id,
                    title = "Golden Hour Over the Cascades — 4K Drone Cinematography",
                    description = "Shot on dual-native ISO cinema camera above misty mountain ridges at sunrise. Full LUT and exposure breakdown included in the pinned comment.",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    thumbnailDrawableKey = "cinema",
                    isShort = false,
                    category = "Cinema",
                    visibility = "PUBLIC",
                    tagsCsv = "drone,cinematography,4k,filmmaking,color grading",
                    hashtagsCsv = "#cinematography #4kdrone #filmmaking",
                    durationSec = 425L,
                    viewsCount = 84_200L,
                    likesCount = 6_410L,
                    dislikesCount = 42L,
                    watchMinutesTotal = 142_800.0,
                    impressionsCount = 610_000L,
                    ctrPercent = 7.8,
                    adRevenueUsd = 284.50,
                    createdAt = now - 14 * hour
                ),
                VideoEntity(
                    id = "vid_2",
                    channelId = techChannel.id,
                    title = "Building the Ultimate 2026 Minimalist OLED Desk Setup",
                    description = "From custom coiled cables to calibrated 240Hz QD-OLED panels and acoustic studio treatment. Every component linked below.",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    thumbnailDrawableKey = "tech",
                    isShort = false,
                    category = "Tech",
                    visibility = "PUBLIC",
                    tagsCsv = "desk setup,oled,keyboard,workspace,tech review",
                    hashtagsCsv = "#desksetup #tech2026 #oled",
                    durationSec = 695L,
                    viewsCount = 215_900L,
                    likesCount = 18_320L,
                    dislikesCount = 190L,
                    watchMinutesTotal = 510_400.0,
                    impressionsCount = 1_450_000L,
                    ctrPercent = 8.4,
                    adRevenueUsd = 612.90,
                    createdAt = now - 2 * day
                ),
                VideoEntity(
                    id = "vid_3",
                    channelId = musicChannel.id,
                    title = "Midnight Analog Synth Session — Live Hardware Set",
                    description = "100% live hardware improvisation recorded in one take. Sequential Prophet, Eurorack textures, and analog tape delay.",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    thumbnailDrawableKey = "music",
                    isShort = false,
                    category = "Music",
                    visibility = "PUBLIC",
                    tagsCsv = "synth,electronic music,live session,modular,studio",
                    hashtagsCsv = "#analogsynth #liveset #sounddesign",
                    durationSec = 940L,
                    viewsCount = 128_400L,
                    likesCount = 11_250L,
                    dislikesCount = 74L,
                    watchMinutesTotal = 289_000.0,
                    impressionsCount = 820_000L,
                    ctrPercent = 6.9,
                    adRevenueUsd = 342.10,
                    createdAt = now - 4 * day
                ),
                VideoEntity(
                    id = "vid_4",
                    channelId = creatorChannel.id,
                    title = "Inside My Acoustic Studio: Audio Chains & Camera Rigs",
                    description = "Full tour of the MY VIDEO Creator Studio setup — dual-camera framing, shotgun mic placement, and waveform monitoring.",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    thumbnailDrawableKey = "studio",
                    isShort = false,
                    category = "Education",
                    visibility = "PUBLIC",
                    tagsCsv = "studio tour,audio,camera rig,creator,podcast",
                    hashtagsCsv = "#studiotour #creatorgear #audio",
                    durationSec = 540L,
                    viewsCount = 62_100L,
                    likesCount = 4_890L,
                    dislikesCount = 31L,
                    watchMinutesTotal = 95_200.0,
                    impressionsCount = 420_000L,
                    ctrPercent = 6.2,
                    adRevenueUsd = 198.40,
                    createdAt = now - 6 * day
                ),
                VideoEntity(
                    id = "vid_5",
                    channelId = adminChannel.id,
                    title = "MY VIDEO Partner Program: Early vs Full Monetization Explained",
                    description = "Learn how MY VIDEO's two-tier creator monetization works: Early Fan Funding at 500 subscribers and Full Ad Revenue Sharing at 1,000 subscribers.",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    thumbnailDrawableKey = "studio",
                    isShort = false,
                    category = "Education",
                    visibility = "PUBLIC",
                    tagsCsv = "monetization,creator program,earnings,my video,partner",
                    hashtagsCsv = "#monetization #creators #myvideo",
                    durationSec = 312L,
                    viewsCount = 340_500L,
                    likesCount = 24_900L,
                    dislikesCount = 210L,
                    watchMinutesTotal = 720_000.0,
                    impressionsCount = 2_100_000L,
                    ctrPercent = 9.1,
                    adRevenueUsd = 0.0,
                    createdAt = now - 8 * day
                ),
                // Shorts
                VideoEntity(
                    id = "short_1",
                    channelId = creatorChannel.id,
                    title = "1-Second Exposure Trick for Epic Sunrise Flares",
                    description = "Try lowering shutter angle to 180° and catching the ridge line just before dawn! #filmmaking #shorts",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    thumbnailDrawableKey = "cinema",
                    isShort = true,
                    category = "Cinema",
                    visibility = "PUBLIC",
                    tagsCsv = "shorts,camera trick,sunrise,cinema",
                    hashtagsCsv = "#filmmaking #cameratricks #shorts",
                    durationSec = 15L,
                    viewsCount = 490_000L,
                    likesCount = 38_200L,
                    dislikesCount = 180L,
                    watchMinutesTotal = 122_500.0,
                    impressionsCount = 1_900_000L,
                    ctrPercent = 11.4,
                    adRevenueUsd = 76.20,
                    createdAt = now - 5 * hour
                ),
                VideoEntity(
                    id = "short_2",
                    channelId = techChannel.id,
                    title = "Thocky Gasket-Mount Keyboard Sound Test",
                    description = "Lubed linear switches on brass plate — listen with headphones! #keyboard #tech #shorts",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    thumbnailDrawableKey = "tech",
                    isShort = true,
                    category = "Tech",
                    visibility = "PUBLIC",
                    tagsCsv = "keyboard,asmr,tech,shorts",
                    hashtagsCsv = "#mechanicalkeyboard #tech #shorts",
                    durationSec = 28L,
                    viewsCount = 820_000L,
                    likesCount = 67_500L,
                    dislikesCount = 320L,
                    watchMinutesTotal = 246_000.0,
                    impressionsCount = 3_400_000L,
                    ctrPercent = 12.8,
                    adRevenueUsd = 118.40,
                    createdAt = now - 1 * day
                ),
                VideoEntity(
                    id = "short_3",
                    channelId = musicChannel.id,
                    title = "Designing a Cyberpunk Bassline in 30 Seconds",
                    description = "Sawtooth + sub-oscillator + analog chorus = instant neon energy. #synth #music #shorts",
                    videoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
                    thumbnailDrawableKey = "music",
                    isShort = true,
                    category = "Music",
                    visibility = "PUBLIC",
                    tagsCsv = "synth,bassline,music production,shorts",
                    hashtagsCsv = "#synthwave #musicproducer #shorts",
                    durationSec = 32L,
                    viewsCount = 1_240_000L,
                    likesCount = 104_000L,
                    dislikesCount = 410L,
                    watchMinutesTotal = 410_000.0,
                    impressionsCount = 5_100_000L,
                    ctrPercent = 13.6,
                    adRevenueUsd = 164.80,
                    createdAt = now - 3 * day
                )
            )
            seededVideos.forEach { dao.upsertVideo(it) }

            // Subscriptions & Watch Later for creatorUser
            dao.upsertSubscription(SubscriptionEntity(creatorUser.id, techChannel.id, "ALL", now - 20 * day))
            dao.upsertSubscription(SubscriptionEntity(creatorUser.id, musicChannel.id, "PERSONALIZED", now - 15 * day))
            dao.upsertPlaylist(PlaylistEntity("pl_watch_later_${creatorUser.id}", creatorUser.id, "Watch Later", isWatchLater = true))
            dao.upsertPlaylist(PlaylistEntity("pl_fav_${creatorUser.id}", creatorUser.id, "Color Grading Inspo", isWatchLater = false))
            dao.addVideoToPlaylist(PlaylistItemEntity("pl_watch_later_${creatorUser.id}", "vid_2"))
            dao.addVideoToPlaylist(PlaylistItemEntity("pl_fav_${creatorUser.id}", "vid_1"))

            // Comments & Replies
            val c1 = CommentEntity(
                id = "cmt_1",
                videoId = "vid_1",
                authorUserId = techUser.id,
                authorName = "Pixel Forge Lab",
                authorHandle = "@pixelforgelab",
                text = "That dynamic range when the sun breaks over the ridge is unreal. What ND filter strength did you run?",
                likesCount = 142L,
                createdAt = now - 10 * hour
            )
            val c1Reply = CommentEntity(
                id = "cmt_2",
                videoId = "vid_1",
                parentCommentId = "cmt_1",
                authorUserId = creatorUser.id,
                authorName = "Aria Vance Films",
                authorHandle = "@ariavance",
                text = "Appreciate it Marcus! ND16/PL combo at 24fps, 1/50s shutter.",
                likesCount = 68L,
                createdAt = now - 9 * hour
            )
            val c2 = CommentEntity(
                id = "cmt_3",
                videoId = "vid_2",
                authorUserId = musicUser.id,
                authorName = "Kaelen Nova Live",
                authorHandle = "@kaelennova",
                text = "That walnut grain paired with the crimson bias lighting looks straight out of a mastering suite.",
                likesCount = 89L,
                createdAt = now - 1 * day
            )
            listOf(c1, c1Reply, c2).forEach { dao.insertComment(it) }

            // Community Posts
            dao.insertCommunityPost(
                CommunityPostEntity(
                    id = "post_1",
                    channelId = creatorChannel.id,
                    content = "Filming a 3-part alpine night-lapse documentary this weekend! Drop camera or color grading questions below.",
                    likesCount = 312L,
                    commentsCount = 48L,
                    createdAt = now - 2 * day
                )
            )
            dao.insertCommunityPost(
                CommunityPostEntity(
                    id = "post_2",
                    channelId = techChannel.id,
                    content = "Testing 4 flagship creator phones head-to-head for 4K60 HDR video capture. Full benchmark drops Friday.",
                    likesCount = 940L,
                    commentsCount = 112L,
                    createdAt = now - 1 * day
                )
            )

            // Notifications for creatorUser
            listOf(
                NotificationEntity(
                    id = "notif_1",
                    userId = creatorUser.id,
                    type = "SUBSCRIBER",
                    title = "New Milestone: 740 Subscribers!",
                    message = "You gained 28 new subscribers this week. Only 260 away from Full Advertising Monetization!",
                    relatedId = creatorChannel.id,
                    isRead = false,
                    createdAt = now - 1 * hour
                ),
                NotificationEntity(
                    id = "notif_2",
                    userId = creatorUser.id,
                    type = "COMMENT",
                    title = "New comment on Golden Hour Over the Cascades",
                    message = "@pixelforgelab commented: \"That dynamic range when the sun breaks over the ridge is unreal...\"",
                    relatedId = "vid_1",
                    isRead = false,
                    createdAt = now - 3 * hour
                ),
                NotificationEntity(
                    id = "notif_3",
                    userId = creatorUser.id,
                    type = "PAYMENT",
                    title = "MY VIDEO Monthly Earnings Ready",
                    message = "Your estimated balance reached $148.60 — above the $100.00 minimum payout threshold.",
                    relatedId = creatorChannel.id,
                    isRead = false,
                    createdAt = now - 12 * hour
                ),
                NotificationEntity(
                    id = "notif_4",
                    userId = creatorUser.id,
                    type = "NEW_VIDEO",
                    title = "Pixel Forge Lab uploaded a new video",
                    message = "Building the Ultimate 2026 Minimalist OLED Desk Setup",
                    relatedId = "vid_2",
                    isRead = true,
                    createdAt = now - 2 * day
                )
            ).forEach { dao.insertNotification(it) }

            // Payout records
            listOf(
                PayoutRecordEntity(
                    id = "pay_1",
                    channelId = creatorChannel.id,
                    channelName = creatorChannel.name,
                    amountUsd = 224.80,
                    method = "Bank Transfer (ACH) **** 4829",
                    periodLabel = "October 2026",
                    status = "PAID",
                    createdAt = now - 18 * day
                ),
                PayoutRecordEntity(
                    id = "pay_2",
                    channelId = creatorChannel.id,
                    channelName = creatorChannel.name,
                    amountUsd = 239.00,
                    method = "Bank Transfer (ACH) **** 4829",
                    periodLabel = "September 2026",
                    status = "PAID",
                    createdAt = now - 48 * day
                ),
                PayoutRecordEntity(
                    id = "pay_3",
                    channelId = techChannel.id,
                    channelName = techChannel.name,
                    amountUsd = 1420.50,
                    method = "Wire Transfer **** 9014",
                    periodLabel = "October 2026",
                    status = "PENDING",
                    createdAt = now - 2 * day
                )
            ).forEach { dao.insertPayoutRecord(it) }

            // Ad Campaigns & Platform Settings
            listOf(
                AdCampaignEntity(
                    id = "ad_1",
                    advertiserName = "Lumina Optics",
                    title = "Cinema Prime Lenses 2026",
                    cpmUsd = 8.40,
                    budgetUsd = 25_000.0,
                    impressionsServed = 1_420_000L,
                    isActive = true
                ),
                AdCampaignEntity(
                    id = "ad_2",
                    advertiserName = "KeyCraft Studio",
                    title = "Pro Mechanical Keyboards",
                    cpmUsd = 6.20,
                    budgetUsd = 15_000.0,
                    impressionsServed = 890_000L,
                    isActive = true
                )
            ).forEach { dao.upsertAdCampaign(it) }

            dao.upsertPlatformSettings(PlatformSettingsEntity())
        }
    }
}

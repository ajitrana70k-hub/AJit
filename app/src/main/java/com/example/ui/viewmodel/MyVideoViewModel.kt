package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class HomeFeedFilter(val label: String) {
    ALL("All"),
    TRENDING("Trending"),
    LATEST("Latest"),
    POPULAR("Popular"),
    TECH("Tech"),
    CINEMA("Cinema"),
    MUSIC("Music"),
    GAMING("Gaming"),
    EDUCATION("Education")
}

enum class SearchScope(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    CHANNELS("Channels"),
    HASHTAGS("Hashtags"),
    CATEGORIES("Categories")
}

enum class DateFilter(val label: String) {
    ANY("Any time"),
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month")
}

enum class SortFilter(val label: String) {
    RELEVANCE("Relevance"),
    MOST_VIEWED("Most viewed"),
    TOP_RATED("Top liked")
}

enum class DurationFilter(val label: String) {
    ANY("Any length"),
    SHORT("Under 4m"),
    MEDIUM("4 - 20m"),
    LONG("Over 20m")
}

data class SearchUiState(
    val query: String = "",
    val scope: SearchScope = SearchScope.ALL,
    val dateFilter: DateFilter = DateFilter.ANY,
    val sortFilter: SortFilter = SortFilter.RELEVANCE,
    val durationFilter: DurationFilter = DurationFilter.ANY
)

@OptIn(ExperimentalCoroutinesApi::class)
class MyVideoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MyVideoDatabase.getInstance(application)
    val prefs = AppPreferences(application)
    val repo = MyVideoRepository(application, db.dao(), prefs)

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _homeFilter = MutableStateFlow(HomeFeedFilter.ALL)
    val homeFilter: StateFlow<HomeFeedFilter> = _homeFilter.asStateFlow()

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _uploadQueue = MutableStateFlow<List<UploadProgressItem>>(emptyList())
    val uploadQueue: StateFlow<List<UploadProgressItem>> = _uploadQueue.asStateFlow()

    private val _recordedVideoUri = MutableStateFlow<Uri?>(null)
    val recordedVideoUri: StateFlow<Uri?> = _recordedVideoUri.asStateFlow()

    val themeMode: StateFlow<ThemePreference> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.DARK)

    val youtubeClientId: StateFlow<String> = prefs.youtubeOAuthClientId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val currentUserId: StateFlow<String?> = prefs.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: StateFlow<UserEntity?> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else repo.observeUser(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentChannel: StateFlow<ChannelEntity?> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else repo.observeChannelByOwner(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allVideosWithChannels: StateFlow<List<VideoWithChannel>> = repo.videosWithChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<ChannelEntity>> = repo.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repo.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeVideos: StateFlow<List<VideoWithChannel>> = combine(
        allVideosWithChannels,
        homeFilter
    ) { items, filter ->
        val publicLong = items.filter { !it.video.isShort && it.video.visibility == "PUBLIC" }
        when (filter) {
            HomeFeedFilter.ALL -> publicLong
            HomeFeedFilter.TRENDING -> publicLong.sortedByDescending { it.video.viewsCount + it.video.likesCount * 4 }
            HomeFeedFilter.LATEST -> publicLong.sortedByDescending { it.video.createdAt }
            HomeFeedFilter.POPULAR -> publicLong.sortedByDescending { it.video.viewsCount }
            HomeFeedFilter.TECH -> publicLong.filter { it.video.category.equals("Tech", true) }
            HomeFeedFilter.CINEMA -> publicLong.filter { it.video.category.equals("Cinema", true) }
            HomeFeedFilter.MUSIC -> publicLong.filter { it.video.category.equals("Music", true) }
            HomeFeedFilter.GAMING -> publicLong.filter { it.video.category.equals("Gaming", true) }
            HomeFeedFilter.EDUCATION -> publicLong.filter { it.video.category.equals("Education", true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortsFeed: StateFlow<List<VideoWithChannel>> = allVideosWithChannels.map { items ->
        items.filter { it.video.isShort && it.video.visibility == "PUBLIC" }
            .sortedByDescending { it.video.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSubscriptions: StateFlow<List<SubscriptionEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeSubscriptions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscribedVideos: StateFlow<List<VideoWithChannel>> = combine(
        allVideosWithChannels,
        userSubscriptions
    ) { videos, subs ->
        val subIds = subs.map { it.channelId }.toSet()
        videos.filter { it.channel.id in subIds && it.video.visibility == "PUBLIC" }
            .sortedByDescending { it.video.createdAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPlaylists: StateFlow<List<PlaylistEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observePlaylists(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylistItems: StateFlow<List<PlaylistItemEntity>> = repo.observeAllPlaylistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeWatchHistory(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeSearchHistory(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repo.observeNotifications(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentChannelPayouts: StateFlow<List<PayoutRecordEntity>> = currentChannel.flatMapLatest { ch ->
        if (ch == null) flowOf(emptyList()) else repo.observeChannelPayouts(ch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideosAdmin: StateFlow<List<VideoEntity>> = repo.allVideosAdmin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComments: StateFlow<List<CommentEntity>> = repo.allComments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repo.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayouts: StateFlow<List<PayoutRecordEntity>> = repo.allPayouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adCampaigns: StateFlow<List<AdCampaignEntity>> = repo.adCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platformSettings: StateFlow<PlatformSettingsEntity> = repo.platformSettings
        .map { it ?: PlatformSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformSettingsEntity())

    val searchResults: StateFlow<List<VideoWithChannel>> = combine(
        allVideosWithChannels,
        searchUiState
    ) { items, state ->
        val q = state.query.trim().lowercase()
        val now = System.currentTimeMillis()
        val publicOnly = items.filter { it.video.visibility == "PUBLIC" }

        val matched = if (q.isBlank()) {
            publicOnly
        } else {
            publicOnly.filter { item ->
                when (state.scope) {
                    SearchScope.ALL -> item.video.title.lowercase().contains(q) ||
                        item.video.description.lowercase().contains(q) ||
                        item.video.hashtagsCsv.lowercase().contains(q) ||
                        item.video.tagsCsv.lowercase().contains(q) ||
                        item.video.category.lowercase().contains(q) ||
                        item.channel.name.lowercase().contains(q) ||
                        item.channel.handle.lowercase().contains(q)
                    SearchScope.VIDEOS -> item.video.title.lowercase().contains(q) ||
                        item.video.description.lowercase().contains(q)
                    SearchScope.CHANNELS -> item.channel.name.lowercase().contains(q) ||
                        item.channel.handle.lowercase().contains(q)
                    SearchScope.HASHTAGS -> item.video.hashtagsCsv.lowercase().contains(q) ||
                        item.video.tagsCsv.lowercase().contains(q)
                    SearchScope.CATEGORIES -> item.video.category.lowercase().contains(q)
                }
            }
        }

        val dateFiltered = matched.filter { item ->
            when (state.dateFilter) {
                DateFilter.ANY -> true
                DateFilter.TODAY -> now - item.video.createdAt <= 86_400_000L
                DateFilter.THIS_WEEK -> now - item.video.createdAt <= 7 * 86_400_000L
                DateFilter.THIS_MONTH -> now - item.video.createdAt <= 30 * 86_400_000L
            }
        }

        val durationFiltered = dateFiltered.filter { item ->
            when (state.durationFilter) {
                DurationFilter.ANY -> true
                DurationFilter.SHORT -> item.video.durationSec < 240
                DurationFilter.MEDIUM -> item.video.durationSec in 240..1200
                DurationFilter.LONG -> item.video.durationSec > 1200
            }
        }

        when (state.sortFilter) {
            SortFilter.RELEVANCE -> durationFiltered
            SortFilter.MOST_VIEWED -> durationFiltered.sortedByDescending { it.video.viewsCount }
            SortFilter.TOP_RATED -> durationFiltered.sortedByDescending { it.video.likesCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repo.ensureInitialized()
            _isInitializing.value = false
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun setHomeFilter(filter: HomeFeedFilter) {
        _homeFilter.value = filter
    }

    fun setRecordedVideoUri(uri: Uri?) {
        _recordedVideoUri.value = uri
    }

    fun setThemeMode(mode: ThemePreference) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    // --- Search Controls ---
    fun updateSearchQuery(q: String) {
        _searchUiState.update { it.copy(query = q) }
    }

    fun updateSearchScope(scope: SearchScope) {
        _searchUiState.update { it.copy(scope = scope) }
    }

    fun updateDateFilter(dateFilter: DateFilter) {
        _searchUiState.update { it.copy(dateFilter = dateFilter) }
    }

    fun updateSortFilter(sortFilter: SortFilter) {
        _searchUiState.update { it.copy(sortFilter = sortFilter) }
    }

    fun updateDurationFilter(durationFilter: DurationFilter) {
        _searchUiState.update { it.copy(durationFilter = durationFilter) }
    }

    fun commitSearch(query: String) {
        val uid = currentUserId.value ?: return
        _searchUiState.update { it.copy(query = query) }
        viewModelScope.launch { repo.recordSearch(uid, query) }
    }

    fun deleteSearchHistoryItem(id: String) {
        viewModelScope.launch { repo.deleteSearchHistoryItem(id) }
    }

    fun clearSearchHistory() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch { repo.clearSearchHistory(uid) }
    }

    // --- Auth Actions ---
    fun loginWithEmail(email: String, password: String, onResult: (Result<UserEntity>) -> Unit) {
        viewModelScope.launch {
            val res = repo.loginWithEmail(email, password)
            onResult(res)
        }
    }

    fun registerAccount(
        email: String,
        displayName: String,
        password: String,
        channelName: String,
        onResult: (Result<UserEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repo.registerAccount(email, displayName, password, channelName)
            onResult(res)
        }
    }

    fun signInWithGoogleOAuth(email: String, displayName: String, onResult: (Result<UserEntity>) -> Unit) {
        viewModelScope.launch {
            val res = repo.signInWithGoogleOAuthAccount(email, displayName)
            onResult(res)
        }
    }

    fun quickSignInDemo(userId: String) {
        viewModelScope.launch {
            repo.quickSignInExisting(userId).onFailure { showMessage(it.message ?: "Sign-in failed") }
        }
    }

    fun requestRecoveryCode(email: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch { onResult(repo.requestPasswordRecovery(email)) }
    }

    fun resetPasswordWithCode(email: String, code: String, newPass: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(repo.resetPasswordWithCode(email, code, newPass)) }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    // --- Video Interactions ---
    fun observeVideo(videoId: String): Flow<VideoEntity?> = repo.observeVideo(videoId)
    fun observeChannel(channelId: String): Flow<ChannelEntity?> = repo.observeChannel(channelId)
    fun observeComments(videoId: String): Flow<List<CommentEntity>> = repo.observeComments(videoId)
    fun observeCommunityPosts(channelId: String): Flow<List<CommunityPostEntity>> = repo.observeCommunityPosts(channelId)
    fun observeReaction(videoId: String): Flow<VideoReactionEntity?> {
        val uid = currentUserId.value ?: return flowOf(null)
        return repo.observeReaction(uid, videoId)
    }

    fun recordVideoWatch(video: VideoEntity) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch { repo.recordVideoWatch(uid, video) }
    }

    fun toggleReaction(video: VideoEntity, reaction: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch { repo.toggleReaction(uid, video, reaction) }
    }

    fun postComment(videoId: String, text: String, parentCommentId: String? = null) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repo.postComment(uid, videoId, text, parentCommentId)
                .onFailure { showMessage(it.message ?: "Could not post comment") }
        }
    }

    fun likeComment(commentId: String) {
        viewModelScope.launch { repo.likeComment(commentId) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { repo.deleteComment(commentId) }
    }

    fun toggleSubscribe(channelId: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            val nowSub = repo.toggleSubscribe(uid, channelId)
            showMessage(if (nowSub) "Subscribed to channel" else "Unsubscribed from channel")
        }
    }

    fun setNotificationLevel(channelId: String, level: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repo.setSubscriptionNotificationLevel(uid, channelId, level)
            showMessage("Channel notifications set to $level")
        }
    }

    fun toggleWatchLater(videoId: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            val saved = repo.toggleWatchLater(uid, videoId)
            showMessage(if (saved) "Saved to Watch Later" else "Removed from Watch Later")
        }
    }

    fun togglePlaylistVideo(playlistId: String, videoId: String) {
        viewModelScope.launch {
            repo.togglePlaylistVideo(playlistId, videoId)
            showMessage("Updated playlist")
        }
    }

    fun createPlaylist(title: String, initialVideoId: String? = null) {
        val uid = currentUserId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.createPlaylist(uid, title, initialVideoId)
            showMessage("Created playlist \"$title\"")
        }
    }

    fun reportVideo(video: VideoEntity, reason: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repo.reportVideo(uid, video, reason)
            showMessage("Report submitted to moderation team")
        }
    }

    // --- Channel & Upload ---
    fun updateChannelDetails(name: String, description: String, avatarUri: String?, bannerUri: String?) {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.updateChannelDetails(ch.id, name, description, avatarUri, bannerUri)
            showMessage("Channel details updated")
        }
    }

    fun createCommunityPost(content: String) {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.createCommunityPost(ch.id, content)
            showMessage("Community post published")
        }
    }

    fun likeCommunityPost(postId: String) {
        viewModelScope.launch { repo.likeCommunityPost(postId) }
    }

    fun publishVideos(
        sourceUris: List<Uri>,
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
        onComplete: (Boolean) -> Unit
    ) {
        val ch = currentChannel.value
        if (ch == null) {
            showMessage("Create or sign in to a channel first")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            var allSucceeded = true
            sourceUris.forEachIndexed { index, uri ->
                val itemId = "up_${System.currentTimeMillis()}_$index"
                val itemTitle = if (sourceUris.size == 1) title else "$title (${index + 1}/${sourceUris.size})"
                val initial = UploadProgressItem(itemId, itemTitle, 0, "Queued...", isShort)
                _uploadQueue.update { it + initial }

                val result = repo.publishVideoUpload(
                    channelId = ch.id,
                    sourceUri = uri,
                    isLocalFileAlready = isLocalFileAlready,
                    customThumbnailSourceUri = customThumbnailSourceUri,
                    title = itemTitle,
                    description = description,
                    hashtags = hashtags,
                    tags = tags,
                    category = category,
                    visibility = visibility,
                    isShort = isShort,
                    publishToYouTube = publishToYouTube
                ) { pct, stage ->
                    _uploadQueue.update { list ->
                        list.map { if (it.id == itemId) it.copy(progressPercent = pct, stageLabel = stage) else it }
                    }
                }
                if (result.isFailure) {
                    allSucceeded = false
                    showMessage("Upload failed: ${result.exceptionOrNull()?.message}")
                }
            }
            if (allSucceeded) {
                showMessage(if (sourceUris.size > 1) "${sourceUris.size} videos published!" else "Video published to MY VIDEO!")
            }
            _uploadQueue.update { list -> list.filter { it.progressPercent < 100 } }
            onComplete(allSucceeded)
        }
    }

    fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String,
        tagsCsv: String,
        hashtagsCsv: String
    ) {
        viewModelScope.launch {
            repo.updateVideoMetadata(videoId, title, description, category, visibility, tagsCsv, hashtagsCsv)
            showMessage("Video details updated")
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            repo.deleteVideo(videoId)
            showMessage("Video deleted")
        }
    }

    // --- Monetization & Payouts ---
    fun applyForMonetization(requestedTier: String) {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.applyForMonetization(ch.id, requestedTier)
                .onSuccess { showMessage("Monetization application submitted!") }
                .onFailure { showMessage(it.message ?: "Not eligible yet") }
        }
    }

    fun updatePayoutAndTaxSettings(method: String, maskedAccount: String, taxVerified: Boolean, idVerified: Boolean) {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.updatePayoutAndTaxSettings(ch.id, method, maskedAccount, taxVerified, idVerified)
            showMessage("Payment & verification settings saved")
        }
    }

    fun requestPayout() {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.requestCreatorPayout(ch.id)
                .onSuccess { showMessage("Payout request submitted!") }
                .onFailure { showMessage(it.message ?: "Cannot request payout") }
        }
    }

    // --- YouTube OAuth ---
    fun saveYoutubeOAuthClientId(clientId: String) {
        viewModelScope.launch {
            prefs.setYoutubeOAuthClientId(clientId)
            showMessage("Google OAuth Client ID saved")
        }
    }

    fun connectYouTubeWithToken(accessToken: String, includeUploadScope: Boolean) {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.connectYouTubeWithToken(ch.id, accessToken, includeUploadScope)
                .onSuccess { updated ->
                    showMessage("Connected YouTube channel: ${updated.youtubeChannelTitle}")
                }
                .onFailure { err ->
                    showMessage(err.message ?: "YouTube connection failed")
                }
        }
    }

    fun disconnectYouTube() {
        val ch = currentChannel.value ?: return
        viewModelScope.launch {
            repo.disconnectYouTube(ch.id)
            showMessage("YouTube account disconnected")
        }
    }

    // --- Notifications & History ---
    fun markNotificationRead(id: String) {
        viewModelScope.launch { repo.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch { repo.markAllNotificationsRead(uid) }
    }

    fun clearWatchHistory() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repo.clearWatchHistory(uid)
            showMessage("Watch history cleared")
        }
    }

    // --- Admin Actions ---
    fun adminSetUserSuspended(userId: String, suspended: Boolean) {
        viewModelScope.launch { repo.adminSetUserSuspended(userId, suspended) }
    }

    fun adminSetUserRole(userId: String, role: String) {
        viewModelScope.launch { repo.adminSetUserRole(userId, role) }
    }

    fun adminSetChannelVerified(channelId: String, verified: Boolean) {
        viewModelScope.launch { repo.adminSetChannelVerified(channelId, verified) }
    }

    fun adminApproveMonetization(channelId: String, full: Boolean) {
        viewModelScope.launch {
            repo.adminApproveMonetization(channelId, full)
            showMessage("Channel monetization updated")
        }
    }

    fun adminModerateVideo(videoId: String, approved: Boolean, flagged: Boolean, reportId: String? = null) {
        viewModelScope.launch {
            repo.adminModerateVideo(videoId, approved, flagged, reportId)
            showMessage("Moderation decision applied")
        }
    }

    fun adminMarkPayoutPaid(payoutId: String) {
        viewModelScope.launch {
            repo.adminMarkPayoutPaid(payoutId)
            showMessage("Payout marked as PAID")
        }
    }

    fun adminToggleAdCampaign(id: String, active: Boolean) {
        viewModelScope.launch { repo.adminToggleAdCampaign(id, active) }
    }

    fun adminCreateAdCampaign(advertiser: String, title: String, cpm: Double, budget: Double) {
        viewModelScope.launch {
            repo.adminCreateAdCampaign(advertiser, title, cpm, budget)
            showMessage("Ad campaign created")
        }
    }

    fun adminSaveSettings(settings: PlatformSettingsEntity) {
        viewModelScope.launch {
            repo.adminSavePlatformSettings(settings)
            showMessage("Platform settings saved")
        }
    }

    fun runSpamScan() {
        viewModelScope.launch {
            val flagged = repo.runSpamAndFraudScan()
            showMessage("Fraud & spam scan complete: $flagged new anomalies flagged")
        }
    }
}

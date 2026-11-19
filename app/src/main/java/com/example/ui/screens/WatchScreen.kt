package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CommentEntity
import com.example.data.YouTubeOAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    videoId: String,
    viewModel: MyVideoViewModel,
    onBack: () -> Unit,
    onWatchAnother: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenHashtagSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val video by viewModel.observeVideo(videoId).collectAsStateWithLifecycle(initialValue = null)
    val allItems by viewModel.allVideosWithChannels.collectAsStateWithLifecycle()
    val subscriptions by viewModel.userSubscriptions.collectAsStateWithLifecycle()
    val reaction by viewModel.observeReaction(videoId).collectAsStateWithLifecycle(initialValue = null)
    val comments by viewModel.observeComments(videoId).collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val playlistItems by viewModel.allPlaylistItems.collectAsStateWithLifecycle()

    var isFullscreen by remember { mutableStateOf(false) }
    var expandDescription by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var notifMenuExpanded by remember { mutableStateOf(false) }

    BackHandler {
        if (isFullscreen) isFullscreen = false else onBack()
    }

    val currentVideo = video
    if (currentVideo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val channel by viewModel.observeChannel(currentVideo.channelId).collectAsStateWithLifecycle(initialValue = null)
    val subscription = remember(subscriptions, currentVideo.channelId) {
        subscriptions.firstOrNull { it.channelId == currentVideo.channelId }
    }
    val recommendations = remember(allItems, videoId) {
        allItems.filter { it.video.id != videoId && it.video.visibility == "PUBLIC" }
    }
    val currentQueueIdx = remember(allItems, videoId) {
        allItems.indexOfFirst { it.video.id == videoId }
    }

    LaunchedEffect(videoId) {
        viewModel.recordVideoWatch(currentVideo)
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MyVideoPlayerView(
                videoUri = currentVideo.videoUri,
                videoTitle = currentVideo.title,
                isFullscreen = true,
                onToggleFullscreen = { isFullscreen = false },
                hasPrevious = currentQueueIdx > 0,
                hasNext = currentQueueIdx in 0 until (allItems.size - 1),
                onPrevious = {
                    if (currentQueueIdx > 0) onWatchAnother(allItems[currentQueueIdx - 1].video.id)
                },
                onNext = {
                    if (currentQueueIdx in 0 until (allItems.size - 1)) {
                        onWatchAnother(allItems[currentQueueIdx + 1].video.id)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Player Header + Back Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            MyVideoPlayerView(
                videoUri = currentVideo.videoUri,
                videoTitle = currentVideo.title,
                isFullscreen = false,
                onToggleFullscreen = { isFullscreen = true },
                hasPrevious = currentQueueIdx > 0,
                hasNext = currentQueueIdx in 0 until (allItems.size - 1),
                onPrevious = {
                    if (currentQueueIdx > 0) onWatchAnother(allItems[currentQueueIdx - 1].video.id)
                },
                onNext = {
                    if (currentQueueIdx in 0 until (allItems.size - 1)) {
                        onWatchAnother(allItems[currentQueueIdx + 1].video.id)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Video Title, Stats, Expandable Description
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandDescription = !expandDescription }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = currentVideo.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatCompactCount(currentVideo.viewsCount)} views · ${formatRelativeTime(currentVideo.createdAt)} · ${currentVideo.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expandDescription) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = currentVideo.description.ifBlank { "No description provided." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (currentVideo.hashtagsCsv.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        currentVideo.hashtagsCsv.split(" ").filter { it.isNotBlank() }.forEach { tag ->
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = StudioBlue,
                                                modifier = Modifier.clickable { onOpenHashtagSearch(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons Row (Like, Dislike, Share, Watch Later, Playlist, YouTube, Report)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.toggleReaction(currentVideo, "LIKE") },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (reaction?.reaction == "LIKE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.testTag("watch_like_btn")
                    ) {
                        Icon(
                            if (reaction?.reaction == "LIKE") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formatCompactCount(currentVideo.likesCount))
                    }

                    FilledTonalButton(
                        onClick = { viewModel.toggleReaction(currentVideo, "DISLIKE") },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (reaction?.reaction == "DISLIKE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.testTag("watch_dislike_btn")
                    ) {
                        Icon(
                            if (reaction?.reaction == "DISLIKE") Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch \"${currentVideo.title}\" on MY VIDEO!")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                        },
                        modifier = Modifier.testTag("watch_share_btn")
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    FilledTonalButton(
                        onClick = { showPlaylistSheet = true },
                        modifier = Modifier.testTag("watch_save_btn")
                    ) {
                        Icon(Icons.Outlined.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }

                // Official YouTube external open option when linked/imported + Report
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentVideo.isYouTubeLinked && currentVideo.youtubeVideoId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                YouTubeOAuthHelper.openInYouTube(context, currentVideo.youtubeVideoId)
                            },
                            modifier = Modifier.testTag("open_in_youtube_btn")
                        ) {
                            Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = StudioCrimson)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in YouTube App / Web")
                        }
                    }
                    TextButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report")
                    }
                }
            }

            // Channel Row & Subscribe
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { channel?.let { onOpenChannel(it.id) } }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelAvatar(channel = channel, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel?.name ?: "Creator Channel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (channel?.isVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.CheckCircle, null, tint = StudioBlue, modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            text = "${formatCompactCount(channel?.subscriberCount ?: 0L)} subscribers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (subscription != null) {
                        Box {
                            IconButton(onClick = { notifMenuExpanded = true }) {
                                Icon(
                                    imageVector = when (subscription.notificationLevel) {
                                        "ALL" -> Icons.Filled.NotificationsActive
                                        "NONE" -> Icons.Outlined.NotificationsOff
                                        else -> Icons.Outlined.Notifications
                                    },
                                    contentDescription = "Notification preferences"
                                )
                            }
                            DropdownMenu(
                                expanded = notifMenuExpanded,
                                onDismissRequest = { notifMenuExpanded = false }
                            ) {
                                listOf("ALL" to "All notifications", "PERSONALIZED" to "Personalized", "NONE" to "None")
                                    .forEach { (level, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                notifMenuExpanded = false
                                                channel?.let { viewModel.setNotificationLevel(it.id, level) }
                                            }
                                        )
                                    }
                            }
                        }
                    }
                    Button(
                        onClick = { channel?.let { viewModel.toggleSubscribe(it.id) } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subscription != null) MaterialTheme.colorScheme.surfaceContainer else StudioCrimson,
                            contentColor = if (subscription != null) MaterialTheme.colorScheme.onSurface else Color.White
                        ),
                        modifier = Modifier.testTag("watch_subscribe_btn")
                    ) {
                        Text(if (subscription != null) "Subscribed" else "Subscribe")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Comments & Replies Section
            item {
                CommentsSectionContent(
                    comments = comments,
                    onPostComment = { text, parentId -> viewModel.postComment(videoId, text, parentId) },
                    onLikeComment = { viewModel.likeComment(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            items(recommendations, key = { it.video.id }) { rec ->
                VideoFeedCard(
                    item = rec,
                    onWatchClick = { onWatchAnother(rec.video.id) },
                    onChannelClick = { onOpenChannel(rec.channel.id) },
                    onSaveWatchLater = { viewModel.toggleWatchLater(rec.video.id) },
                    onShare = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Watch \"${rec.video.title}\" on MY VIDEO!")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                    },
                    onReport = { showReportDialog = true }
                )
            }
        }
    }

    if (showPlaylistSheet) {
        var newPlaylistTitle by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showPlaylistSheet = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Save video to...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                playlists.forEach { pl ->
                    val inPlaylist = playlistItems.any { it.playlistId == pl.id && it.videoId == videoId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.togglePlaylistVideo(pl.id, videoId) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = inPlaylist,
                            onCheckedChange = { viewModel.togglePlaylistVideo(pl.id, videoId) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pl.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        label = { Text("New playlist title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newPlaylistTitle.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistTitle, videoId)
                                newPlaylistTitle = ""
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showReportDialog) {
        ReportVideoDialog(
            videoTitle = currentVideo.title,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                viewModel.reportVideo(currentVideo, reason)
                showReportDialog = false
            }
        )
    }
}

@Composable
fun CommentsSectionContent(
    comments: List<CommentEntity>,
    onPostComment: (String, String?) -> Unit,
    onLikeComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentDraft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) }

    val topLevel = remember(comments) { comments.filter { it.parentCommentId == null } }
    val repliesByParent = remember(comments) {
        comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId!! }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Comments (${comments.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (replyingTo != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ${replyingTo!!.authorHandle}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, "Cancel reply", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentDraft,
                onValueChange = { commentDraft = it },
                placeholder = { Text(if (replyingTo != null) "Add a reply..." else "Add a comment...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("comment_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commentDraft.isNotBlank()) {
                        onPostComment(commentDraft, replyingTo?.id)
                        commentDraft = ""
                        replyingTo = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCrimson)
                    .testTag("comment_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Post comment", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        topLevel.forEach { comment ->
            CommentRowItem(
                comment = comment,
                onLike = { onLikeComment(comment.id) },
                onReply = { replyingTo = comment }
            )
            repliesByParent[comment.id].orEmpty().forEach { reply ->
                CommentRowItem(
                    comment = reply,
                    onLike = { onLikeComment(reply.id) },
                    onReply = { replyingTo = comment },
                    isReply = true
                )
            }
        }
    }
}

@Composable
private fun CommentRowItem(
    comment: CommentEntity,
    onLike: () -> Unit,
    onReply: () -> Unit,
    isReply: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 28.dp else 0.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.authorHandle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatRelativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = comment.text,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onLike, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Outlined.ThumbUp, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(comment.likesCount.toString(), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onReply, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Reply", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

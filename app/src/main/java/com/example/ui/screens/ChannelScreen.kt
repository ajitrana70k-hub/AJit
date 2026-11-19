package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.MediaStorageHelper
import com.example.data.VideoWithChannel
import com.example.data.YouTubeOAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.viewmodel.MyVideoViewModel
import kotlinx.coroutines.launch

private enum class ChannelTab(val label: String) {
    VIDEOS("Videos"),
    SHORTS("Shorts"),
    PLAYLISTS("Playlists"),
    COMMUNITY("Community"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelId: String,
    viewModel: MyVideoViewModel,
    onBack: () -> Unit,
    onWatchVideo: (String) -> Unit,
    onOpenShort: (String) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val channel by viewModel.observeChannel(channelId).collectAsStateWithLifecycle(initialValue = null)
    val myChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosWithChannels.collectAsStateWithLifecycle()
    val subscriptions by viewModel.userSubscriptions.collectAsStateWithLifecycle()
    val communityPosts by viewModel.observeCommunityPosts(channelId).collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by viewModel.userPlaylists.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(ChannelTab.VIDEOS) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newPostDraft by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    val ch = channel
    if (ch == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isOwner = myChannel?.id == ch.id
    val subscription = subscriptions.firstOrNull { it.channelId == ch.id }
    val channelLongVideos = remember(allVideos, ch.id) {
        allVideos.filter { it.channel.id == ch.id && !it.video.isShort }
    }
    val channelShorts = remember(allVideos, ch.id) {
        allVideos.filter { it.channel.id == ch.id && it.video.isShort }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(ch.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("edit_channel_btn")) {
                            Icon(Icons.Outlined.Edit, "Edit Channel")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Channel Banner
            item {
                if (ch.bannerUri.isNotBlank()) {
                    AsyncImage(
                        model = ch.bannerUri,
                        contentDescription = "Channel banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(128.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = drawableForKey(ch.bannerDrawableKey)),
                        contentDescription = "Channel banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(128.dp)
                    )
                }

                // Channel Header Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(channel = ch, size = 64.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ch.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                if (ch.isVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.CheckCircle, null, tint = StudioBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                "${ch.handle} · ${formatCompactCount(ch.subscriberCount)} subscribers · ${formatCompactCount(ch.totalViews)} views",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(ch.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isOwner) {
                            Button(
                                onClick = onOpenStudio,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Dashboard, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage in Studio")
                            }
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit Channel")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.toggleSubscribe(ch.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (subscription != null) MaterialTheme.colorScheme.surfaceContainer else StudioCrimson,
                                    contentColor = if (subscription != null) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (subscription != null) "Subscribed" else "Subscribe")
                            }
                            if (subscription != null) {
                                OutlinedButton(
                                    onClick = {
                                        val next = when (subscription.notificationLevel) {
                                            "ALL" -> "PERSONALIZED"
                                            "PERSONALIZED" -> "NONE"
                                            else -> "ALL"
                                        }
                                        viewModel.setNotificationLevel(ch.id, next)
                                    }
                                ) {
                                    Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(subscription.notificationLevel.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                }

                PrimaryScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    edgePadding = 12.dp
                ) {
                    ChannelTab.entries.forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            when (activeTab) {
                ChannelTab.VIDEOS -> {
                    if (channelLongVideos.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Outlined.VideoLibrary,
                                title = "No Long Videos",
                                subtitle = "Uploaded long-form videos appear here."
                            )
                        }
                    } else {
                        items(channelLongVideos, key = { it.video.id }) { item ->
                            VideoFeedCard(
                                item = item,
                                onWatchClick = { onWatchVideo(item.video.id) },
                                onChannelClick = {},
                                onSaveWatchLater = { viewModel.toggleWatchLater(item.video.id) },
                                onShare = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Watch \"${item.video.title}\" on MY VIDEO!")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share"))
                                },
                                onReport = { viewModel.reportVideo(item.video, "Reported from channel") }
                            )
                        }
                    }
                }

                ChannelTab.SHORTS -> {
                    if (channelShorts.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Outlined.Bolt,
                                title = "No Shorts Yet",
                                subtitle = "Vertical Shorts uploaded to this channel appear here."
                            )
                        }
                    } else {
                        items(channelShorts.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { item ->
                                    ShortShelfCard(
                                        item = item,
                                        onClick = { onOpenShort(item.video.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                ChannelTab.PLAYLISTS -> {
                    items(playlists, key = { it.id }) { pl ->
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.PlaylistPlay, null) },
                            headlineContent = { Text(pl.title) },
                            supportingContent = { Text(if (pl.isWatchLater) "Saved Watch Later queue" else "Creator playlist") }
                        )
                    }
                }

                ChannelTab.COMMUNITY -> {
                    if (isOwner) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPostDraft,
                                    onValueChange = { newPostDraft = it },
                                    label = { Text("Post an update to subscribers...") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.createCommunityPost(newPostDraft)
                                        newPostDraft = ""
                                    }
                                ) {
                                    Text("Post")
                                }
                            }
                        }
                    }
                    items(communityPosts, key = { it.id }) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(ch.name, style = MaterialTheme.typography.labelLarge)
                                Text(formatRelativeTime(post.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(post.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.likeCommunityPost(post.id) }) {
                                    Icon(Icons.Outlined.ThumbUp, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${post.likesCount}")
                                }
                            }
                        }
                    }
                }

                ChannelTab.ABOUT -> {
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Description", style = MaterialTheme.typography.titleMedium)
                            Text(ch.description, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                            Text("Channel Stats", style = MaterialTheme.typography.titleMedium)
                            Text("Total Views: ${formatCompactCount(ch.totalViews)}")
                            Text("Watch Hours (12M): ${"%.1f".format(ch.watchHours12M)} hrs")
                            Text("Shorts Views (90D): ${formatCompactCount(ch.shortsViews90D)}")
                            if (ch.youtubeConnected) {
                                HorizontalDivider()
                                Text("Connected YouTube Channel: ${ch.youtubeChannelTitle}", fontWeight = FontWeight.SemiBold)
                                OutlinedButton(
                                    onClick = {
                                        YouTubeOAuthHelper.openInYouTube(context, null, ch.youtubeChannelId)
                                    }
                                ) {
                                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Channel on YouTube")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var nameEdit by remember { mutableStateOf(ch.name) }
        var descEdit by remember { mutableStateOf(ch.description) }
        var avatarUriEdit by remember { mutableStateOf<String?>(null) }
        var bannerUriEdit by remember { mutableStateOf<String?>(null) }

        val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch {
                avatarUriEdit = MediaStorageHelper.importImage(context, uri)
            }
        }
        val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch {
                bannerUriEdit = MediaStorageHelper.importImage(context, uri)
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Customize Channel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameEdit,
                        onValueChange = { nameEdit = it },
                        label = { Text("Channel Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descEdit,
                        onValueChange = { descEdit = it },
                        label = { Text("Description") },
                        minLines = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (avatarUriEdit != null) "Avatar Ready" else "Pick Avatar")
                        }
                        OutlinedButton(
                            onClick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (bannerUriEdit != null) "Banner Ready" else "Pick Banner")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateChannelDetails(nameEdit, descEdit, avatarUriEdit, bannerUriEdit)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

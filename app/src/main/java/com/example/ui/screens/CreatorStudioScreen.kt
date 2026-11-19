package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VideoEntity
import com.example.data.YouTubeOAuthHelper
import com.example.ui.components.*
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

private enum class StudioTab(val label: String) {
    ANALYTICS("Analytics"),
    CONTENT("Content"),
    COMMENTS("Comments"),
    AUDIENCE("Audience"),
    YOUTUBE("YouTube OAuth")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    viewModel: MyVideoViewModel,
    initialTabIndex: Int = 0,
    onBack: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenMonetization: () -> Unit,
    onWatchVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosWithChannels.collectAsStateWithLifecycle()
    val allComments by viewModel.allComments.collectAsStateWithLifecycle()
    val youtubeClientId by viewModel.youtubeClientId.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(StudioTab.entries.getOrElse(initialTabIndex) { StudioTab.ANALYTICS }) }
    var editingVideo by remember { mutableStateOf<VideoEntity?>(null) }

    BackHandler(onBack = onBack)

    val ch = currentChannel
    if (ch == null) {
        EmptyStatePanel(
            icon = Icons.Outlined.Dashboard,
            title = "Channel Required",
            subtitle = "Sign in to access MY VIDEO Creator Studio."
        )
        return
    }

    val myVideos = remember(allVideos, ch.id) {
        allVideos.filter { it.channel.id == ch.id }.map { it.video }
    }
    val myVideoIds = remember(myVideos) { myVideos.map { it.id }.toSet() }
    val myComments = remember(allComments, myVideoIds) {
        allComments.filter { it.videoId in myVideoIds }
    }

    val totalImpressions = myVideos.sumOf { it.impressionsCount }
    val avgCtr = if (myVideos.isNotEmpty()) myVideos.map { it.ctrPercent }.average() else 0.0

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("MY VIDEO Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenMonetization) {
                        Icon(Icons.Outlined.MonetizationOn, null, tint = StudioEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Earn")
                    }
                    IconButton(onClick = onOpenUpload) {
                        Icon(Icons.Filled.AddCircleOutline, "Upload Video")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryScrollableTabRow(selectedTabIndex = activeTab.ordinal, edgePadding = 12.dp) {
                StudioTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    StudioTab.ANALYTICS -> {
                        item {
                            Text("Channel Performance", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StudioKpiCard("Views", formatCompactCount(ch.totalViews), Modifier.weight(1f))
                                StudioKpiCard("Watch Time", "${"%.1f".format(ch.watchHours12M)}h", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StudioKpiCard("Impressions", formatCompactCount(totalImpressions), Modifier.weight(1f))
                                StudioKpiCard("Avg CTR", "${"%.1f".format(avgCtr)}%", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StudioKpiCard("Subscribers", formatCompactCount(ch.subscriberCount), Modifier.weight(1f))
                                StudioKpiCard("Est. Revenue", formatCurrency(ch.estimatedBalanceUsd), Modifier.weight(1f))
                            }
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("7-Day Channel Views", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val base = (ch.totalViews / 70f).coerceAtLeast(120f)
                                    AnalyticsBarChart(
                                        labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                                        values = listOf(base * 0.8f, base * 1.1f, base * 0.95f, base * 1.4f, base * 1.25f, base * 1.7f, base * 1.55f)
                                    )
                                }
                            }
                        }
                    }

                    StudioTab.CONTENT -> {
                        if (myVideos.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Outlined.VideoCall,
                                    title = "No Uploaded Videos",
                                    subtitle = "Upload from gallery or record with CameraX.",
                                    actionLabel = "Upload Video",
                                    onAction = onOpenUpload
                                )
                            }
                        } else {
                            items(myVideos, key = { it.id }) { vid ->
                                Card(shape = RoundedCornerShape(14.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(vid.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${vid.visibility} · ${vid.category} · ${formatCompactCount(vid.viewsCount)} views · ${formatCurrency(vid.adRevenueUsd)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { onWatchVideo(vid.id) }) {
                                                Icon(Icons.Filled.PlayArrow, "Watch")
                                            }
                                            IconButton(onClick = { editingVideo = vid }) {
                                                Icon(Icons.Outlined.Edit, "Edit")
                                            }
                                            IconButton(onClick = { viewModel.deleteVideo(vid.id) }) {
                                                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StudioTab.COMMENTS -> {
                        if (myComments.isEmpty()) {
                            item {
                                EmptyStatePanel(
                                    icon = Icons.Outlined.Comment,
                                    title = "No Comments Yet",
                                    subtitle = "Viewer comments across your videos will appear here."
                                )
                            }
                        } else {
                            items(myComments, key = { it.id }) { cmt ->
                                var replyDraft by remember { mutableStateOf("") }
                                Card(shape = RoundedCornerShape(14.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cmt.authorHandle, style = MaterialTheme.typography.labelLarge)
                                            IconButton(
                                                onClick = { viewModel.deleteComment(cmt.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Outlined.Delete, "Remove comment", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(cmt.text, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = replyDraft,
                                                onValueChange = { replyDraft = it },
                                                placeholder = { Text("Quick reply...") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(onClick = {
                                                if (replyDraft.isNotBlank()) {
                                                    viewModel.postComment(cmt.videoId, replyDraft, cmt.id)
                                                    replyDraft = ""
                                                }
                                            }) {
                                                Text("Reply")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    StudioTab.AUDIENCE -> {
                        item {
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Audience Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Watch Time from Subscribers: 42% · Non-Subscribers: 58%")
                                    LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth())
                                    HorizontalDivider()
                                    Text("Top Viewing Regions", style = MaterialTheme.typography.titleSmall)
                                    Text("United States (38%) · India (22%) · United Kingdom (14%) · Canada (11%)")
                                    HorizontalDivider()
                                    Text("Device Distribution", style = MaterialTheme.typography.titleSmall)
                                    Text("Mobile App (74%) · Tablet/Foldable (16%) · Desktop Web (10%)")
                                }
                            }
                        }
                    }

                    StudioTab.YOUTUBE -> {
                        item {
                            YouTubeIntegrationSection(
                                channel = ch,
                                savedClientId = youtubeClientId,
                                onSaveClientId = { viewModel.saveYoutubeOAuthClientId(it) },
                                onLaunchOAuth = { clientId, uploadScope ->
                                    val scopes = if (uploadScope) {
                                        listOf(YouTubeOAuthHelper.YOUTUBE_READONLY_SCOPE, YouTubeOAuthHelper.YOUTUBE_UPLOAD_SCOPE)
                                    } else {
                                        listOf(YouTubeOAuthHelper.YOUTUBE_READONLY_SCOPE)
                                    }
                                    YouTubeOAuthHelper.launchBrowserOAuth(context, clientId, scopes)
                                },
                                onConnectWithToken = { token, uploadScope ->
                                    viewModel.connectYouTubeWithToken(token, uploadScope)
                                },
                                onDisconnect = { viewModel.disconnectYouTube() },
                                onOpenInYouTube = {
                                    YouTubeOAuthHelper.openInYouTube(context, null, ch.youtubeChannelId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    editingVideo?.let { vid ->
        var titleEdit by remember { mutableStateOf(vid.title) }
        var descEdit by remember { mutableStateOf(vid.description) }
        var visEdit by remember { mutableStateOf(vid.visibility) }
        var catEdit by remember { mutableStateOf(vid.category) }
        var tagsEdit by remember { mutableStateOf(vid.tagsCsv) }
        var hashEdit by remember { mutableStateOf(vid.hashtagsCsv) }

        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Edit Video Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = titleEdit, onValueChange = { titleEdit = it }, label = { Text("Title") })
                    OutlinedTextField(value = descEdit, onValueChange = { descEdit = it }, label = { Text("Description") }, minLines = 2)
                    OutlinedTextField(value = catEdit, onValueChange = { catEdit = it }, label = { Text("Category") })
                    OutlinedTextField(value = visEdit, onValueChange = { visEdit = it }, label = { Text("Visibility (PUBLIC/UNLISTED/PRIVATE)") })
                    OutlinedTextField(value = hashEdit, onValueChange = { hashEdit = it }, label = { Text("Hashtags") })
                    OutlinedTextField(value = tagsEdit, onValueChange = { tagsEdit = it }, label = { Text("Tags") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateVideoMetadata(vid.id, titleEdit, descEdit, catEdit, visEdit, tagsEdit, hashEdit)
                    editingVideo = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingVideo = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudioKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YouTubeIntegrationSection(
    channel: com.example.data.ChannelEntity,
    savedClientId: String,
    onSaveClientId: (String) -> Unit,
    onLaunchOAuth: (String, Boolean) -> Unit,
    onConnectWithToken: (String, Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onOpenInYouTube: () -> Unit
) {
    var clientIdInput by remember(savedClientId) { mutableStateOf(savedClientId) }
    var accessTokenInput by remember { mutableStateOf("") }
    var requestUploadScope by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayCircle, null, tint = StudioCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Official YouTube OAuth Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Connect your existing YouTube account via official Google OAuth 2.0. MY VIDEO never asks for your Google password and only performs YouTube API operations you explicitly authorize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (channel.youtubeConnected) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Connected: ${channel.youtubeChannelTitle}", fontWeight = FontWeight.Bold)
                    Text("Channel ID: ${channel.youtubeChannelId}", style = MaterialTheme.typography.bodySmall)
                    Text("YouTube Subscribers: ${formatCompactCount(channel.youtubeSubscribers)} · Videos: ${channel.youtubeVideoCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Authorized Scopes: ${channel.youtubeScopes}", style = MaterialTheme.typography.labelSmall, color = StudioBlue)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenInYouTube, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in YouTube")
                        }
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Authorize via Google OAuth 2.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Google OAuth Client ID") },
                    placeholder = { Text("xxxxx.apps.googleusercontent.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requestUploadScope, onCheckedChange = { requestUploadScope = it })
                    Text("Also authorize youtube.upload scope (opt-in cross-publishing)", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSaveClientId(clientIdInput) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Client ID")
                    }
                    Button(
                        onClick = { onLaunchOAuth(clientIdInput.trim(), requestUploadScope) },
                        enabled = clientIdInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Authorize Browser")
                    }
                }
                HorizontalDivider()
                Text("Verify OAuth Access Token & Import Channel Metadata", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = accessTokenInput,
                    onValueChange = { accessTokenInput = it },
                    label = { Text("Authorized Google OAuth Bearer Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onConnectWithToken(accessTokenInput, requestUploadScope) },
                    enabled = accessTokenInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync YouTube Channel & Eligible Videos")
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.*
import com.example.ui.theme.StudioCrimson
import com.example.ui.viewmodel.HomeFeedFilter
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MyVideoViewModel,
    onWatchVideo: (String) -> Unit,
    onOpenShort: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenSearch: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homeVideos by viewModel.homeVideos.collectAsStateWithLifecycle()
    val shorts by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.homeFilter.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()

    var reportTarget by remember { mutableStateOf<VideoEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { MyVideoBrandLogo() },
                actions = {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge(containerColor = StudioCrimson) {
                                        Text(unreadNotifs.coerceAtMost(99).toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(
                        onClick = { onOpenSearch(false) },
                        modifier = Modifier.testTag("home_search_btn")
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search videos")
                    }
                    IconButton(
                        onClick = { onOpenSearch(true) },
                        modifier = Modifier.testTag("home_voice_search_btn")
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Voice search")
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable(onClick = onOpenProfile)
                            .testTag("home_profile_avatar")
                    ) {
                        ChannelAvatar(channel = currentChannel, size = 32.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category & Recommendation Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HomeFeedFilter.entries) { filter ->
                        FilterChip(
                            selected = filter == selectedFilter,
                            onClick = { viewModel.setHomeFilter(filter) },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("category_chip_${filter.name}")
                        )
                    }
                }
            }

            // Shorts Shelf
            if (shorts.isNotEmpty() && selectedFilter == HomeFeedFilter.ALL) {
                item {
                    Column(modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = StudioCrimson,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shorts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(shorts, key = { it.video.id }) { shortItem ->
                                ShortShelfCard(
                                    item = shortItem,
                                    onClick = { onOpenShort(shortItem.video.id) }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (homeVideos.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Outlined.VideoLibrary,
                        title = "No videos in ${selectedFilter.label}",
                        subtitle = "Try another category or upload the first video in this topic.",
                        actionLabel = "Show All Videos",
                        onAction = { viewModel.setHomeFilter(HomeFeedFilter.ALL) }
                    )
                }
            } else {
                items(homeVideos, key = { it.video.id }) { item ->
                    VideoFeedCard(
                        item = item,
                        onWatchClick = { onWatchVideo(item.video.id) },
                        onChannelClick = { onOpenChannel(item.channel.id) },
                        onSaveWatchLater = { viewModel.toggleWatchLater(item.video.id) },
                        onShare = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch \"${item.video.title}\" by ${item.channel.name} on MY VIDEO!"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                        },
                        onReport = { reportTarget = item.video }
                    )
                }
            }
        }
    }

    reportTarget?.let { target ->
        ReportVideoDialog(
            videoTitle = target.title,
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                viewModel.reportVideo(target, reason)
                reportTarget = null
            }
        )
    }
}

@Composable
fun ReportVideoDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val reasons = listOf(
        "Spam or misleading metadata",
        "Fraudulent engagement / bot views",
        "Harassment or harmful content",
        "Copyright infringement"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Content") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Why are you reporting \"$videoTitle\"?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasons.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(r, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason) }) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

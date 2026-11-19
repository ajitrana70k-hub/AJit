package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MyVideoBrandLogo
import com.example.ui.components.VideoFeedCard
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: MyVideoViewModel,
    onWatchVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscriptions by viewModel.userSubscriptions.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val subscribedVideos by viewModel.subscribedVideos.collectAsStateWithLifecycle()

    var selectedChannelFilter by remember { mutableStateOf<String?>(null) }

    val subscribedChannels = remember(subscriptions, allChannels) {
        val ids = subscriptions.map { it.channelId }.toSet()
        allChannels.filter { it.id in ids }
    }

    val displayedVideos = remember(subscribedVideos, selectedChannelFilter) {
        if (selectedChannelFilter == null) subscribedVideos
        else subscribedVideos.filter { it.channel.id == selectedChannelFilter }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MyVideoBrandLogo(showWordmark = false)
                        Text("Subscriptions")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (subscribedChannels.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Outlined.Subscriptions,
                title = "No Subscriptions Yet",
                subtitle = "Subscribe to creators on Home or Shorts to follow their newest uploads here.",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedChannelFilter == null,
                                onClick = { selectedChannelFilter = null },
                                label = { Text("All (${subscribedChannels.size})") }
                            )
                        }
                        items(subscribedChannels, key = { it.id }) { ch ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable {
                                        selectedChannelFilter = if (selectedChannelFilter == ch.id) null else ch.id
                                    }
                            ) {
                                ChannelAvatar(channel = ch, size = 50.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { onOpenChannel(ch.id) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("Visit", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }

                items(displayedVideos, key = { it.video.id }) { item ->
                    VideoFeedCard(
                        item = item,
                        onWatchClick = { onWatchVideo(item.video.id) },
                        onChannelClick = { onOpenChannel(item.channel.id) },
                        onSaveWatchLater = { viewModel.toggleWatchLater(item.video.id) },
                        onShare = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch \"${item.video.title}\" on MY VIDEO!")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                        },
                        onReport = { viewModel.reportVideo(item.video, "Reported from subscriptions") }
                    )
                }
            }
        }
    }
}

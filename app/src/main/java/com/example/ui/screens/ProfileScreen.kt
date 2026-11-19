package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ThemePreference
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MyVideoViewModel,
    onOpenChannel: (String) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenMonetization: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdmin: () -> Unit,
    onWatchVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val channel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val playlists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosWithChannels.collectAsStateWithLifecycle()

    val historyVideos = remember(watchHistory, allVideos) {
        val map = allVideos.associateBy { it.video.id }
        watchHistory.mapNotNull { map[it.videoId] }.take(10)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Studio") },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Outlined.Notifications, "Notifications")
                    }
                    IconButton(onClick = { viewModel.logout() }, modifier = Modifier.testTag("logout_btn")) {
                        Icon(Icons.Outlined.Logout, "Sign Out")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Account Header Card
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelAvatar(channel = channel, size = 60.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel?.name ?: user?.displayName ?: "Creator",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${channel?.handle ?: ""} · ${user?.email ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                AssistChip(
                                    onClick = { channel?.let { onOpenChannel(it.id) } },
                                    label = { Text("View Channel") }
                                )
                            }
                        }
                    }
                }
            }

            // Creator Quick Actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedCard(
                        onClick = { onOpenStudio(0) },
                        modifier = Modifier.weight(1f).testTag("profile_studio_btn")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.Dashboard, null, tint = StudioCrimson)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Creator Studio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Analytics & videos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ElevatedCard(
                        onClick = onOpenMonetization,
                        modifier = Modifier.weight(1f).testTag("profile_monetization_btn")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Outlined.MonetizationOn, null, tint = StudioEmerald)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Monetization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Earnings & payouts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Watch History Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Watch History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (historyVideos.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearWatchHistory() }) {
                            Text("Clear")
                        }
                    }
                }
                if (historyVideos.isEmpty()) {
                    Text(
                        "Videos you watch will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(historyVideos, key = { it.video.id }) { item ->
                            Card(
                                onClick = { onWatchVideo(item.video.id) },
                                modifier = Modifier.width(164.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    VideoThumbnailImage(
                                        video = item.video,
                                        modifier = Modifier.fillMaxWidth().height(92.dp)
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            item.video.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playlists & Watch Later
            item {
                Text("Playlists & Watch Later", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column {
                        playlists.forEach { pl ->
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        if (pl.isWatchLater) Icons.Outlined.WatchLater else Icons.Outlined.PlaylistPlay,
                                        null
                                    )
                                },
                                headlineContent = { Text(pl.title) }
                            )
                        }
                    }
                }
            }

            // Theme Mode Switcher (Dark Mode & Light Mode)
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemePreference.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, ThemePreference.entries.size),
                                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                }
            }

            // Integrations, Admin & Account Actions
            item {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column {
                        ListItem(
                            leadingContent = { Icon(Icons.Filled.PlayCircle, null, tint = StudioCrimson) },
                            headlineContent = { Text("YouTube Account Integration") },
                            supportingContent = {
                                Text(
                                    if (channel?.youtubeConnected == true) "Connected: ${channel?.youtubeChannelTitle}"
                                    else "Connect via official Google OAuth"
                                )
                            },
                            modifier = Modifier.clickable { onOpenStudio(4) }.testTag("profile_youtube_oauth_btn")
                        )
                        HorizontalDivider()
                        if (user?.role == "ADMIN") {
                            ListItem(
                                leadingContent = { Icon(Icons.Outlined.AdminPanelSettings, null, tint = StudioBlue) },
                                headlineContent = { Text("Platform Admin Dashboard") },
                                supportingContent = { Text("Moderate videos, payouts, ads & spam detection") },
                                modifier = Modifier.clickable(onClick = onOpenAdmin).testTag("profile_admin_btn")
                            )
                            HorizontalDivider()
                        }
                        ListItem(
                            leadingContent = { Icon(Icons.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                            headlineContent = { Text("Sign Out") },
                            supportingContent = { Text("Switch account or sign out securely") },
                            modifier = Modifier.clickable { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

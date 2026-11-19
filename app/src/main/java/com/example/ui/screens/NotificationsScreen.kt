package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MyVideoViewModel,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Outlined.NotificationsNone,
                title = "All Caught Up",
                subtitle = "Subscriber milestones, comments, uploads, and payout alerts appear here.",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    val (icon, tint) = when (notif.type) {
                        "SUBSCRIBER" -> Icons.Filled.PersonAdd to StudioCrimson
                        "COMMENT" -> Icons.Filled.Comment to StudioBlue
                        "LIKE" -> Icons.Filled.ThumbUp to StudioCrimson
                        "UPLOAD" -> Icons.Filled.CloudDone to StudioEmerald
                        "PAYMENT" -> Icons.Filled.Payments to StudioEmerald
                        "MONETIZATION" -> Icons.Filled.MonetizationOn to StudioAmber
                        else -> Icons.Filled.PlayCircle to StudioCrimson
                    }
                    ListItem(
                        leadingContent = {
                            Icon(icon, null, tint = tint)
                        },
                        headlineContent = {
                            Text(
                                notif.title,
                                fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatRelativeTime(notif.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        trailingContent = {
                            if (!notif.isRead) {
                                Badge(containerColor = StudioCrimson)
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.markNotificationRead(notif.id)
                            when {
                                notif.relatedId.startsWith("vid_") || notif.relatedId.startsWith("short_") ->
                                    onOpenVideo(notif.relatedId)
                                notif.relatedId.startsWith("ch_") ->
                                    onOpenChannel(notif.relatedId)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }
    }
}

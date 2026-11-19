package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.StudioCrimson
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: MyVideoViewModel,
    initialShortId: String? = null,
    onOpenChannel: (String) -> Unit,
    onOpenHashtagSearch: (String) -> Unit,
    onCreateShort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shorts by viewModel.shortsFeed.collectAsStateWithLifecycle()
    val subscriptions by viewModel.userSubscriptions.collectAsStateWithLifecycle()
    val subIds = remember(subscriptions) { subscriptions.map { it.channelId }.toSet() }

    if (shorts.isEmpty()) {
        EmptyStatePanel(
            icon = Icons.Outlined.Bolt,
            title = "No Shorts Yet",
            subtitle = "Record or upload a vertical Short to get started!",
            actionLabel = "Create Short",
            onAction = onCreateShort,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val startIndex = remember(shorts, initialShortId) {
        if (initialShortId == null) 0
        else shorts.indexOfFirst { it.video.id == initialShortId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { shorts.size })
    var commentSheetVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagerState.currentPage, shorts) {
        shorts.getOrNull(pagerState.currentPage)?.let { item ->
            viewModel.recordVideoWatch(item.video)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = shorts[page]
            val reaction by viewModel.observeReaction(item.video.id).collectAsStateWithLifecycle(initialValue = null)
            val comments by viewModel.observeComments(item.video.id).collectAsStateWithLifecycle(initialValue = emptyList())
            val isSubscribed = item.channel.id in subIds
            var expandedDesc by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                ShortsVideoSurface(
                    videoUri = item.video.videoUri,
                    isActive = page == pagerState.currentPage,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-to-bottom contrast scrims
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Right-side action rail
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.toggleReaction(item.video, "LIKE") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("short_like_btn")
                        ) {
                            Icon(
                                imageVector = if (reaction?.reaction == "LIKE") Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like Short",
                                tint = if (reaction?.reaction == "LIKE") StudioCrimson else Color.White
                            )
                        }
                        Text(
                            text = formatCompactCount(item.video.likesCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleReaction(item.video, "DISLIKE") },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = if (reaction?.reaction == "DISLIKE") Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                            contentDescription = "Dislike Short",
                            tint = if (reaction?.reaction == "DISLIKE") StudioCrimson else Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { commentSheetVideoId = item.video.id },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("short_comment_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = comments.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch Short \"${item.video.title}\" on MY VIDEO!")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Short"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share Short", tint = Color.White)
                    }
                }

                // Bottom-left metadata overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 80.dp, bottom = 22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChannelAvatar(
                            channel = item.channel,
                            size = 36.dp,
                            modifier = Modifier.clickable { onOpenChannel(item.channel.id) }
                        )
                        Text(
                            text = item.channel.handle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenChannel(item.channel.id) }
                        )
                        Button(
                            onClick = { viewModel.toggleSubscribe(item.channel.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubscribed) Color.White.copy(alpha = 0.2f) else StudioCrimson,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("short_subscribe_btn")
                        ) {
                            Text(if (isSubscribed) "Subscribed" else "Subscribe", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = if (expandedDesc) 4 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { expandedDesc = !expandedDesc }
                    )
                    if (expandedDesc && item.video.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.video.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    if (item.video.hashtagsCsv.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.video.hashtagsCsv.split(" ").filter { it.isNotBlank() }.take(3).forEach { tag ->
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.clickable { onOpenHashtagSearch(tag) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top overlay header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shorts",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = onCreateShort,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("shorts_create_btn")
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Short")
            }
        }
    }

    commentSheetVideoId?.let { vidId ->
        val comments by viewModel.observeComments(vidId).collectAsStateWithLifecycle(initialValue = emptyList())
        ModalBottomSheet(onDismissRequest = { commentSheetVideoId = null }) {
            CommentsSectionContent(
                comments = comments,
                onPostComment = { text, parentId -> viewModel.postComment(vidId, text, parentId) },
                onLikeComment = { viewModel.likeComment(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.formatCompactCount
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MyVideoViewModel,
    initialQuery: String = "",
    triggerVoiceOnOpen: Boolean = false,
    onBack: () -> Unit,
    onWatchVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.commitSearch(spoken)
            }
        }
    }

    fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search MY VIDEO")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: Exception) {
            viewModel.showMessage("Voice speech recognizer unavailable on this device")
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.commitSearch(initialQuery)
        }
    }

    LaunchedEffect(triggerVoiceOnOpen) {
        if (triggerVoiceOnOpen) startVoiceSearch()
    }

    BackHandler(onBack = onBack)

    val suggestions = remember(searchState.query, results, allChannels) {
        val q = searchState.query.trim().lowercase()
        if (q.isBlank()) listOf("#filmmaking", "#desksetup", "Cinema", "Tech", "Synth")
        else {
            (results.map { it.video.title } + allChannels.map { it.name })
                .filter { it.lowercase().contains(q) }
                .distinct()
                .take(5)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchState.query,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search videos, channels, #tags") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchState.query.isNotBlank()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("search_input")
                    )
                },
                actions = {
                    IconButton(onClick = { startVoiceSearch() }, modifier = Modifier.testTag("voice_search_btn")) {
                        Icon(Icons.Outlined.Mic, "Voice search")
                    }
                    IconButton(onClick = { showFilters = !showFilters }, modifier = Modifier.testTag("search_filter_btn")) {
                        Icon(Icons.Outlined.Tune, "Search filters")
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
            // Scope Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchScope.entries) { scope ->
                        FilterChip(
                            selected = searchState.scope == scope,
                            onClick = { viewModel.updateSearchScope(scope) },
                            label = { Text(scope.label) }
                        )
                    }
                }
            }

            // Expandable Advanced Filter Bar
            if (showFilters) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sort By", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SortFilter.entries) { sort ->
                                    FilterChip(
                                        selected = searchState.sortFilter == sort,
                                        onClick = { viewModel.updateSortFilter(sort) },
                                        label = { Text(sort.label) }
                                    )
                                }
                            }
                            Text("Upload Date", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(DateFilter.entries) { df ->
                                    FilterChip(
                                        selected = searchState.dateFilter == df,
                                        onClick = { viewModel.updateDateFilter(df) },
                                        label = { Text(df.label) }
                                    )
                                }
                            }
                            Text("Duration", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(DurationFilter.entries) { dur ->
                                    FilterChip(
                                        selected = searchState.durationFilter == dur,
                                        onClick = { viewModel.updateDurationFilter(dur) },
                                        label = { Text(dur.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search History & Suggestions when query is empty
            if (searchState.query.isBlank()) {
                if (history.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("Clear all")
                            }
                        }
                    }
                    items(history, key = { it.id }) { h ->
                        ListItem(
                            headlineContent = { Text(h.query) },
                            leadingContent = { Icon(Icons.Outlined.History, null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteSearchHistoryItem(h.id) }) {
                                    Icon(Icons.Filled.Close, "Remove")
                                }
                            },
                            modifier = Modifier.clickable { viewModel.commitSearch(h.query) }
                        )
                    }
                }
                item {
                    Text(
                        "Trending Suggestions",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(suggestions) { sug ->
                    ListItem(
                        headlineContent = { Text(sug) },
                        leadingContent = { Icon(Icons.Outlined.TrendingUp, null) },
                        modifier = Modifier.clickable { viewModel.commitSearch(sug) }
                    )
                }
            } else {
                // Matching Channels Section
                val matchingChannels = allChannels.filter {
                    it.name.lowercase().contains(searchState.query.trim().lowercase()) ||
                        it.handle.lowercase().contains(searchState.query.trim().lowercase())
                }
                if (matchingChannels.isNotEmpty()) {
                    item {
                        Text(
                            "Channels",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(matchingChannels, key = { it.id }) { ch ->
                        ListItem(
                            leadingContent = { ChannelAvatar(ch, size = 44.dp) },
                            headlineContent = { Text(ch.name) },
                            supportingContent = { Text("${ch.handle} · ${formatCompactCount(ch.subscriberCount)} subscribers") },
                            modifier = Modifier.clickable {
                                viewModel.commitSearch(searchState.query)
                                onOpenChannel(ch.id)
                            }
                        )
                    }
                }

                if (results.isEmpty() && matchingChannels.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matches found",
                            subtitle = "Try broader keywords or adjust your search filters."
                        )
                    }
                } else {
                    items(results, key = { it.video.id }) { item ->
                        VideoFeedCard(
                            item = item,
                            onWatchClick = {
                                viewModel.commitSearch(searchState.query)
                                onWatchVideo(item.video.id)
                            },
                            onChannelClick = { onOpenChannel(item.channel.id) },
                            onSaveWatchLater = { viewModel.toggleWatchLater(item.video.id) },
                            onShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Watch \"${item.video.title}\" on MY VIDEO!")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                            },
                            onReport = { viewModel.reportVideo(item.video, "Reported from search") }
                        )
                    }
                }
            }
        }
    }
}

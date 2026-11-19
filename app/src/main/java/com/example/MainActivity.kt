package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ThemePreference
import com.example.ui.screens.*
import com.example.ui.theme.MyVideoTheme
import com.example.ui.theme.StudioCrimson
import com.example.ui.viewmodel.MyVideoViewModel

sealed class AppDestination {
    data object Home : AppDestination()
    data class Shorts(val initialShortId: String? = null) : AppDestination()
    data object Subscriptions : AppDestination()
    data object Profile : AppDestination()
    data class Watch(val videoId: String) : AppDestination()
    data class Channel(val channelId: String) : AppDestination()
    data class Search(val initialQuery: String = "", val triggerVoice: Boolean = false) : AppDestination()
    data class Upload(val isShort: Boolean = false) : AppDestination()
    data class Studio(val initialTab: Int = 0) : AppDestination()
    data object Monetization : AppDestination()
    data object Notifications : AppDestination()
    data object Admin : AppDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MyVideoViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
            }

            MyVideoTheme(darkTheme = isDark) {
                MyVideoRootApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVideoRootApp(viewModel: MyVideoViewModel) {
    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (isInitializing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = StudioCrimson)
        }
        return
    }

    if (currentUserId.isNullOrBlank()) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            AuthScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
        return
    }

    // Authenticated App Shell
    key(currentUserId) {
        var backStack by remember { mutableStateOf<List<AppDestination>>(listOf(AppDestination.Home)) }
        var showCreateSheet by remember { mutableStateOf(false) }

        val currentDest = backStack.lastOrNull() ?: AppDestination.Home

        fun navigateTo(dest: AppDestination) {
            backStack = if (dest is AppDestination.Home || dest is AppDestination.Shorts || dest is AppDestination.Subscriptions || dest is AppDestination.Profile) {
                listOf(dest)
            } else {
                backStack + dest
            }
        }

        fun popBack() {
            if (backStack.size > 1) {
                backStack = backStack.dropLast(1)
            } else {
                backStack = listOf(AppDestination.Home)
            }
        }

        val showBottomNav = currentDest is AppDestination.Home ||
            currentDest is AppDestination.Shorts ||
            currentDest is AppDestination.Subscriptions ||
            currentDest is AppDestination.Profile

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 720.dp && showBottomNav

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomNav && !useNavigationRail) {
                        NavigationBar(
                            containerColor = if (currentDest is AppDestination.Shorts) {
                                Color(0xFF0A0C10)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavigationBarItem(
                                selected = currentDest is AppDestination.Home,
                                onClick = { navigateTo(AppDestination.Home) },
                                icon = {
                                    Icon(
                                        if (currentDest is AppDestination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                        "Home"
                                    )
                                },
                                label = { Text("Home", maxLines = 1) },
                                modifier = Modifier.testTag("nav_home")
                            )
                            NavigationBarItem(
                                selected = currentDest is AppDestination.Shorts,
                                onClick = { navigateTo(AppDestination.Shorts()) },
                                icon = {
                                    Icon(
                                        if (currentDest is AppDestination.Shorts) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                                        "Shorts"
                                    )
                                },
                                label = { Text("Shorts", maxLines = 1) },
                                modifier = Modifier.testTag("nav_shorts")
                            )
                            // Center Create (+) Button
                            NavigationBarItem(
                                selected = false,
                                onClick = { showCreateSheet = true },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(StudioCrimson),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Create",
                                            tint = Color.White
                                        )
                                    }
                                },
                                label = { Text("Create", maxLines = 1) },
                                modifier = Modifier.testTag("nav_create")
                            )
                            NavigationBarItem(
                                selected = currentDest is AppDestination.Subscriptions,
                                onClick = { navigateTo(AppDestination.Subscriptions) },
                                icon = {
                                    Icon(
                                        if (currentDest is AppDestination.Subscriptions) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
                                        "Subscriptions"
                                    )
                                },
                                label = { Text("Subs", maxLines = 1) },
                                modifier = Modifier.testTag("nav_subscriptions")
                            )
                            NavigationBarItem(
                                selected = currentDest is AppDestination.Profile,
                                onClick = { navigateTo(AppDestination.Profile) },
                                icon = {
                                    Icon(
                                        if (currentDest is AppDestination.Profile) Icons.Filled.Person else Icons.Outlined.Person,
                                        "Profile"
                                    )
                                },
                                label = { Text("Profile", maxLines = 1) },
                                modifier = Modifier.testTag("nav_profile")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            NavigationRailItem(
                                selected = currentDest is AppDestination.Home,
                                onClick = { navigateTo(AppDestination.Home) },
                                icon = { Icon(Icons.Filled.Home, "Home") },
                                label = { Text("Home") }
                            )
                            NavigationRailItem(
                                selected = currentDest is AppDestination.Shorts,
                                onClick = { navigateTo(AppDestination.Shorts()) },
                                icon = { Icon(Icons.Filled.Bolt, "Shorts") },
                                label = { Text("Shorts") }
                            )
                            NavigationRailItem(
                                selected = false,
                                onClick = { showCreateSheet = true },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(StudioCrimson),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Add, "Create", tint = Color.White)
                                    }
                                },
                                label = { Text("Create") }
                            )
                            NavigationRailItem(
                                selected = currentDest is AppDestination.Subscriptions,
                                onClick = { navigateTo(AppDestination.Subscriptions) },
                                icon = { Icon(Icons.Filled.Subscriptions, "Subscriptions") },
                                label = { Text("Subs") }
                            )
                            NavigationRailItem(
                                selected = currentDest is AppDestination.Profile,
                                onClick = { navigateTo(AppDestination.Profile) },
                                icon = { Icon(Icons.Filled.Person, "Profile") },
                                label = { Text("Profile") }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (val dest = currentDest) {
                            is AppDestination.Home -> HomeScreen(
                                viewModel = viewModel,
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) },
                                onOpenShort = { navigateTo(AppDestination.Shorts(it)) },
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) },
                                onOpenSearch = { voice -> navigateTo(AppDestination.Search(triggerVoice = voice)) },
                                onOpenNotifications = { navigateTo(AppDestination.Notifications) },
                                onOpenProfile = { navigateTo(AppDestination.Profile) }
                            )

                            is AppDestination.Shorts -> ShortsScreen(
                                viewModel = viewModel,
                                initialShortId = dest.initialShortId,
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) },
                                onOpenHashtagSearch = { tag -> navigateTo(AppDestination.Search(initialQuery = tag)) },
                                onCreateShort = { navigateTo(AppDestination.Upload(isShort = true)) }
                            )

                            is AppDestination.Subscriptions -> SubscriptionsScreen(
                                viewModel = viewModel,
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) },
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) }
                            )

                            is AppDestination.Profile -> ProfileScreen(
                                viewModel = viewModel,
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) },
                                onOpenStudio = { tab -> navigateTo(AppDestination.Studio(tab)) },
                                onOpenMonetization = { navigateTo(AppDestination.Monetization) },
                                onOpenNotifications = { navigateTo(AppDestination.Notifications) },
                                onOpenAdmin = { navigateTo(AppDestination.Admin) },
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) }
                            )

                            is AppDestination.Watch -> WatchScreen(
                                videoId = dest.videoId,
                                viewModel = viewModel,
                                onBack = { popBack() },
                                onWatchAnother = { nextId -> navigateTo(AppDestination.Watch(nextId)) },
                                onOpenChannel = { chId -> navigateTo(AppDestination.Channel(chId)) },
                                onOpenHashtagSearch = { tag -> navigateTo(AppDestination.Search(initialQuery = tag)) }
                            )

                            is AppDestination.Channel -> ChannelScreen(
                                channelId = dest.channelId,
                                viewModel = viewModel,
                                onBack = { popBack() },
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) },
                                onOpenShort = { navigateTo(AppDestination.Shorts(it)) },
                                onOpenStudio = { navigateTo(AppDestination.Studio(0)) }
                            )

                            is AppDestination.Search -> SearchScreen(
                                viewModel = viewModel,
                                initialQuery = dest.initialQuery,
                                triggerVoiceOnOpen = dest.triggerVoice,
                                onBack = { popBack() },
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) },
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) }
                            )

                            is AppDestination.Upload -> UploadAndCameraScreen(
                                viewModel = viewModel,
                                initialIsShort = dest.isShort,
                                onBack = { popBack() },
                                onOpenYouTubeConnect = { navigateTo(AppDestination.Studio(4)) },
                                onUploadFinished = {
                                    backStack = listOf(
                                        if (dest.isShort) AppDestination.Shorts() else AppDestination.Home
                                    )
                                }
                            )

                            is AppDestination.Studio -> CreatorStudioScreen(
                                viewModel = viewModel,
                                initialTabIndex = dest.initialTab,
                                onBack = { popBack() },
                                onOpenUpload = { navigateTo(AppDestination.Upload(false)) },
                                onOpenMonetization = { navigateTo(AppDestination.Monetization) },
                                onWatchVideo = { navigateTo(AppDestination.Watch(it)) }
                            )

                            is AppDestination.Monetization -> MonetizationScreen(
                                viewModel = viewModel,
                                onBack = { popBack() }
                            )

                            is AppDestination.Notifications -> NotificationsScreen(
                                viewModel = viewModel,
                                onBack = { popBack() },
                                onOpenVideo = { navigateTo(AppDestination.Watch(it)) },
                                onOpenChannel = { navigateTo(AppDestination.Channel(it)) }
                            )

                            is AppDestination.Admin -> AdminDashboardScreen(
                                viewModel = viewModel,
                                onBack = { popBack() }
                            )
                        }
                    }
                }
            }
        }

        if (showCreateSheet) {
            ModalBottomSheet(onDismissRequest = { showCreateSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Create on MY VIDEO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    CreateMenuRow(
                        icon = Icons.Outlined.CloudUpload,
                        title = "Upload a Video",
                        subtitle = "Pick single or multiple videos from phone gallery"
                    ) {
                        showCreateSheet = false
                        navigateTo(AppDestination.Upload(isShort = false))
                    }
                    CreateMenuRow(
                        icon = Icons.Outlined.Bolt,
                        title = "Create a Short",
                        subtitle = "Upload or record a vertical 9:16 Short"
                    ) {
                        showCreateSheet = false
                        navigateTo(AppDestination.Upload(isShort = true))
                    }
                    CreateMenuRow(
                        icon = Icons.Outlined.Dashboard,
                        title = "Open Creator Studio",
                        subtitle = "Analytics, monetization, and YouTube OAuth sync"
                    ) {
                        showCreateSheet = false
                        navigateTo(AppDestination.Studio(0))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

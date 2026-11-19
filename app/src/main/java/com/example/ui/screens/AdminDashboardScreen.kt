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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatCurrency
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

private enum class AdminTab(val label: String) {
    OVERVIEW("Ops & Fraud"),
    USERS("Users & Channels"),
    REPORTS("Videos & Reports"),
    PAYOUTS("Monetization & Pay"),
    ADS("Ads & Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MyVideoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val videos by viewModel.allVideosAdmin.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val payouts by viewModel.allPayouts.collectAsStateWithLifecycle()
    val adCampaigns by viewModel.adCampaigns.collectAsStateWithLifecycle()
    val settings by viewModel.platformSettings.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    if (currentUser?.role != "ADMIN") {
        EmptyStatePanel(
            icon = Icons.Outlined.AdminPanelSettings,
            title = "Admin Role Required",
            subtitle = "Sign in with an Administrator account to access platform moderation.",
            actionLabel = "Go Back",
            onAction = onBack
        )
        return
    }

    var activeTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var showNewAdDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("MY VIDEO Admin Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                AdminTab.entries.forEach { tab ->
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
                    AdminTab.OVERVIEW -> {
                        item {
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Platform Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Users: ${users.size} · Channels: ${channels.size} · Videos: ${videos.size}")
                                    Text("Open Reports: ${reports.count { it.status == "PENDING" }} · Pending Payouts: ${payouts.count { it.status != "PAID" }}")
                                    Button(
                                        onClick = { viewModel.runSpamScan() },
                                        modifier = Modifier.fillMaxWidth().testTag("run_spam_scan_btn")
                                    ) {
                                        Icon(Icons.Outlined.Security, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Run Spam & Fraudulent View Scan")
                                    }
                                }
                            }
                        }
                        val flaggedVideos = videos.filter { it.isFlaggedSpam }
                        if (flaggedVideos.isNotEmpty()) {
                            item {
                                Text("Flagged Spam / Suspicious Activity", style = MaterialTheme.typography.titleSmall)
                            }
                            items(flaggedVideos, key = { it.id }) { vid ->
                                Card(shape = RoundedCornerShape(12.dp)) {
                                    ListItem(
                                        headlineContent = { Text(vid.title) },
                                        supportingContent = { Text("Views: ${vid.viewsCount} · Watch min: ${"%.1f".format(vid.watchMinutesTotal)}") },
                                        trailingContent = {
                                            TextButton(onClick = { viewModel.adminModerateVideo(vid.id, approved = false, flagged = true) }) {
                                                Text("Suspend")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AdminTab.USERS -> {
                        item { Text("Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(users, key = { it.id }) { u ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                ListItem(
                                    headlineContent = { Text("${u.displayName} (${u.role})") },
                                    supportingContent = { Text(u.email) },
                                    trailingContent = {
                                        if (u.id != currentUser?.id) {
                                            OutlinedButton(onClick = { viewModel.adminSetUserSuspended(u.id, !u.isSuspended) }) {
                                                Text(if (u.isSuspended) "Restore" else "Suspend")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        item { Text("Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(channels, key = { it.id }) { ch ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                ListItem(
                                    headlineContent = { Text("${ch.name} (${ch.handle})") },
                                    supportingContent = { Text("${formatCompactCount(ch.subscriberCount)} subs · ${ch.monetizationTier}") },
                                    trailingContent = {
                                        OutlinedButton(onClick = { viewModel.adminSetChannelVerified(ch.id, !ch.isVerified) }) {
                                            Text(if (ch.isVerified) "Verified" else "Verify")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AdminTab.REPORTS -> {
                        item { Text("Reported Content Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(reports, key = { it.id }) { rep ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(rep.videoTitle, fontWeight = FontWeight.Bold)
                                    Text("Reason: ${rep.reason} · Status: ${rep.status}", style = MaterialTheme.typography.bodySmall)
                                    if (rep.status == "PENDING") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.adminModerateVideo(rep.videoId, approved = false, flagged = true, reportId = rep.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) { Text("Remove Video") }
                                            OutlinedButton(
                                                onClick = { viewModel.adminModerateVideo(rep.videoId, approved = true, flagged = false, reportId = rep.id) }
                                            ) { Text("Dismiss") }
                                        }
                                    }
                                }
                            }
                        }
                        item { Text("All Videos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(videos, key = { it.id }) { vid ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                ListItem(
                                    headlineContent = { Text(vid.title) },
                                    supportingContent = { Text("${vid.visibility} · Approved: ${vid.isApproved}") },
                                    trailingContent = {
                                        Switch(
                                            checked = vid.isApproved,
                                            onCheckedChange = { viewModel.adminModerateVideo(vid.id, it, vid.isFlaggedSpam) }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    AdminTab.PAYOUTS -> {
                        item { Text("Monetization Applications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        val applicants = channels.filter { it.monetizationTier.endsWith("APPLIED") }
                        if (applicants.isEmpty()) {
                            item { Text("No pending monetization applications.", style = MaterialTheme.typography.bodySmall) }
                        } else {
                            items(applicants, key = { it.id }) { ch ->
                                Card(shape = RoundedCornerShape(12.dp)) {
                                    ListItem(
                                        headlineContent = { Text(ch.name) },
                                        supportingContent = { Text("Requested: ${ch.monetizationTier} · ${ch.subscriberCount} subs") },
                                        trailingContent = {
                                            Button(onClick = {
                                                viewModel.adminApproveMonetization(ch.id, ch.monetizationTier.startsWith("FULL"))
                                            }) {
                                                Text("Approve")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        item { Text("Creator Payout Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(payouts, key = { it.id }) { pay ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                ListItem(
                                    headlineContent = { Text("${pay.channelName} · ${formatCurrency(pay.amountUsd)}") },
                                    supportingContent = { Text("${pay.periodLabel} · ${pay.method}") },
                                    trailingContent = {
                                        if (pay.status == "PAID") {
                                            Text("PAID", color = StudioEmerald, fontWeight = FontWeight.Bold)
                                        } else {
                                            Button(onClick = { viewModel.adminMarkPayoutPaid(pay.id) }) {
                                                Text("Mark Paid")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    AdminTab.ADS -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ad Campaigns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                FilledTonalButton(onClick = { showNewAdDialog = true }) {
                                    Text("New Ad")
                                }
                            }
                        }
                        items(adCampaigns, key = { it.id }) { ad ->
                            Card(shape = RoundedCornerShape(12.dp)) {
                                ListItem(
                                    headlineContent = { Text("${ad.advertiserName} — ${ad.title}") },
                                    supportingContent = { Text("CPM ${formatCurrency(ad.cpmUsd)} · Budget ${formatCurrency(ad.budgetUsd)}") },
                                    trailingContent = {
                                        Switch(
                                            checked = ad.isActive,
                                            onCheckedChange = { viewModel.adminToggleAdCampaign(ad.id, it) }
                                        )
                                    }
                                )
                            }
                        }
                        item {
                            var minPayoutText by remember(settings.minPayoutThresholdUsd) { mutableStateOf(settings.minPayoutThresholdUsd.toString()) }
                            var rpmText by remember(settings.defaultRpmUsd) { mutableStateOf(settings.defaultRpmUsd.toString()) }
                            Card(shape = RoundedCornerShape(14.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Platform Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = minPayoutText,
                                        onValueChange = { minPayoutText = it },
                                        label = { Text("Min Payout Threshold (USD)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = rpmText,
                                        onValueChange = { rpmText = it },
                                        label = { Text("Default Platform RPM (USD)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.adminSaveSettings(
                                                settings.copy(
                                                    minPayoutThresholdUsd = minPayoutText.toDoubleOrNull() ?: settings.minPayoutThresholdUsd,
                                                    defaultRpmUsd = rpmText.toDoubleOrNull() ?: settings.defaultRpmUsd
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Platform Settings")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewAdDialog) {
        var adv by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var cpm by remember { mutableStateOf("7.50") }
        var budget by remember { mutableStateOf("10000") }
        AlertDialog(
            onDismissRequest = { showNewAdDialog = false },
            title = { Text("Create Ad Campaign") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = adv, onValueChange = { adv = it }, label = { Text("Advertiser") })
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Title") })
                    OutlinedTextField(value = cpm, onValueChange = { cpm = it }, label = { Text("CPM ($)") })
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Budget ($)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adv.isNotBlank() && title.isNotBlank()) {
                        viewModel.adminCreateAdCampaign(adv, title, cpm.toDoubleOrNull() ?: 5.0, budget.toDoubleOrNull() ?: 5000.0)
                        showNewAdDialog = false
                    }
                }) { Text("Launch") }
            },
            dismissButton = {
                TextButton(onClick = { showNewAdDialog = false }) { Text("Cancel") }
            }
        )
    }
}

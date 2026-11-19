package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatCurrency
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.MyVideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetizationScreen(
    viewModel: MyVideoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val allVideos by viewModel.allVideosWithChannels.collectAsStateWithLifecycle()
    val payouts by viewModel.currentChannelPayouts.collectAsStateWithLifecycle()
    val settings by viewModel.platformSettings.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    val ch = channel
    if (ch == null) {
        EmptyStatePanel(
            icon = Icons.Outlined.MonetizationOn,
            title = "Sign In Required",
            subtitle = "Sign in to view MY VIDEO Creator Partner monetization & earnings."
        )
        return
    }

    val elig = remember(ch) { viewModel.repo.evaluateEligibility(ch) }
    val channelVideos = remember(allVideos, ch.id) {
        allVideos.filter { it.channel.id == ch.id }.map { it.video }
    }
    val threshold = settings.minPayoutThresholdUsd
    val monthlyEarnings = ch.estimatedBalanceUsd + payouts.take(1).sumOf { it.amountUsd }
    val dailyEarnings = (ch.estimatedBalanceUsd / 30.0).coerceAtLeast(0.0)

    var payoutMethodEdit by remember(ch.payoutMethod) { mutableStateOf(ch.payoutMethod) }
    var payoutAccountEdit by remember(ch.payoutAccountMasked) { mutableStateOf(ch.payoutAccountMasked) }
    var taxVerifiedEdit by remember(ch.taxVerified) { mutableStateOf(ch.taxVerified) }
    var idVerifiedEdit by remember(ch.identityVerified) { mutableStateOf(ch.identityVerified) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Monetization & Earnings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            // Independent MY VIDEO Payout System Notice
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.monetization_disclaimer),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Earnings Summary Dashboard
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Estimated Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatCurrency(ch.estimatedBalanceUsd),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = StudioEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(ch.monetizationTier.replace("_", " ")) }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Monthly Est.", style = MaterialTheme.typography.labelSmall)
                                    Text(formatCurrency(monthlyEarnings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Daily Avg", style = MaterialTheme.typography.labelSmall)
                                    Text(formatCurrency(dailyEarnings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Lifetime", style = MaterialTheme.typography.labelSmall)
                                    Text(formatCurrency(ch.lifetimeEarningsUsd), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Minimum Payout Threshold Progress
                        val thresholdProgress = (ch.estimatedBalanceUsd / threshold).toFloat().coerceIn(0f, 1f)
                        Text(
                            "Payout Threshold: ${formatCurrency(ch.estimatedBalanceUsd)} / ${formatCurrency(threshold)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        LinearProgressIndicator(
                            progress = { thresholdProgress },
                            color = StudioEmerald,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        )
                        Button(
                            onClick = { viewModel.requestPayout() },
                            enabled = ch.estimatedBalanceUsd >= threshold && ch.taxVerified && ch.identityVerified,
                            modifier = Modifier.fillMaxWidth().testTag("request_payout_btn")
                        ) {
                            Text("Request MY VIDEO Payout")
                        }
                    }
                }
            }

            // Tier 1: Early Monetization Eligibility (500 subs + 3,000 watch hrs OR 3M Shorts views)
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.StarOutline, null, tint = StudioAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tier 1: Early Monetization (Fan Funding)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Unlocks Channel Memberships, Super Chat & Creator Tips. Distinct from Full Advertising Revenue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EligibilityBar("Subscribers", "${elig.subscribers} / 500", (elig.subscribers / 500f).coerceIn(0f, 1f), StudioAmber)
                        EligibilityBar("Public Watch Hours (12M)", "${"%.0f".format(elig.watchHours12M)} / 3,000 hrs", (elig.watchHours12M / 3000.0).toFloat().coerceIn(0f, 1f), StudioAmber)
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        EligibilityBar("Public Shorts Views (90D)", "${formatCompactCount(elig.shortsViews90D)} / 3.0M", (elig.shortsViews90D / 3_000_000f).coerceIn(0f, 1f), StudioAmber)

                        Button(
                            onClick = { viewModel.applyForMonetization("EARLY_APPLIED") },
                            enabled = elig.isEarlyEligible && ch.monetizationTier == "NONE",
                            modifier = Modifier.fillMaxWidth().testTag("apply_early_monetization_btn")
                        ) {
                            Text(
                                when (ch.monetizationTier) {
                                    "EARLY_APPROVED", "FULL_APPROVED", "FULL_APPLIED" -> "Early Monetization Active"
                                    "EARLY_APPLIED" -> "Early Application Pending Review"
                                    else -> if (elig.isEarlyEligible) "Apply for Early Monetization" else "Reach 500 Subs + Watch/Shorts Goal"
                                }
                            )
                        }
                    }
                }
            }

            // Tier 2: Full Advertising Monetization Eligibility (1,000 subs + 4,000 watch hrs OR 10M Shorts views)
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, null, tint = StudioEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tier 2: Full Advertising Monetization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Unlocks Watch Page Ads (${settings.creatorRevenueSharePercent}% share) and Shorts Feed Ad Revenue (${settings.shortsRevenueSharePercent}% share).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EligibilityBar("Subscribers", "${elig.subscribers} / 1,000", (elig.subscribers / 1000f).coerceIn(0f, 1f), StudioEmerald)
                        EligibilityBar("Public Watch Hours (12M)", "${"%.0f".format(elig.watchHours12M)} / 4,000 hrs", (elig.watchHours12M / 4000.0).toFloat().coerceIn(0f, 1f), StudioEmerald)
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        EligibilityBar("Public Shorts Views (90D)", "${formatCompactCount(elig.shortsViews90D)} / 10.0M", (elig.shortsViews90D / 10_000_000f).coerceIn(0f, 1f), StudioEmerald)

                        Button(
                            onClick = { viewModel.applyForMonetization("FULL_APPLIED") },
                            enabled = elig.isFullEligible && ch.monetizationTier != "FULL_APPROVED" && ch.monetizationTier != "FULL_APPLIED",
                            modifier = Modifier.fillMaxWidth().testTag("apply_full_monetization_btn")
                        ) {
                            Text(
                                when (ch.monetizationTier) {
                                    "FULL_APPROVED" -> "Full Ad Monetization Active"
                                    "FULL_APPLIED" -> "Full Application Pending Review"
                                    else -> if (elig.isFullEligible) "Apply for Full Ad Monetization" else "Reach 1,000 Subs + Watch/Shorts Goal"
                                }
                            )
                        }
                    }
                }
            }

            // Video-Wise Advertising Revenue Breakdown
            item {
                Text("Video-Wise Advertising Revenue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(channelVideos, key = { it.id }) { vid ->
                ListItem(
                    headlineContent = { Text(vid.title, maxLines = 1) },
                    supportingContent = { Text("${formatCompactCount(vid.viewsCount)} views · CTR ${vid.ctrPercent}%") },
                    trailingContent = {
                        Text(formatCurrency(vid.adRevenueUsd), style = MaterialTheme.typography.titleSmall, color = StudioEmerald, fontWeight = FontWeight.Bold)
                    }
                )
            }

            // Payment Account & Tax/Identity Verification
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payment Account & Tax Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = payoutMethodEdit,
                            onValueChange = { payoutMethodEdit = it },
                            label = { Text("Payout Method (ACH / Wire / PayPal)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = payoutAccountEdit,
                            onValueChange = { payoutAccountEdit = it },
                            label = { Text("Account Last 4 / Handle") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = taxVerifiedEdit, onCheckedChange = { taxVerifiedEdit = it })
                            Text("W-9 / W-8BEN Tax Form Verified")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = idVerifiedEdit, onCheckedChange = { idVerifiedEdit = it })
                            Text("Government Identity Verified")
                        }
                        Button(
                            onClick = {
                                viewModel.updatePayoutAndTaxSettings(payoutMethodEdit, payoutAccountEdit, taxVerifiedEdit, idVerifiedEdit)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Payment & Tax Settings")
                        }
                    }
                }
            }

            // Monthly Payment Reports & Payout History
            item {
                Text("Payment History & Monthly Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (payouts.isEmpty()) {
                item {
                    Text("No payouts issued yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(payouts, key = { it.id }) { pay ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        ListItem(
                            headlineContent = { Text("${pay.periodLabel} · ${formatCurrency(pay.amountUsd)}") },
                            supportingContent = { Text(pay.method) },
                            trailingContent = {
                                Badge(containerColor = if (pay.status == "PAID") StudioEmerald else StudioAmber) {
                                    Text(pay.status, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EligibilityBar(label: String, valueLabel: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
        )
    }
}

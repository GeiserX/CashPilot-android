package com.cashpilot.android.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.EarningsPresentation
import com.cashpilot.android.model.PlatformEarnings
import com.cashpilot.android.ui.MainViewModel
import com.cashpilot.android.ui.component.EarningsCard
import com.cashpilot.android.util.FormatUtils
import kotlinx.coroutines.delay

private val RunningGreen = Color(0xFF22C55E)
// Amber, deliberately NOT the stopped red: "we cannot see this" is a different
// claim from "this is dead", and colouring them the same is what made a
// permission problem look like a fleet of dead apps.
private val UnknownAmber = Color(0xFFF59E0B)
private val StoppedRed = Color(0xFFEF4444)
private val DisabledGray = Color(0xFF9CA3AF)
private val NotInstalledGray = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val apps by viewModel.apps.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val lastHeartbeat by viewModel.lastHeartbeat.collectAsState()
    val lastHeartbeatFailed by viewModel.lastHeartbeatFailed.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val publicIp by viewModel.publicIp.collectAsState()
    val earnings by viewModel.earnings.collectAsState()
    val earningsAsOf by viewModel.earningsAsOf.collectAsState()
    val isBlind by viewModel.isBlind.collectAsState()
    // Recomputed on each refresh tick rather than read at composition time, so
    // the "may be out of date" badge appears while the screen is open instead of
    // only after the user navigates away and back.
    var nowMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    // Auto-refresh every 30s while visible
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMillis = System.currentTimeMillis()
            viewModel.refreshStatuses()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshStatuses() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // -- Summary header (full width) --
                item(span = { GridItemSpan(2) }) {
                    SummaryHeader(
                        summary = summary,
                        serverConfigured = settings.serverUrl.isNotBlank() && settings.apiKey.isNotBlank(),
                        lastHeartbeat = lastHeartbeat,
                        heartbeatFailed = lastHeartbeatFailed,
                        publicIp = publicIp,
                        onNavigateToSettings = onNavigateToSettings,
                    )
                }

                // -- Permissions --
                // While blind the banner is the wrong shape: it is dismissible,
                // and dismissing it leaves a screen full of cards that cannot
                // say anything true. The blocking card takes over instead.
                item(span = { GridItemSpan(2) }) {
                    if (isBlind) PermissionBlocker() else PermissionBanner(viewModel)
                }

                // -- Earnings (full width) --
                item(span = { GridItemSpan(2) }) {
                    EarningsCard(
                        earnings = earnings,
                        asOfMillis = earningsAsOf,
                        nowMillis = nowMillis,
                        serverConfigured = settings.serverUrl.isNotBlank() && settings.apiKey.isNotBlank(),
                    )
                }

                // -- App grid --
                // Every card would read "Can't tell", which is eleven copies of
                // one message. The blocking card above says it once, with the fix.
                // Earnings stay visible either way -- they come from the server
                // and are unaffected by what this device can see.
                if (!isBlind) {
                    items(apps, key = { it.app.slug }) { info ->
                        AppCard(info, earnings?.platforms?.firstOrNull { it.slug == info.app.slug })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    summary: com.cashpilot.android.ui.FleetSummary,
    serverConfigured: Boolean,
    lastHeartbeat: Long,
    heartbeatFailed: Boolean,
    publicIp: String?,
    onNavigateToSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status counts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Running
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_circle),
                        contentDescription = null,
                        tint = RunningGreen,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.summary_running, summary.running),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                // Stopped
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_circle),
                        contentDescription = null,
                        tint = StoppedRed,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.summary_stopped, summary.stopped),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                // Unknown -- only when there ARE any, because a permanent
                // "0 unknown" is noise. Shown BEFORE not-installed: an app we
                // cannot see is a live problem, one that is not installed is not.
                if (summary.unknown > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_circle),
                            contentDescription = null,
                            tint = UnknownAmber,
                            modifier = Modifier.size(10.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.summary_unknown, summary.unknown),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // Not installed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_circle),
                        contentDescription = null,
                        tint = NotInstalledGray,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.summary_na, summary.notInstalled),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bandwidth row
            if (summary.totalTx > 0 || summary.totalRx > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_upward),
                            contentDescription = stringResource(R.string.upload),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            formatBytes(summary.totalTx),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_downward),
                            contentDescription = stringResource(R.string.download),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            formatBytes(summary.totalRx),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.bandwidth_24h),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Public IP row
            if (publicIp != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_language),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "IP: $publicIp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Server / heartbeat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!serverConfigured) {
                    TextButton(onClick = onNavigateToSettings) {
                        Icon(
                            painterResource(R.drawable.ic_cloud_off),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.not_paired),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                heartbeatFailed -> StoppedRed
                                lastHeartbeat > 0 -> RunningGreen
                                else -> DisabledGray
                            },
                            label = "heartbeat-dot",
                        )
                        Icon(
                            painterResource(R.drawable.ic_circle),
                            contentDescription = null,
                            tint = dotColor,
                            modifier = Modifier.size(8.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                lastHeartbeat == 0L -> stringResource(R.string.no_heartbeat_yet)
                                else -> stringResource(R.string.last_heartbeat, relativeTime(lastHeartbeat))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shown INSTEAD of the app grid when no detection signal is available.
 *
 * Not a styling choice. With neither notification nor usage access every app
 * resolves to [AppState.UNKNOWN], so the grid would be eleven identical "Can't
 * tell" cards — the same sentence, repeated, with the actual fix nowhere on
 * screen. This says it once and offers the two buttons that resolve it.
 *
 * Deliberately NOT dismissible, unlike [PermissionBanner]. Dismissing it would
 * leave a screen that looks informative and knows nothing.
 */
@Composable
private fun PermissionBlocker() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_visibility_off),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.permissions_blocking_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permissions_blocking_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) {
                Icon(painterResource(R.drawable.ic_notifications), null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.grant_notification_access))
            }
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) {
                Icon(painterResource(R.drawable.ic_language), null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.grant_usage_access))
            }
        }
    }
}

@Composable
private fun PermissionBanner(viewModel: MainViewModel) {
    val hasNotif by viewModel.hasNotificationAccess.collectAsState()
    val hasUsage by viewModel.hasUsageAccess.collectAsState()
    var dismissed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset dismissal if permissions were revoked after user dismissed the banner
    LaunchedEffect(hasNotif, hasUsage) {
        if (!hasNotif || !hasUsage) dismissed = false
    }

    if (dismissed || (hasNotif && hasUsage)) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_warning),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.permissions_needed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                if (!hasNotif) {
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_notifications), null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.grant_notification_access), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!hasUsage) {
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_visibility_off), null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.grant_usage_access), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = { dismissed = true }) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.dismiss),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AppCard(info: AppDisplayInfo, earnings: PlatformEarnings? = null) {
    val context = LocalContext.current
    val borderColor = when (info.state) {
        AppState.RUNNING -> RunningGreen
        AppState.STOPPED -> StoppedRed
        AppState.UNKNOWN -> UnknownAmber
        AppState.DISABLED -> DisabledGray
        AppState.NOT_INSTALLED -> Color.Transparent
    }
    val cardAlpha = when (info.state) {
        AppState.NOT_INSTALLED -> 0.5f
        AppState.DISABLED -> 0.65f
        else -> 1f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .then(
                if (info.state == AppState.NOT_INSTALLED) {
                    Modifier.clickable {
                        openAppInstall(context, info.app)
                    }
                } else {
                    Modifier
                },
            ),
        border = if (info.state != AppState.NOT_INSTALLED) {
            BorderStroke(1.5.dp, borderColor)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Status dot + app name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_circle),
                    contentDescription = null,
                    tint = when (info.state) {
                        AppState.RUNNING -> RunningGreen
                        AppState.STOPPED -> StoppedRed
                        AppState.UNKNOWN -> UnknownAmber
                        AppState.DISABLED -> DisabledGray
                        AppState.NOT_INSTALLED -> NotInstalledGray
                    },
                    modifier = Modifier.size(10.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    info.app.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (info.state == AppState.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Per-app earnings. Only for apps that are actually on this
            // device: a figure beside an app the user has not installed would
            // be noise, and beside a DISABLED one it would be stale by design.
            if (EarningsPresentation.showsPerAppEarnings(info.state)) {
                Spacer(Modifier.height(4.dp))
                Text(
                    // An em-dash for null, never "$0.00". Nothing has been read,
                    // which is not the same as having earned nothing.
                    FormatUtils.formatPlatformEarnings(earnings?.usd),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (earnings?.sharedWithOtherWorkers == true) {
                    Text(
                        stringResource(R.string.earnings_shared),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The label alone says the figure is shared; it does not say
                    // what that means for the number directly above it. Without
                    // this line a user reasonably reads it as what THIS phone
                    // earned, which is the misreading the label exists to prevent.
                    // Follows the earnings_none_yet / _detail pair in EarningsCard.
                    Text(
                        stringResource(R.string.earnings_shared_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // State label
            Text(
                when (info.state) {
                    AppState.RUNNING -> stringResource(R.string.state_running)
                    AppState.STOPPED -> info.status?.lastActive?.let { stringResource(R.string.state_last_active, relativeTime(parseIso(it))) } ?: stringResource(R.string.state_stopped)
                    AppState.UNKNOWN -> stringResource(R.string.state_unknown)
                    AppState.DISABLED -> stringResource(R.string.state_disabled)
                    AppState.NOT_INSTALLED -> stringResource(R.string.state_not_installed)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Notification indicator for running apps
            if (info.state == AppState.RUNNING && info.status?.notificationActive == true) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_notifications),
                        contentDescription = stringResource(R.string.notification_active),
                        modifier = Modifier.size(12.dp),
                        tint = RunningGreen.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        stringResource(R.string.notification_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // Bandwidth for apps with data
            val tx = info.status?.netTx24h ?: 0
            val rx = info.status?.netRx24h ?: 0
            if (tx > 0 || rx > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_upward),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        formatBytes(tx),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painterResource(R.drawable.ic_arrow_downward),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        formatBytes(rx),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = FormatUtils.formatBytes(bytes)

private fun relativeTime(millis: Long): String {
    if (millis == 0L) return "never"
    return DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

private fun parseIso(iso: String): Long = FormatUtils.parseIso(iso)

private fun openAppInstall(context: Context, app: MonitoredApp) {
    // Try referral URL first, fall back to Play Store on any failure
    val referral = app.referralUrl
    if (referral != null) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(referral))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        } catch (_: Exception) { /* fall through to Play Store */ }
    }
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}")),
        )
    } catch (_: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")),
        )
    }
}

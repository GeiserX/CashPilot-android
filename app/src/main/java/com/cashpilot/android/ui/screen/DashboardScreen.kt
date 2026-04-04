package com.cashpilot.android.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.MainViewModel
import kotlinx.coroutines.delay

private val RunningGreen = Color(0xFF22C55E)
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

    // Auto-refresh every 30s while visible
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            viewModel.refreshStatuses()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
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

                // -- Permission banner (full width) --
                item(span = { GridItemSpan(2) }) {
                    PermissionBanner(viewModel)
                }

                // -- App grid --
                items(apps, key = { it.app.slug }) { info ->
                    AppCard(info)
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
                        Icons.Default.Circle,
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
                        Icons.Default.Circle,
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
                // Not installed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Circle,
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
                            Icons.Default.ArrowUpward,
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
                            Icons.Default.ArrowDownward,
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
                        Icons.Default.Language,
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
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.not_connected),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
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
                            Icons.Default.Circle,
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
                Icons.Default.Warning,
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
                        Icon(Icons.Default.Notifications, null, Modifier.size(14.dp))
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
                        Icon(Icons.Default.VisibilityOff, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.grant_usage_access), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = { dismissed = true }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AppCard(info: AppDisplayInfo) {
    val context = LocalContext.current
    val borderColor = when (info.state) {
        AppState.RUNNING -> RunningGreen
        AppState.STOPPED -> StoppedRed
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
                        // Use referral URL if available (opens browser with referral tracking)
                        val url = info.app.referralUrl
                        if (url != null) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        } else {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${info.app.packageName}"),
                            )
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${info.app.packageName}"),
                                    ),
                                )
                            }
                        }
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
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = when (info.state) {
                        AppState.RUNNING -> RunningGreen
                        AppState.STOPPED -> StoppedRed
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

            Spacer(Modifier.height(4.dp))

            // State label
            Text(
                when (info.state) {
                    AppState.RUNNING -> stringResource(R.string.state_running)
                    AppState.STOPPED -> info.status?.lastActive?.let { stringResource(R.string.state_last_active, relativeTime(parseIso(it))) } ?: stringResource(R.string.state_stopped)
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
                        Icons.Default.Notifications,
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
                        Icons.Default.ArrowUpward,
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
                        Icons.Default.ArrowDownward,
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

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private fun relativeTime(millis: Long): String {
    if (millis == 0L) return "never"
    return DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

private fun parseIso(iso: String): Long =
    try {
        java.time.Instant.parse(iso).toEpochMilli()
    } catch (_: Exception) {
        0L
    }

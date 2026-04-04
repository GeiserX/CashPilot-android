package com.cashpilot.android.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.ui.MainViewModel

@Composable
fun SetupScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val hasNotif by viewModel.hasNotificationAccess.collectAsState()
    val hasUsage by viewModel.hasUsageAccess.collectAsState()
    val hasBattery by viewModel.hasBatteryOptOut.collectAsState()
    val context = LocalContext.current

    var localUrl by rememberSaveable { mutableStateOf("") }
    var localKey by rememberSaveable { mutableStateOf("") }

    // Sync each field independently so a partial DataStore write doesn't clobber the other
    var urlSynced by rememberSaveable { mutableStateOf(false) }
    var keySynced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.serverUrl) {
        if (!urlSynced && settings.serverUrl.isNotEmpty()) {
            localUrl = settings.serverUrl
            urlSynced = true
        }
    }
    LaunchedEffect(settings.apiKey) {
        if (!keySynced && settings.apiKey.isNotEmpty()) {
            localKey = settings.apiKey
            keySynced = true
        }
    }

    val serverDone = localUrl.isNotBlank() && localKey.isNotBlank()

    val finishSetup = {
        viewModel.updateSettings { it.copy(serverUrl = localUrl, apiKey = localKey, setupCompleted = true) }
        onComplete()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // Step 1: Server connection
            SetupCard(
                step = 1,
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.setup_server_title),
                description = stringResource(R.string.setup_server_desc),
                done = serverDone,
            ) {
                OutlinedTextField(
                    value = localUrl,
                    onValueChange = { localUrl = it; viewModel.updateServerUrl(it) },
                    label = { Text(stringResource(R.string.setup_server_url_label)) },
                    placeholder = { Text("https://cashpilot.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = localUrl.trim().startsWith("http://", ignoreCase = true),
                    supportingText = if (localUrl.trim().startsWith("http://", ignoreCase = true)) {
                        { Text(stringResource(R.string.cleartext_warning)) }
                    } else null,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localKey,
                    onValueChange = { localKey = it; viewModel.updateApiKey(it) },
                    label = { Text(stringResource(R.string.setup_api_key_label)) },
                    placeholder = { Text("CASHPILOT_API_KEY") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Step 2: Notification access
            SetupCard(
                step = 2,
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.setup_notif_title),
                description = stringResource(R.string.setup_notif_desc),
                done = hasNotif,
            ) {
                TextButton(
                    onClick = { openNotificationListenerSettings(context) },
                ) {
                    Text(stringResource(R.string.setup_grant_access))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }

            // Step 3: Usage access
            SetupCard(
                step = 3,
                icon = Icons.Default.QueryStats,
                title = stringResource(R.string.setup_usage_title),
                description = stringResource(R.string.setup_usage_desc),
                done = hasUsage,
            ) {
                TextButton(
                    onClick = { openUsageAccessSettings(context) },
                ) {
                    Text(stringResource(R.string.setup_grant_access))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }

            // Step 4: Battery optimization
            SetupCard(
                step = 4,
                icon = Icons.Default.BatteryAlert,
                title = stringResource(R.string.setup_battery_title),
                description = stringResource(R.string.setup_battery_desc),
                done = hasBattery,
            ) {
                TextButton(
                    onClick = { openBatteryOptimizationSettings(context) },
                ) {
                    Text(stringResource(R.string.setup_grant_access))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = finishSetup,
                modifier = Modifier.fillMaxWidth(),
                enabled = serverDone && hasNotif && hasUsage,
            ) {
                Text(stringResource(R.string.setup_continue))
            }

            TextButton(
                onClick = finishSetup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.setup_skip))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SetupCard(
    step: Int,
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.setup_step, step, title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun openNotificationListenerSettings(context: Context) {
    context.startActivity(
        Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openUsageAccessSettings(context: Context) {
    context.startActivity(
        Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        // Direct prompt for this app specifically
        context.startActivity(
            Intent(
                AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        // Fallback to global list
        context.startActivity(
            Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

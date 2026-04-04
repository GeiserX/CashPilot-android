package com.cashpilot.android.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Local state for text fields — avoids per-keystroke DataStore writes
    var localUrl by rememberSaveable { mutableStateOf("") }
    var localKey by rememberSaveable { mutableStateOf("") }

    // Sync once when DataStore loads real values (won't re-trigger after)
    var synced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.serverUrl, settings.apiKey) {
        if (!synced && (settings.serverUrl.isNotEmpty() || settings.apiKey.isNotEmpty())) {
            localUrl = settings.serverUrl
            localKey = settings.apiKey
            synced = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Server Connection", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = localUrl,
                    onValueChange = { url ->
                        localUrl = url
                        viewModel.updateServerUrl(url)
                    },
                    label = { Text("CashPilot Server URL") },
                    placeholder = { Text("https://cashpilot.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = localKey,
                    onValueChange = { key ->
                        localKey = key
                        viewModel.updateApiKey(key)
                    },
                    label = { Text("Fleet API Key") },
                    placeholder = { Text("Paste CASHPILOT_API_KEY from your server") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Permissions section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Grant these permissions for full app detection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { openNotificationListenerSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Notification Access")
                    }
                    OutlinedButton(
                        onClick = { openUsageAccessSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Usage Access")
                    }
                    OutlinedButton(
                        onClick = { openBatteryOptimizationSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Disable Battery Optimization")
                    }
                }
            }

            // Monitored apps section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Monitored Apps", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Toggle which apps to monitor. Only installed apps will be reported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(KnownApps.all) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = app.slug in settings.enabledSlugs,
                        onCheckedChange = { viewModel.toggleApp(app.slug) },
                    )
                }
            }
        }
    }
}

private fun openNotificationListenerSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openUsageAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun openBatteryOptimizationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

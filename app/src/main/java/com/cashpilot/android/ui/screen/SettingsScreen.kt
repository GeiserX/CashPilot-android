package com.cashpilot.android.ui.screen

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

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
                    value = settings.serverUrl,
                    onValueChange = { url ->
                        viewModel.updateSettings { it.copy(serverUrl = url) }
                    },
                    label = { Text("CashPilot Server URL") },
                    placeholder = { Text("https://cashpilot.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = settings.joinToken,
                    onValueChange = { token ->
                        viewModel.updateSettings { it.copy(joinToken = token) }
                    },
                    label = { Text("Join Token") },
                    placeholder = { Text("Paste token from CashPilot Fleet page") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

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

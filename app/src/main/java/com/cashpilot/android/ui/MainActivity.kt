package com.cashpilot.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import com.cashpilot.android.service.HeartbeatService
import com.cashpilot.android.ui.screen.DashboardScreen
import com.cashpilot.android.ui.screen.SettingsScreen
import com.cashpilot.android.ui.screen.SetupScreen
import com.cashpilot.android.ui.theme.CashPilotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* HeartbeatService notification becomes visible once granted */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ so the heartbeat notification is visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Start heartbeat service
        val serviceIntent = Intent(this, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Refresh statuses every time the activity resumes (e.g. returning from permission screens)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshStatuses()
            }
        }

        setContent {
            CashPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val settings by viewModel.settings.collectAsState()
                    val hasNotif by viewModel.hasNotificationAccess.collectAsState()
                    val hasUsage by viewModel.hasUsageAccess.collectAsState()
                    val hasBattery by viewModel.hasBatteryOptOut.collectAsState()
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    var setupDismissed by rememberSaveable { mutableStateOf(false) }

                    val needsSetup = !setupDismissed &&
                        (settings.serverUrl.isBlank() || settings.apiKey.isBlank() || !hasNotif || !hasUsage || !hasBattery)

                    // Handle system Back from Settings → return to Dashboard
                    BackHandler(enabled = showSettings && !needsSetup) {
                        showSettings = false
                    }

                    when {
                        needsSetup -> SetupScreen(
                            viewModel = viewModel,
                            onComplete = { setupDismissed = true },
                        )
                        showSettings -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { showSettings = false },
                        )
                        else -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }
}

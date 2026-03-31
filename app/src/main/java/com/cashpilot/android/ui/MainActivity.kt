package com.cashpilot.android.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cashpilot.android.service.HeartbeatService
import com.cashpilot.android.ui.screen.DashboardScreen
import com.cashpilot.android.ui.screen.SettingsScreen
import com.cashpilot.android.ui.theme.CashPilotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start heartbeat service
        val serviceIntent = Intent(this, HeartbeatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Initial scan
        viewModel.refreshStatuses()

        setContent {
            CashPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSettings by rememberSaveable { mutableStateOf(false) }

                    if (showSettings) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { showSettings = false },
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { showSettings = true },
                        )
                    }
                }
            }
        }
    }

    /** Opens system Notification Listener settings. */
    fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /** Opens system Usage Access settings. */
    fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

package com.cashpilot.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.cashpilot.android.service.HeartbeatWorker
import com.cashpilot.android.ui.screen.DashboardScreen
import com.cashpilot.android.ui.screen.SettingsScreen
import com.cashpilot.android.ui.screen.SetupScreen
import com.cashpilot.android.ui.theme.CashPilotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Opening the app reports once now; the 15-minute schedule does the rest.
        HeartbeatWorker.runNow(this)

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
                    var showSettings by rememberSaveable { mutableStateOf(false) }

                    // Handle system Back from Settings → return to Dashboard
                    BackHandler(enabled = showSettings && settings.setupCompleted) {
                        showSettings = false
                    }

                    when {
                        !settings.setupCompleted -> SetupScreen(
                            viewModel = viewModel,
                            onComplete = {},
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

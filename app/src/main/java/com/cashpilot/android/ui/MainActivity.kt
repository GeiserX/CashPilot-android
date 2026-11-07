package com.cashpilot.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cashpilot.android.service.HeartbeatWorker
import com.cashpilot.android.ui.board.BoardApp
import com.cashpilot.android.ui.theme.CashPilotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        // Opening or returning to the app reports once now; the 15-minute
        // schedule does the rest. onStart, not onCreate, so coming back to an
        // activity that is still alive counts as opening it.
        HeartbeatWorker.runNow(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Refresh statuses every time the activity resumes (e.g. returning from permission screens)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshStatuses()
            }
        }

        setContent {
            CashPilotTheme {
                BoardApp(viewModel)
            }
        }
    }
}

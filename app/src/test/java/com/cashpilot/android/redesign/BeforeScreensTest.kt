package com.cashpilot.android.redesign

import android.app.Application
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import com.cashpilot.android.model.Settings
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.MainViewModel
import com.cashpilot.android.ui.screen.DashboardScreen
import com.cashpilot.android.ui.screen.SettingsScreen
import com.cashpilot.android.ui.screen.SetupScreen
import com.cashpilot.android.ui.theme.CashPilotTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The CURRENT app's screens, rendered from the shipping composables.
 *
 * The real MainViewModel is built, then its flows are swapped for fixed ones
 * so each screen shows a known situation (Fixtures). Nothing about the screens
 * themselves is stubbed. Off unless CASHPILOT_RENDER_REDESIGN is set, so CI's
 * verifyRoborazziDebug never sees these images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class BeforeScreensTest {

    private fun vmFor(s: Fixtures.Scenario): MainViewModel {
        val vm = MainViewModel(ApplicationProvider.getApplicationContext<Application>())
        // Each public flow is a separate field from its private MutableStateFlow
        // (asStateFlow() is a read-only copy), so both are replaced.
        fun put(field: String, value: Any?) {
            val flow = MutableStateFlow(value)
            listOf(field, field.removePrefix("_")).distinct().forEach { name ->
                val f = MainViewModel::class.java.getDeclaredField(name)
                f.isAccessible = true
                f.set(vm, flow)
            }
        }
        put("settings", Settings(serverUrl = s.serverUrl, apiKey = if (s.serverConfigured) "k" else "", setupCompleted = true))
        put("_apps", s.apps)
        put("_summary", AppPresentation.summarise(s.apps))
        put("_isRefreshing", false)
        put("_hasNotificationAccess", s.notif)
        put("_hasUsageAccess", s.usage)
        put("_hasBatteryOptOut", s.battery)
        put("isBlind", s.blind)
        put("lastHeartbeat", s.lastHeartbeat)
        put("lastHeartbeatFailed", s.heartbeatFailed)
        put("earnings", s.earnings)
        put("earningsAsOf", s.earningsAsOf)
        put("_publicIp", if (s.serverConfigured) "203.0.113.24" else null)
        return vm
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        renderGate()
        captureRoboImage("$OUT/before/$name.png") {
            CashPilotTheme(darkTheme = true) { Surface { content() } }
        }
    }

    @Test
    fun current() {
        Fixtures.all().forEach { (name, s) ->
            val vm = vmFor(s)
            shot("dashboard_$name") { DashboardScreen(vm, onNavigateToSettings = {}) }
        }
        val first = vmFor(Fixtures.firstRun)
        shot("setup") { SetupScreen(first, onComplete = {}) }
        shot("settings") { SettingsScreen(vmFor(Fixtures.healthy), onBack = {}) }
    }
}

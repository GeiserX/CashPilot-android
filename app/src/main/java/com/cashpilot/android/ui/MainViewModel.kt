package com.cashpilot.android.ui

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings as SystemSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.model.Settings
import com.cashpilot.android.service.AppDetector
import com.cashpilot.android.service.AppNotificationListener
import com.cashpilot.android.service.HeartbeatService
import com.cashpilot.android.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppState { RUNNING, STOPPED, NOT_INSTALLED, DISABLED }

data class AppDisplayInfo(
    val app: MonitoredApp,
    val state: AppState,
    val status: AppStatus? = null,
)

data class FleetSummary(
    val running: Int = 0,
    val stopped: Int = 0,
    val notInstalled: Int = 0,
    val disabled: Int = 0,
    val totalTx: Long = 0,
    val totalRx: Long = 0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = AppDetector(application)

    val settings: StateFlow<Settings> = SettingsStore.settings(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _apps = MutableStateFlow<List<AppDisplayInfo>>(emptyList())
    val apps: StateFlow<List<AppDisplayInfo>> = _apps.asStateFlow()

    private val _summary = MutableStateFlow(FleetSummary())
    val summary: StateFlow<FleetSummary> = _summary.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(false)
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    val lastHeartbeat: StateFlow<Long> = HeartbeatService.lastHeartbeat
    val lastHeartbeatFailed: StateFlow<Boolean> = HeartbeatService.lastHeartbeatFailed

    fun refreshStatuses() {
        viewModelScope.launch { doRefresh() }
    }

    fun toggleApp(slug: String) {
        viewModelScope.launch {
            SettingsStore.update(getApplication()) { s ->
                val new = s.enabledSlugs.toMutableSet()
                if (slug in new) new.remove(slug) else new.add(slug)
                s.copy(enabledSlugs = new)
            }
            // withContext(IO) yields to main, letting the settings StateFlow update
            doRefresh()
        }
    }

    private suspend fun doRefresh() {
        _isRefreshing.value = true
        withContext(Dispatchers.IO) {
            val enabled = settings.value.enabledSlugs
            val detected = detector.detectAll(enabled).associateBy { it.slug }

            val displayList = KnownApps.all.map { app ->
                val installed = detector.isInstalled(app.packageName)
                val isEnabled = app.slug in enabled
                val status = detected[app.slug]

                val state = when {
                    !installed -> AppState.NOT_INSTALLED
                    !isEnabled -> AppState.DISABLED
                    status?.running == true -> AppState.RUNNING
                    else -> AppState.STOPPED
                }

                AppDisplayInfo(app = app, state = state, status = status)
            }

            val sorted = displayList.sortedBy { it.state.ordinal }

            _apps.value = sorted
            _summary.value = FleetSummary(
                running = sorted.count { it.state == AppState.RUNNING },
                stopped = sorted.count { it.state == AppState.STOPPED },
                notInstalled = sorted.count { it.state == AppState.NOT_INSTALLED },
                disabled = sorted.count { it.state == AppState.DISABLED },
                totalTx = sorted.mapNotNull { it.status?.netTx24h }.sum(),
                totalRx = sorted.mapNotNull { it.status?.netRx24h }.sum(),
            )
        }
        checkPermissions()
        _isRefreshing.value = false
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            SettingsStore.update(getApplication(), transform)
        }
    }

    private fun checkPermissions() {
        val ctx = getApplication<Application>()

        val flat = SystemSettings.Secure.getString(
            ctx.contentResolver,
            "enabled_notification_listeners",
        ) ?: ""
        val myComponent = ComponentName(ctx, AppNotificationListener::class.java).flattenToString()
        _hasNotificationAccess.value = myComponent in flat

        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        _hasUsageAccess.value = if (usm != null) {
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 60_000,
                now,
            )
            stats != null && stats.isNotEmpty()
        } else {
            false
        }
    }
}

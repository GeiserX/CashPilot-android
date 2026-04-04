package com.cashpilot.android.ui

import android.app.AppOpsManager
import android.app.Application
import android.os.PowerManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Process
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.coroutines.coroutineContext

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

    companion object {
        private const val PUBLIC_IP_URL = "https://api.ipify.org"
    }

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

    private val _hasBatteryOptOut = MutableStateFlow(false)
    val hasBatteryOptOut: StateFlow<Boolean> = _hasBatteryOptOut.asStateFlow()

    val lastHeartbeat: StateFlow<Long> = HeartbeatService.lastHeartbeat
    val lastHeartbeatFailed: StateFlow<Boolean> = HeartbeatService.lastHeartbeatFailed

    private val _publicIp = MutableStateFlow<String?>(null)
    val publicIp: StateFlow<String?> = _publicIp.asStateFlow()
    private var publicIpFailed = false

    private var refreshJob: Job? = null

    init {
        checkPermissions()
    }

    fun refreshStatuses() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { doRefresh() }
    }

    fun toggleApp(slug: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            // Toggle inside the DataStore transaction to avoid TOCTOU races
            var newSlugs = emptySet<String>()
            SettingsStore.update(getApplication()) { s ->
                val updated = s.enabledSlugs.toMutableSet()
                if (slug in updated) updated.remove(slug) else updated.add(slug)
                newSlugs = updated.toSet()
                s.copy(enabledSlugs = newSlugs)
            }
            doRefresh(enabledOverride = newSlugs)
        }
    }

    private suspend fun doRefresh(enabledOverride: Set<String>? = null) {
        _isRefreshing.value = true
        val result = withContext(Dispatchers.IO) {
            val enabled = enabledOverride ?: settings.value.enabledSlugs
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

            displayList.sortedBy { it.state.ordinal }
        }
        // If this job was cancelled while IO work ran, don't write stale results
        coroutineContext.ensureActive()
        _apps.value = result
        _summary.value = FleetSummary(
            running = result.count { it.state == AppState.RUNNING },
            stopped = result.count { it.state == AppState.STOPPED },
            notInstalled = result.count { it.state == AppState.NOT_INSTALLED },
            disabled = result.count { it.state == AppState.DISABLED },
            totalTx = result.mapNotNull { it.status?.netTx24h }.sum(),
            totalRx = result.mapNotNull { it.status?.netRx24h }.sum(),
        )
        checkPermissions()
        // Only fetch public IP when fully configured, and don't retry on failure
        val serverReady = settings.value.serverUrl.isNotBlank() && settings.value.apiKey.isNotBlank()
        if (serverReady && _publicIp.value == null && !publicIpFailed) {
            fetchPublicIp()
        } else if (!serverReady) {
            _publicIp.value = null
            publicIpFailed = false
        }
        _isRefreshing.value = false
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            SettingsStore.update(getApplication(), transform)
        }
    }

    private var serverUrlJob: Job? = null
    private var apiKeyJob: Job? = null

    fun updateServerUrl(url: String) {
        serverUrlJob?.cancel()
        serverUrlJob = viewModelScope.launch {
            delay(500)
            SettingsStore.update(getApplication()) { it.copy(serverUrl = url) }
        }
    }

    fun updateApiKey(key: String) {
        apiKeyJob?.cancel()
        apiKeyJob = viewModelScope.launch {
            delay(500)
            SettingsStore.update(getApplication()) { it.copy(apiKey = key) }
        }
    }

    private fun fetchPublicIp() {
        viewModelScope.launch {
            val ip = withContext(Dispatchers.IO) {
                try {
                    URL(PUBLIC_IP_URL).readText().trim()
                } catch (_: Exception) {
                    null
                }
            }
            if (ip != null) {
                _publicIp.value = ip
            } else {
                publicIpFailed = true
            }
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

        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        _hasUsageAccess.value = if (appOps != null) {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    ctx.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            false
        }

        val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _hasBatteryOptOut.value = pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
    }
}

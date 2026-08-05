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
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.model.Settings
import com.cashpilot.android.service.AppDetector
import com.cashpilot.android.service.Detection
import com.cashpilot.android.service.AppNotificationListener
import com.cashpilot.android.service.HeartbeatService
import com.cashpilot.android.util.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * What the dashboard can say about an app.
 *
 * [UNKNOWN] is deliberately NOT a flavour of [STOPPED]. They have different
 * causes and different fixes: STOPPED means the app should be reporting and is
 * not, so restart it; UNKNOWN means this device cannot see, so grant the
 * permission. Collapsing them is what made a permission problem look like
 * eleven dead apps.
 */
enum class AppState { STOPPED, UNKNOWN, RUNNING, NOT_INSTALLED, DISABLED }

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
    /**
     * Apps whose state could not be determined.
     *
     * Without this the counts silently swallow them: every app reads UNKNOWN
     * when the permissions are denied, so running and stopped are both 0 and the
     * header states "0 running" -- which reads as "nothing is earning". Same
     * false claim the per-app cards were just fixed for, one summary line up.
     *
     * Declared LAST on purpose, matching the convention SystemInfo already
     * documents: DataClassContractTest destructures this class positionally, so
     * member order is part of its contract and inserting anywhere else silently
     * changes what component3() means. Adding it third broke that test, which is
     * the convention doing its job.
     */
    val unknown: Int = 0,
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

    /**
     * True when NO detection signal is available on this device.
     *
     * Not a nicety: while blind, every app reads UNKNOWN, so the app grid can
     * say nothing true and the permission prompt is the only honest content the
     * screen has. Earnings are unaffected -- they come from the server.
     */
    val isBlind: StateFlow<Boolean> = combine(hasNotificationAccess, hasUsageAccess) { notif, usage ->
        Detection.isBlind(canSeeNotifications = notif, canSeeUsage = usage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastHeartbeat: StateFlow<Long> = HeartbeatService.lastHeartbeat
    val lastHeartbeatFailed: StateFlow<Boolean> = HeartbeatService.lastHeartbeatFailed

    /**
     * What the server says these platforms earned.
     *
     * The service has carried this since the earnings work landed, and nothing
     * read it -- the phone asked, the server answered, the reply was parsed,
     * stored and kept across offline blips, and then no screen showed it. That
     * was the user's original complaint about this app.
     *
     * Null is UNKNOWN, never zero. [earningsAsOf] is exposed alongside so the
     * dashboard can say a figure is stale instead of presenting it as current;
     * a phone is offline often, so the last known value is kept deliberately.
     */
    val earnings: StateFlow<Earnings?> = HeartbeatService.earnings
    val earningsAsOf: StateFlow<Long> = HeartbeatService.earningsAsOf

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

                val state = AppPresentation.resolveState(
                    installed = installed,
                    enabled = isEnabled,
                    running = status?.running,
                )

                AppDisplayInfo(app = app, state = state, status = status)
            }

            // Problems first. This used to sort by AppState.ordinal, which is
            // the enum's declaration order -- RUNNING first -- so the apps that
            // were fine took the top of the screen and anything STOPPED was
            // pushed below them.
            AppPresentation.sortForDashboard(displayList)
        }
        // If this job was cancelled while IO work ran, don't write stale results
        coroutineContext.ensureActive()
        _apps.value = result
        _summary.value = AppPresentation.summarise(result)
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

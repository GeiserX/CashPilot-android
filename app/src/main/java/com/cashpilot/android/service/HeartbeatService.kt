package com.cashpilot.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings.Secure
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cashpilot.android.R
import com.cashpilot.android.BuildConfig
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.util.SettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HeartbeatService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private lateinit var detector: AppDetector

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }
    private var consecutiveFailures = 0

    override fun onCreate() {
        super.onCreate()
        detector = AppDetector(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring apps..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                val settings = SettingsStore.settings(applicationContext).first()
                if (settings.serverUrl.isNotBlank() && settings.activeKey.isNotBlank()) {
                    sendHeartbeat(settings)
                }
                // Exponential backoff on consecutive failures (30s → 60s → 120s, max 5min)
                val baseDelay = (settings.heartbeatIntervalSeconds * 1000L).coerceAtLeast(5_000L)
                val backoff = if (consecutiveFailures > 0) {
                    (baseDelay * (1L shl consecutiveFailures.coerceAtMost(3)))
                        .coerceAtMost(300_000L)
                } else {
                    baseDelay
                }
                delay(backoff)
            }
        }
        return START_STICKY
    }

    private suspend fun sendHeartbeat(settings: Settings) {
        try {
            val apps = detector.detectAll(settings.enabledSlugs)

            // Send apps in both formats for backward compatibility:
            // - `apps` (new): rich app data for servers that understand Android workers
            // - `containers` (legacy): simplified format so older servers still show the worker
            val containers = apps.map { app ->
                AppContainer(
                    slug = app.slug,
                    name = "cashpilot-${app.slug}",
                    // Three-valued on the wire too. `unknown` is NOT "stopped":
                    // the server maps a falsy running to "stopped", so sending
                    // false while blind would hand the fleet page the same false
                    // claim this fix removes from the phone.
                    status = Detection.wireStatus(app.running),
                    labels = mapOf(
                        "cashpilot.managed" to "true",
                        "cashpilot.service" to app.slug,
                    ),
                )
            }

            val heartbeat = WorkerHeartbeat(
                name = "${Build.MANUFACTURER} ${Build.MODEL} (${deviceId()})",
                containers = containers,
                apps = apps,
                systemInfo = SystemInfo(
                    os = "Android",
                    arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                    osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    deviceType = "android",
                    version = BuildConfig.VERSION_NAME,
                ),
            )

            val url = settings.serverUrl.trimEnd('/') + "/api/workers/heartbeat"
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(settings.activeKey)
                setBody(heartbeat)
            }

            if (response.status.isSuccess()) {
                consecutiveFailures = 0
                _lastHeartbeat.value = System.currentTimeMillis()
                _lastHeartbeatFailed.value = false
                // Enrollment: on first contact (and re-delivered until we confirm it
                // by using it) the server returns this device's own fleet key. Persist
                // and adopt it, so subsequent heartbeats authenticate with our own key.
                val body = runCatching { response.body<WorkerHeartbeatResponse>() }.getOrNull()
                if (body != null) {
                    settingsAfterHeartbeat(settings, body)?.let { updated ->
                        SettingsStore.update(applicationContext) { updated }
                        Log.i(TAG, "Enrolled: received and persisted this device's own fleet key")
                    }
                    recordEarnings(body, System.currentTimeMillis())
                }
                // Counts only what is KNOWN to be running. An app we cannot
                // see is not counted as running, and equally not reported as a
                // failure -- the notification says how many are unknown instead
                // of quietly folding them into the stopped remainder.
                val runningCount = apps.count { it.running == true }
                val unknownCount = apps.count { it.running == null }
                updateNotification(
                    if (unknownCount > 0) {
                        "$runningCount/${apps.size} apps running, $unknownCount unknown"
                    } else {
                        "$runningCount/${apps.size} apps running"
                    },
                )
            } else {
                consecutiveFailures++
                _lastHeartbeatFailed.value = true
                Log.w(TAG, "Heartbeat rejected: HTTP ${response.status.value}")
                updateNotification("Server rejected heartbeat (${response.status.value})")
                if (response.status.value == 401 && settings.workerKey.isNotBlank()) {
                    SettingsStore.update(applicationContext) { it.copy(workerKey = "") }
                    Log.i(TAG, "Auth rejected (401) — clearing per-worker key to re-enroll")
                }
            }
        } catch (e: Exception) {
            consecutiveFailures++
            _lastHeartbeatFailed.value = true
            Log.w(TAG, "Heartbeat failed: ${e.message}")
            updateNotification("Heartbeat failed — retrying...")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "CashPilot Agent",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows monitoring status"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CashPilot")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    /** Short unique device ID from ANDROID_ID (per app+device, no permissions needed). */
    private fun deviceId(): String =
        Secure.getString(contentResolver, Secure.ANDROID_ID)?.take(8) ?: "unknown"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        heartbeatJob?.cancel()
        scope.cancel()
        httpClient.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HeartbeatService"
        private const val CHANNEL_ID = "cashpilot_agent"
        private const val NOTIFICATION_ID = 1

        /** Timestamp of last successful heartbeat (0 = never). */
        private val _lastHeartbeat = MutableStateFlow(0L)
        val lastHeartbeat: StateFlow<Long> = _lastHeartbeat.asStateFlow()

        /** Whether the last heartbeat attempt failed. */
        private val _lastHeartbeatFailed = MutableStateFlow(false)
        val lastHeartbeatFailed: StateFlow<Boolean> = _lastHeartbeatFailed.asStateFlow()

        /**
         * Earnings from the most recent heartbeat that carried them, or null.
         *
         * Null is UNKNOWN, not zero: an older server, a server that could not
         * produce the figures, or no heartbeat yet. The last known value is kept
         * when a later heartbeat omits it, because a phone is offline often and
         * blanking the figure on every blip would be worse than showing a stale
         * one -- but see [earningsAsOf], which is what lets the UI say it is stale
         * rather than pretending it is current.
         */
        private val _earnings = MutableStateFlow<Earnings?>(null)
        val earnings: StateFlow<Earnings?> = _earnings.asStateFlow()

        /** When [earnings] was received (0 = never). */
        private val _earningsAsOf = MutableStateFlow(0L)
        val earningsAsOf: StateFlow<Long> = _earningsAsOf.asStateFlow()

        /**
         * The earnings to keep after a heartbeat: the newly received ones, or the
         * previous value when this response carried none. Pure, so it is
         * unit-tested without a service.
         */
        fun earningsToKeep(current: Earnings?, received: Earnings?): Earnings? = received ?: current

        /**
         * The per-worker key to newly persist, given the currently stored key and the
         * one the server returned on this heartbeat — or `null` if nothing should
         * change (no key issued, blank, or unchanged). Pure, so it is unit-tested.
         */
        fun keyToPersist(current: String, issued: String?): String? =
            issued?.takeIf { it.isNotBlank() && it != current }

        /**
         * The [Settings] to persist after a successful heartbeat, given the current
         * settings and the parsed response body — or `null` if nothing should change.
         * Pure, so the response→persist wiring is unit-tested without Robolectric.
         */
        fun settingsAfterHeartbeat(settings: Settings, body: WorkerHeartbeatResponse): Settings? =
            keyToPersist(settings.workerKey, body.workerKey)?.let { newKey -> settings.copy(workerKey = newKey) }

        /**
         * Record the earnings a heartbeat carried. Separate from
         * [settingsAfterHeartbeat] because earnings are ephemeral display state,
         * not settings -- persisting them to DataStore would mean writing on every
         * heartbeat for a value that is meaningless once stale.
         */
        fun recordEarnings(body: WorkerHeartbeatResponse, now: Long) {
            val kept = earningsToKeep(_earnings.value, body.earnings)
            _earnings.value = kept
            if (body.earnings != null) _earningsAsOf.value = now
        }
    }
}

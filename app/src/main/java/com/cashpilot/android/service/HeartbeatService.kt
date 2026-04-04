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
import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.util.SettingsStore
import io.ktor.client.HttpClient
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
                if (settings.serverUrl.isNotBlank() && settings.apiKey.isNotBlank()) {
                    sendHeartbeat(settings)
                }
                // Exponential backoff on consecutive failures (30s → 60s → 120s, max 5min)
                val baseDelay = settings.heartbeatIntervalSeconds * 1000L
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
                    status = if (app.running) "running" else "stopped",
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
                ),
            )

            val url = settings.serverUrl.trimEnd('/') + "/api/workers/heartbeat"
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(settings.apiKey)
                setBody(heartbeat)
            }

            if (response.status.isSuccess()) {
                consecutiveFailures = 0
                _lastHeartbeat.value = System.currentTimeMillis()
                _lastHeartbeatFailed.value = false
                val runningCount = apps.count { it.running }
                updateNotification("$runningCount/${apps.size} apps running")
            } else {
                consecutiveFailures++
                _lastHeartbeatFailed.value = true
                Log.w(TAG, "Heartbeat rejected: HTTP ${response.status.value}")
                updateNotification("Server rejected heartbeat (${response.status.value})")
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
    }
}

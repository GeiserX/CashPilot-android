package com.cashpilot.android.service

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import android.util.Log
import com.cashpilot.android.BuildConfig
import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.HeartbeatJson
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.util.ServerUrlPolicy
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One heartbeat to the CashPilot server, and what the last one said.
 *
 * This used to live in a foreground service that posted every 30 seconds for as
 * long as the phone was on. [HeartbeatWorker] now runs it on WorkManager's
 * schedule instead (every 15 minutes, plus once when the app is opened or the
 * server details change), so there is no permanent notification and no
 * special-use foreground service to justify to a store.
 *
 * The process is no longer kept alive between heartbeats, so what the UI shows
 * about the last one ([lastHeartbeat], [lastHeartbeatFailed], [earnings],
 * [earningsAsOf]) is written to disk after each run and read back by [restore]
 * when the process starts. Without that, every cold start would say "no
 * heartbeat yet" and "nothing read yet" for up to 15 minutes.
 */
object Heartbeat {
    private const val TAG = "Heartbeat"
    private const val PREFS = "heartbeat_state"
    private const val K_LAST = "last_heartbeat"
    private const val K_FAILED = "last_heartbeat_failed"
    private const val K_EARNINGS = "earnings_json"
    private const val K_EARNINGS_AS_OF = "earnings_as_of"

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
     * unit-tested without Android.
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

    /** Record the earnings a heartbeat carried, keeping the previous ones if it carried none. */
    fun recordEarnings(body: WorkerHeartbeatResponse, now: Long) {
        val kept = earningsToKeep(_earnings.value, body.earnings)
        _earnings.value = kept
        if (body.earnings != null) _earningsAsOf.value = now
    }

    /** Load what the last run in an earlier process saw. Called once at process start. */
    fun restore(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _lastHeartbeat.value = p.getLong(K_LAST, 0L)
        _lastHeartbeatFailed.value = p.getBoolean(K_FAILED, false)
        _earnings.value = p.getString(K_EARNINGS, null)?.let {
            // A shape this version cannot read is unknown, never a crash at startup.
            runCatching { HeartbeatJson.decodeFromString(Earnings.serializer(), it) }.getOrNull()
        }
        _earningsAsOf.value = p.getLong(K_EARNINGS_AS_OF, 0L)
    }

    internal fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(K_LAST, _lastHeartbeat.value)
            .putBoolean(K_FAILED, _lastHeartbeatFailed.value)
            .putString(K_EARNINGS, _earnings.value?.let { HeartbeatJson.encodeToString(Earnings.serializer(), it) })
            .putLong(K_EARNINGS_AS_OF, _earningsAsOf.value)
            .apply()
    }

    private fun newClient() = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(HeartbeatJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

    /** Send one heartbeat if a server is configured, and record the outcome. */
    suspend fun sendOnce(context: Context, settings: Settings) {
        if (settings.serverUrl.isBlank() || settings.activeKey.isBlank()) return
        try {
            send(context, settings)
        } finally {
            persist(context)
        }
    }

    private suspend fun send(context: Context, settings: Settings) {
        val url = settings.serverUrl.trimEnd('/') + "/api/workers/heartbeat"
        // http to a public host would hand the fleet key to every network on the way.
        if (!ServerUrlPolicy.allowsToken(url)) {
            _lastHeartbeatFailed.value = true
            Log.w(TAG, "Heartbeat not sent: server URL is not https and not a private address")
            return
        }
        val client = newClient()
        try {
            val apps = AppDetector(context).detectAll(settings.enabledSlugs)

            // Send apps in both formats for backward compatibility:
            // - `apps` (new): rich app data for servers that understand Android workers
            // - `containers` (legacy): simplified format so older servers still show the worker
            val containers = apps.map { app ->
                AppContainer(
                    slug = app.slug,
                    name = "cashpilot-${app.slug}",
                    // Three-valued on the wire too. `unknown` is NOT "stopped":
                    // the server maps a falsy running to "stopped", so sending
                    // false while blind would hand the fleet page a false claim.
                    status = Detection.wireStatus(app.running),
                    labels = mapOf(
                        "cashpilot.managed" to "true",
                        "cashpilot.service" to app.slug,
                    ),
                )
            }

            val heartbeat = WorkerHeartbeat(
                name = "${Build.MANUFACTURER} ${Build.MODEL} (${deviceId(context)})",
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

            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                bearerAuth(settings.activeKey)
                setBody(heartbeat)
            }

            if (response.status.isSuccess()) {
                _lastHeartbeat.value = System.currentTimeMillis()
                _lastHeartbeatFailed.value = false
                // Enrollment: on first contact (and re-delivered until we confirm it
                // by using it) the server returns this device's own fleet key. Persist
                // and adopt it, so subsequent heartbeats authenticate with our own key.
                val body = runCatching { response.body<WorkerHeartbeatResponse>() }.getOrNull()
                if (body != null) {
                    settingsAfterHeartbeat(settings, body)?.let { updated ->
                        SettingsStore.update(context) { updated }
                        Log.i(TAG, "Enrolled: received and persisted this device's own fleet key")
                    }
                    recordEarnings(body, System.currentTimeMillis())
                }
            } else {
                _lastHeartbeatFailed.value = true
                Log.w(TAG, "Heartbeat rejected: HTTP ${response.status.value}")
                if (response.status.value == 401 && settings.workerKey.isNotBlank()) {
                    SettingsStore.update(context) { it.copy(workerKey = "") }
                    Log.i(TAG, "Auth rejected (401) — clearing per-worker key to re-enroll")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _lastHeartbeatFailed.value = true
            Log.w(TAG, "Heartbeat failed: ${e.message}")
        } finally {
            client.close()
        }
    }

    /** Short unique device ID from ANDROID_ID (per app+device, no permissions needed). */
    private fun deviceId(context: Context): String =
        Secure.getString(context.contentResolver, Secure.ANDROID_ID)?.take(8) ?: "unknown"
}

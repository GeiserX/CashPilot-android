package com.cashpilot.android.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Heartbeat payload sent to CashPilot server.
 * Matches the WorkerHeartbeat schema at POST /api/workers/heartbeat.
 */
@Serializable
data class WorkerHeartbeat(
    val name: String,
    val url: String = "",
    /** Docker containers (empty for Android workers). */
    val containers: List<AppContainer> = emptyList(),
    /** Android app statuses (empty for Docker workers). */
    val apps: List<AppStatus> = emptyList(),
    @SerialName("system_info") val systemInfo: SystemInfo = SystemInfo(),
)

/** Maps an Android app to the server's container-like representation.
 *  Must include `slug` — the server discovers worker items via `containers[*].slug`. */
@Serializable
data class AppContainer(
    val slug: String,
    val name: String,
    val status: String,
    val image: String = "",
    val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class SystemInfo(
    val os: String = "",
    val arch: String = "",
    @SerialName("os_version") val osVersion: String = "",
    @SerialName("device_type") val deviceType: String = "android",
    val apps: List<AppStatus> = emptyList(),
)

@Serializable
data class AppStatus(
    val slug: String,
    val running: Boolean,
    @SerialName("notification_active") val notificationActive: Boolean = false,
    @SerialName("net_tx_24h") val netTx24h: Long = 0,
    @SerialName("net_rx_24h") val netRx24h: Long = 0,
    @SerialName("last_active") val lastActive: String? = null,
)

/**
 * Response from POST /api/workers/heartbeat.
 *
 * On this device's first (enrollment) heartbeat — and re-delivered on each
 * subsequent heartbeat until the device confirms it — the server returns a
 * per-worker key in [workerKey]. The client persists it and authenticates with
 * it thereafter (the shared key becomes enrollment-only). [workerKey] is absent
 * once the device is enrolled and confirmed.
 */
@Serializable
data class WorkerHeartbeatResponse(
    val status: String = "",
    @SerialName("worker_id") val workerId: Long? = null,
    @SerialName("worker_key") val workerKey: String? = null,
)

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
    val containers: List<AppContainer> = emptyList(),
    @SerialName("system_info") val systemInfo: SystemInfo = SystemInfo(),
)

/** Maps an Android app to the server's container-like representation. */
@Serializable
data class AppContainer(
    val name: String,
    val status: String,
    val image: String = "",
    /** Extra Android-specific fields packed here for forward compatibility. */
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

package com.cashpilot.android.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Heartbeat payload sent to CashPilot master. Matches /api/worker/heartbeat schema. */
@Serializable
data class Heartbeat(
    @SerialName("device_type") val deviceType: String = "android",
    val hostname: String,
    val os: String,
    val arch: String,
    @SerialName("os_version") val osVersion: String,
    val apps: List<AppStatus>,
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

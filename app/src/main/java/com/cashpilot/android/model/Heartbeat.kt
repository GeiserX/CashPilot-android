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
    /**
     * This app's own version, e.g. "0.2.1".
     *
     * The server reads `system_info.version` to decide whether a worker is on a
     * different release series from the UI. Android never sent it, so every
     * device read as "version unknown" and there was no way to see which phones
     * were on an outdated build -- which is how two devices sat on a
     * pre-enrollment release for weeks without anyone noticing.
     *
     * Defaults to empty on purpose: an ABSENT version must read as unknown, not
     * as a match. The server's own rule is that both sides must be known
     * releases before it will call anything a mismatch.
     *
     * Declared LAST on purpose: DataClassContractTest destructures SystemInfo
     * positionally, so member order is part of this class's contract and
     * inserting anywhere else silently changes what component5() means.
     */
    val version: String = "",
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
    /**
     * What the platforms on this device have earned, when the server chose to
     * send it. Null means UNKNOWN — an older server, or one that could not
     * produce the figures — and must never be rendered as zero.
     */
    val earnings: Earnings? = null,
)

/**
 * Earnings for the platforms this device is running.
 *
 * Deliberately NOT "what this device earned". Providers report one balance per
 * account, so when the same app runs on two machines nothing can split it — see
 * [PlatformEarnings.sharedWithOtherWorkers]. Claiming a per-device figure would
 * be inventing one.
 */
@Serializable
data class Earnings(
    @SerialName("window_days") val windowDays: Int = 30,
    val currency: String = "USD",
    val platforms: List<PlatformEarnings> = emptyList(),
    /** Sum of the platforms that HAVE a reading. Null when none do. */
    @SerialName("total_usd") val totalUsd: Double? = null,
    @SerialName("platforms_without_readings") val platformsWithoutReadings: List<String> = emptyList(),
)

@Serializable
data class PlatformEarnings(
    val slug: String = "",
    /** Null means nothing has ever been read for this platform. Not zero. */
    val usd: Double? = null,
    @SerialName("shared_with_other_workers") val sharedWithOtherWorkers: Boolean = false,
)

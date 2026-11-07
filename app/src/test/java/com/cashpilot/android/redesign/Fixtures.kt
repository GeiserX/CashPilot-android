package com.cashpilot.android.redesign

import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.PlatformEarnings
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppPresentation
import java.time.Instant

/**
 * One set of phone situations, shared by the screenshots of the current app and
 * of the three proposals, so every image shows the same facts.
 */
object Fixtures {
    private val now = System.currentTimeMillis()
    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val MB = 1024L * 1024L

    private fun iso(millisAgo: Long) = Instant.ofEpochMilli(now - millisAgo).toString()

    data class Scenario(
        val apps: List<AppDisplayInfo>,
        val earnings: Earnings?,
        val earningsAsOf: Long,
        val serverUrl: String,
        val lastHeartbeat: Long,
        val heartbeatFailed: Boolean,
        val notif: Boolean,
        val usage: Boolean,
        val battery: Boolean,
    ) {
        val serverConfigured get() = serverUrl.isNotBlank()
        val blind get() = !notif && !usage
    }

    private fun running(slug: String, tx: Long, rx: Long) =
        AppStatus(slug, running = true, notificationActive = true, netTx24h = tx * MB, netRx24h = rx * MB, lastActive = iso(4 * MIN))

    private fun stopped(slug: String, agoMillis: Long) =
        AppStatus(slug, running = false, netTx24h = 3 * MB, netRx24h = 1 * MB, lastActive = iso(agoMillis))

    /** Builds the display list exactly as MainViewModel.doRefresh does, then sorts it the same way. */
    private fun build(statuses: Map<String, AppStatus?>, installed: Set<String>, disabled: Set<String> = emptySet()): List<AppDisplayInfo> =
        AppPresentation.sortForDashboard(
            KnownApps.all.map { app ->
                val status = statuses[app.slug]
                val state = AppPresentation.resolveState(
                    installed = app.slug in installed,
                    enabled = app.slug !in disabled,
                    running = status?.running,
                )
                AppDisplayInfo(app, state, status)
            },
        )

    private val installedSet = setOf("earnapp", "iproyal", "mysterium", "traffmonetizer", "grass", "bytelixir")

    private val earnings = Earnings(
        windowDays = 30,
        platforms = listOf(
            PlatformEarnings("bytelixir", 0.94, false),
            PlatformEarnings("earnapp", 4.12, false),
            PlatformEarnings("grass", null, false),
            PlatformEarnings("iproyal", 2.87, false),
            PlatformEarnings("mysterium", 6.40, true),
            PlatformEarnings("traffmonetizer", 1.05, false),
        ),
        totalUsd = 15.38,
        platformsWithoutReadings = listOf("grass"),
    )

    /** Paired, reporting, everything installed is earning. */
    val healthy = Scenario(
        apps = build(
            mapOf(
                "earnapp" to running("earnapp", 412, 38),
                "iproyal" to running("iproyal", 655, 41),
                "mysterium" to running("mysterium", 1290, 210),
                "traffmonetizer" to running("traffmonetizer", 233, 19),
                "grass" to running("grass", 96, 12),
                "bytelixir" to running("bytelixir", 120, 9),
            ),
            installedSet,
        ),
        earnings = earnings,
        earningsAsOf = now - 2 * MIN,
        serverUrl = "https://cashpilot.home.example",
        lastHeartbeat = now - 20_000,
        heartbeatFailed = false,
        notif = true, usage = true, battery = true,
    )

    /** Two earners died, the server stopped answering 25 minutes ago, battery saver is on. */
    val problems = healthy.copy(
        apps = build(
            mapOf(
                "earnapp" to running("earnapp", 412, 38),
                "iproyal" to running("iproyal", 655, 41),
                "mysterium" to running("mysterium", 1290, 210),
                "traffmonetizer" to stopped("traffmonetizer", 26 * HOUR),
                "grass" to stopped("grass", 3 * HOUR),
                "bytelixir" to running("bytelixir", 120, 9),
            ),
            installedSet,
        ),
        earningsAsOf = now - 7 * HOUR,
        lastHeartbeat = now - 25 * MIN,
        heartbeatFailed = true,
        battery = false,
    )

    /** No server. A supported way to use the app. */
    val standalone = healthy.copy(
        apps = build(
            mapOf(
                "earnapp" to running("earnapp", 412, 38),
                "iproyal" to running("iproyal", 655, 41),
                "grass" to stopped("grass", 3 * HOUR),
            ),
            setOf("earnapp", "iproyal", "grass"),
        ),
        earnings = null,
        earningsAsOf = 0,
        serverUrl = "",
        lastHeartbeat = 0,
        heartbeatFailed = false,
    )

    /** First launch after setup was skipped: no permissions, so nothing can be seen. */
    val blind = healthy.copy(
        apps = build(installedSet.associateWith { null }, installedSet),
        notif = false, usage = false, battery = false,
        earnings = null, earningsAsOf = 0, lastHeartbeat = 0,
    )

    /** A fresh install: nothing granted, no server typed yet. */
    val firstRun = blind.copy(serverUrl = "")

    fun all() = mapOf("healthy" to healthy, "problems" to problems, "standalone" to standalone, "blind" to blind)
}

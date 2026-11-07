package com.cashpilot.android.board

import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.PlatformEarnings
import com.cashpilot.android.model.Settings
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.board.Board
import java.time.Instant

/**
 * The four phone situations every screen is tested and photographed in, built
 * through [Board.from] exactly as the app builds them, at a fixed moment so the
 * ages ("6 min ago") never change between runs.
 */
object Situations {
    val NOW: Long = Instant.parse("2026-11-07T10:00:00Z").toEpochMilli()
    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val MB = 1024L * 1024L

    private fun iso(millisAgo: Long) = Instant.ofEpochMilli(NOW - millisAgo).toString()

    fun running(slug: String, tx: Long, rx: Long) =
        AppStatus(slug, running = true, notificationActive = true, netTx24h = tx * MB, netRx24h = rx * MB, lastActive = iso(4 * MIN))

    fun stopped(slug: String, agoMillis: Long) =
        AppStatus(slug, running = false, netTx24h = 3 * MB, netRx24h = 1 * MB, lastActive = iso(agoMillis))

    /** The display list exactly as MainViewModel.doRefresh builds and sorts it. */
    fun apps(statuses: Map<String, AppStatus?>, installed: Set<String>, disabled: Set<String> = emptySet()): List<AppDisplayInfo> =
        AppPresentation.sortForDashboard(
            KnownApps.all.map { app ->
                val status = statuses[app.slug]
                AppDisplayInfo(
                    app,
                    AppPresentation.resolveState(app.slug in installed, app.slug !in disabled, status?.running),
                    status,
                )
            },
        )

    private val installedSet = setOf("earnapp", "iproyal", "mysterium", "traffmonetizer", "grass", "bytelixir")

    val earnings = Earnings(
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

    private val paired = Settings(serverUrl = "https://cashpilot.home.example", apiKey = "cp_live_8f2c1d9a7b9a", setupCompleted = true)

    fun board(
        apps: List<AppDisplayInfo>,
        settings: Settings = paired,
        lastHeartbeat: Long = NOW - 6 * MIN,
        failed: Boolean = false,
        earnings: Earnings? = this.earnings,
        earningsAsOf: Long = NOW - 2 * MIN,
        notif: Boolean = true,
        usage: Boolean = true,
        battery: Boolean = true,
    ) = Board.from(apps, settings, lastHeartbeat, failed, earnings, earningsAsOf, notif, usage, battery, NOW)

    private val healthyApps = apps(
        mapOf(
            "earnapp" to running("earnapp", 412, 38),
            "iproyal" to running("iproyal", 655, 41),
            "mysterium" to running("mysterium", 1290, 210),
            "traffmonetizer" to running("traffmonetizer", 233, 19),
            "grass" to running("grass", 96, 12),
            "bytelixir" to running("bytelixir", 120, 9),
        ),
        installedSet,
    )

    /** Paired, reporting, everything installed is running. */
    val healthy = board(healthyApps)

    /** Two apps stopped, the server stopped answering 25 minutes ago, battery optimisation is on. */
    val problems = board(
        apps(
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
        lastHeartbeat = NOW - 25 * MIN,
        failed = true,
        earningsAsOf = NOW - 7 * HOUR,
        battery = false,
    )

    /** No server, which is a supported way to use the app. One app stopped. */
    val standalone = board(
        apps(
            mapOf(
                "earnapp" to running("earnapp", 412, 38),
                "iproyal" to running("iproyal", 655, 41),
                "grass" to stopped("grass", 3 * HOUR),
            ),
            setOf("earnapp", "iproyal", "grass"),
        ),
        settings = Settings(setupCompleted = true),
        lastHeartbeat = 0,
        earnings = null,
        earningsAsOf = 0,
    )

    /** Neither access granted, so nothing can be seen. */
    val blind = board(
        apps(installedSet.associateWith { null }, installedSet),
        lastHeartbeat = 0,
        earnings = null,
        earningsAsOf = 0,
        notif = false,
        usage = false,
        battery = false,
    )

    /** Nothing supported installed, no server: what a store reviewer sees. */
    val empty = board(apps(emptyMap(), emptySet()), settings = Settings(setupCompleted = true), lastHeartbeat = 0, earnings = null, earningsAsOf = 0)

    val four = linkedMapOf("healthy" to healthy, "problems" to problems, "standalone" to standalone, "blind" to blind)
}

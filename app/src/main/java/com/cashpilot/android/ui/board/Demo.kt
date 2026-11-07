package com.cashpilot.android.ui.board

import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.ui.AppPresentation
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.EarningsPresentation

/**
 * Sample data for demo mode, so someone without any supported app (a store
 * reviewer, or someone deciding whether to install one) can see what the board
 * does. Every demo screen carries the "sample data" banner.
 *
 * Built relative to [now] so the ages read the same whenever it is opened. The
 * figures are not anybody's earnings and are kept small and round on purpose.
 */
object Demo {
    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val MB = 1024L * 1024L

    fun board(now: Long): Board {
        data class S(val state: AppState, val agoMin: Long, val mb: Long, val usd: Double?, val shared: Boolean = false)
        val sample = mapOf(
            "earnapp" to S(AppState.RUNNING, 4, 450, 1.20),
            "iproyal" to S(AppState.RUNNING, 4, 690, 0.80),
            "mysterium" to S(AppState.RUNNING, 4, 1500, 2.00, shared = true),
            "grass" to S(AppState.STOPPED, 3 * 60, 4, null),
            "traffmonetizer" to S(AppState.RUNNING, 4, 250, 0.50),
        )
        val rows = KnownApps.all.map { app ->
            val s = sample[app.slug]
            AppRow(
                slug = app.slug,
                name = app.displayName,
                packageName = app.packageName,
                state = s?.state ?: AppState.NOT_INSTALLED,
                lastActive = s?.let { now - it.agoMin * MIN },
                bytes24h = (s?.mb ?: 0) * MB,
                usd = s?.usd,
                shared = s?.shared == true,
                referralUrl = app.referralUrl,
            )
        }.sortedWith(compareBy({ AppPresentation.attentionRank(it.state) }, { it.name.lowercase() }))
        return Board(
            apps = rows,
            server = ServerState.OK,
            host = "cashpilot.example.com",
            lastReport = now - 6 * MIN,
            total = sample.values.mapNotNull { it.usd }.sum(),
            windowDays = 30,
            earningsAsOf = now - HOUR / 4,
            earningsMode = EarningsPresentation.Mode.FIGURE,
            stale = false,
            notif = true,
            usage = true,
            battery = true,
            now = now,
        )
    }
}

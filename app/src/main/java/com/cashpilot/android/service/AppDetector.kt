package com.cashpilot.android.service

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Detects the state of monitored apps using three complementary Android APIs:
 *
 * 1. NotificationListenerService — instant callback, proves foreground service is alive
 * 2. UsageStatsManager — last foreground time (~2h buckets)
 * 3. NetworkStatsManager — per-app bytes tx/rx (~2h buckets)
 */
class AppDetector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    /** Check all enabled apps and return their current status. */
    fun detectAll(enabledSlugs: Set<String>): List<AppStatus> =
        KnownApps.all
            .filter { it.slug in enabledSlugs }
            .filter { isInstalled(it.packageName) }
            .map { detect(it) }

    private fun detect(app: MonitoredApp): AppStatus {
        val notificationActive = AppNotificationListener.isAppNotificationActive(app.packageName)
        val lastActive = getLastActiveTime(app.packageName)
        val (tx, rx) = getNetworkStats(app.packageName)

        // App is "running" if it has an active foreground notification
        // OR was active within the last 15 minutes
        val recentlyActive = lastActive?.let {
            (System.currentTimeMillis() - it) < 15 * 60 * 1000
        } ?: false

        return AppStatus(
            slug = app.slug,
            running = notificationActive || recentlyActive,
            notificationActive = notificationActive,
            netTx24h = tx,
            netRx24h = rx,
            lastActive = lastActive?.let {
                Instant.ofEpochMilli(it)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
            },
        )
    }

    private fun isInstalled(packageName: String): Boolean =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun getLastActiveTime(packageName: String): Long? {
        val usm = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 24 * 60 * 60 * 1000,
            now,
        )
        return stats
            ?.filter { it.packageName == packageName }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.lastTimeUsed
            ?.takeIf { it > 0 }
    }

    @Suppress("DEPRECATION")
    private fun getNetworkStats(packageName: String): Pair<Long, Long> {
        val nsm = networkStatsManager ?: return 0L to 0L
        val uid = try {
            packageManager.getApplicationInfo(packageName, 0).uid
        } catch (_: PackageManager.NameNotFoundException) {
            return 0L to 0L
        }

        val now = System.currentTimeMillis()
        val dayAgo = now - 24 * 60 * 60 * 1000
        var tx = 0L
        var rx = 0L

        // Query both WiFi and mobile
        for (networkType in intArrayOf(
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_MOBILE,
        )) {
            try {
                val bucket = nsm.querySummaryForDevice(networkType, null, dayAgo, now)
                // querySummary gives per-app breakdowns
                val summary = nsm.querySummary(networkType, null, dayAgo, now)
                val b = android.app.usage.NetworkStats.Bucket()
                while (summary.hasNextBucket()) {
                    summary.getNextBucket(b)
                    if (b.uid == uid) {
                        tx += b.txBytes
                        rx += b.rxBytes
                    }
                }
                summary.close()
            } catch (_: Exception) {
                // Permission not granted or network type not available
            }
        }

        return tx to rx
    }
}

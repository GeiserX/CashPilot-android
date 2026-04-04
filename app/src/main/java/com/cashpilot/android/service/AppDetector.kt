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

    /** Check whether a package is installed on this device. */
    fun isInstalled(packageName: String): Boolean =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private fun detect(app: MonitoredApp): AppStatus {
        val notificationActive = AppNotificationListener.isAppNotificationActive(app.packageName)
        val lastActive = getLastActiveTime(app.packageName)
        val (tx24h, rx24h) = getNetworkStats(app.packageName, hours = 24)
        // Use a 2h window for the "running" heuristic to avoid stale 24h false positives
        val (tx2h, rx2h) = getNetworkStats(app.packageName, hours = 2)

        // App is "running" if:
        // 1. It has an active foreground notification, OR
        // 2. It was in foreground within the last 15 minutes, OR
        // 3. It has network activity in the last 2h (bandwidth apps run as background services)
        val recentlyActive = lastActive?.let {
            (System.currentTimeMillis() - it) < 15 * 60 * 1000
        } ?: false
        val hasRecentNetworkActivity = (tx2h + rx2h) > 1024 // >1KB in last 2h

        return AppStatus(
            slug = app.slug,
            running = notificationActive || recentlyActive || hasRecentNetworkActivity,
            notificationActive = notificationActive,
            netTx24h = tx24h,
            netRx24h = rx24h,
            lastActive = lastActive?.let {
                Instant.ofEpochMilli(it)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
            },
        )
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
    private fun getNetworkStats(packageName: String, hours: Int = 24): Pair<Long, Long> {
        val nsm = networkStatsManager ?: return 0L to 0L
        val uid = try {
            packageManager.getApplicationInfo(packageName, 0).uid
        } catch (_: PackageManager.NameNotFoundException) {
            return 0L to 0L
        }

        val now = System.currentTimeMillis()
        val start = now - hours.toLong() * 60 * 60 * 1000
        var tx = 0L
        var rx = 0L

        // Query both WiFi and mobile
        for (networkType in intArrayOf(
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_MOBILE,
        )) {
            try {
                // querySummary gives per-app breakdowns
                val summary = nsm.querySummary(networkType, null, start, now)
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

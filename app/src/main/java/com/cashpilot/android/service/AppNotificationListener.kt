package com.cashpilot.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.cashpilot.android.model.KnownApps
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listens for status bar notifications from monitored passive income apps.
 *
 * When a bandwidth-sharing app is running, it shows a persistent foreground
 * notification. This service detects when those notifications appear/disappear,
 * giving us an instant signal of whether each app is actively running.
 *
 * Requires the user to grant Notification Access in system settings.
 */
class AppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg in KnownApps.byPackage) {
            activeNotifications_[pkg] = System.currentTimeMillis()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeNotifications_.remove(sbn.packageName)
    }

    override fun onListenerConnected() {
        connected.set(true)
        // Full resync: clear stale state, then scan current notifications
        activeNotifications_.clear()
        activeNotifications?.forEach { sbn ->
            val pkg = sbn.packageName
            if (pkg in KnownApps.byPackage) {
                activeNotifications_[pkg] = System.currentTimeMillis()
            }
        }
    }

    override fun onListenerDisconnected() {
        connected.set(false)
        // Clear all state — we can no longer trust notification presence
        activeNotifications_.clear()
    }

    companion object {
        /** Package name → timestamp when notification was last seen. */
        val activeNotifications_ = ConcurrentHashMap<String, Long>()

        /** Whether the listener is currently connected to the system. */
        private val connected = AtomicBoolean(false)

        fun isAppNotificationActive(packageName: String): Boolean {
            if (!connected.get()) return false
            return packageName in activeNotifications_
        }

        fun isConnected(): Boolean = connected.get()
    }
}

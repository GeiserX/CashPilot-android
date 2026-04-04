package com.cashpilot.android.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.cashpilot.android.model.KnownApps
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listens for status bar notifications from monitored passive income apps.
 *
 * Only counts ongoing (FLAG_ONGOING_EVENT) notifications, which correspond to
 * foreground service notifications. Transient notifications (promos, updates)
 * are ignored.
 *
 * On removal, re-scans remaining notifications for the package instead of
 * blindly clearing — so dismissing one notification doesn't create a false
 * "stopped" if the foreground service notification is still present.
 *
 * Requires the user to grant Notification Access in system settings.
 */
class AppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg in KnownApps.byPackage && sbn.isOngoing) {
            activeNotifications_[pkg] = System.currentTimeMillis()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in KnownApps.byPackage) return

        // Check if any ongoing notification remains for this package
        val stillHasOngoing = activeNotifications?.any {
            it.packageName == pkg && it.isOngoing
        } ?: false

        if (!stillHasOngoing) {
            activeNotifications_.remove(pkg)
        }
    }

    override fun onListenerConnected() {
        connected.set(true)
        // Full resync: clear stale state, then scan current ongoing notifications
        activeNotifications_.clear()
        activeNotifications?.forEach { sbn ->
            val pkg = sbn.packageName
            if (pkg in KnownApps.byPackage && sbn.isOngoing) {
                activeNotifications_[pkg] = System.currentTimeMillis()
            }
        }
    }

    override fun onListenerDisconnected() {
        connected.set(false)
        activeNotifications_.clear()
    }

    companion object {
        /** Package name → timestamp when ongoing notification was last seen. */
        val activeNotifications_ = ConcurrentHashMap<String, Long>()

        private val connected = AtomicBoolean(false)

        fun isAppNotificationActive(packageName: String): Boolean {
            if (!connected.get()) return false
            return activeNotifications_.containsKey(packageName)
        }

        fun isConnected(): Boolean = connected.get()
    }
}

private val StatusBarNotification.isOngoing: Boolean
    get() = notification.flags and Notification.FLAG_ONGOING_EVENT != 0

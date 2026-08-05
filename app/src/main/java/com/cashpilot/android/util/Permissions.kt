package com.cashpilot.android.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.cashpilot.android.service.AppNotificationListener

/**
 * The two permissions this app's detection actually depends on.
 *
 * These checks lived only inside `MainViewModel`, so `AppDetector` had no way to
 * know whether it could see anything — and it did not ask. It called
 * `getSystemService(...)`, got a non-null manager back (the service is always
 * returned; the permission is enforced at QUERY time), and read the empty
 * results as "nothing is running".
 *
 * One copy, used by both, so the detector and the screen can never disagree
 * about whether this device can see its own apps.
 */
object Permissions {

    /**
     * Notification-listener access: the instant, authoritative signal that an
     * app's foreground service is alive.
     */
    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return ComponentName(context, AppNotificationListener::class.java).flattenToString() in enabled
    }

    /**
     * Usage access, which gates BOTH the foreground-time lookup and the per-app
     * network counters — two of the three detection signals.
     *
     * Checked via AppOpsManager rather than by calling `queryUsageStats` and
     * seeing whether it comes back empty: an empty result is genuinely ambiguous
     * between "denied" and "nothing used recently", and guessing wrong there is
     * the whole defect this module exists to avoid.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

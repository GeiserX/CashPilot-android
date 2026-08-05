package com.cashpilot.android.service

/**
 * Whether a monitored app is running — and, crucially, whether that can be known
 * at all.
 *
 * ## The bug this exists to prevent
 *
 * Detection previously reduced to one line:
 *
 * ```
 * running = notificationActive || recentlyActive || hasRecentNetworkActivity
 * ```
 *
 * Every one of those three degrades to `false` when the permissions are missing.
 * Without notification-listener access the service receives no callbacks;
 * without usage access `getLastActiveTime` returns null and `getNetworkStats`
 * returns `0L to 0L`. So a device with the permissions denied reported **every**
 * earning app as STOPPED — stating as fact that the user's apps had died when it
 * simply could not see them. A user acting on that would restart apps that were
 * running perfectly, and the same claim was sent upstream to the fleet.
 *
 * ## The rule
 *
 * A negative is only trustworthy when every signal source was actually
 * available. A *positive* needs just one, because each is proof of life:
 *
 * | signals seen | any positive | answer |
 * |---|---|---|
 * | any | yes | `true` — something proved it is alive |
 * | all | no | `false` — it should be reporting and is not |
 * | some or none | no | `null` — **cannot tell**, do not guess |
 *
 * That middle row is what STOPPED means, and it is worth acting on. The last row
 * is a different problem with a different fix: grant the permission, do not
 * restart the app.
 */
object Detection {

    /**
     * Resolve the running state, or null when the detectors could not tell.
     *
     * @param canSeeNotifications notification-listener access is granted
     * @param canSeeUsage usage access is granted — this gates BOTH the
     *   foreground-time lookup and the per-app network counters, which is why
     *   one flag covers two of the three signals
     */
    fun resolveRunning(
        canSeeNotifications: Boolean,
        canSeeUsage: Boolean,
        notificationActive: Boolean,
        recentlyActive: Boolean,
        hasRecentNetworkActivity: Boolean,
    ): Boolean? {
        // Any positive signal is proof of life regardless of what else is
        // missing. A running app does not become less running because another
        // permission was denied.
        if (notificationActive || recentlyActive || hasRecentNetworkActivity) return true

        // Nothing positive. That is only meaningful if we could actually look
        // everywhere -- most bandwidth apps run with no visible notification and
        // are caught solely by network activity, so with usage access denied a
        // "false" here would be exactly the app we failed to see.
        if (!canSeeNotifications || !canSeeUsage) return null

        return false
    }

    /**
     * Whether detection is blind for this device: no signal source at all.
     *
     * Distinct from [resolveRunning] returning null for one app — this is the
     * device-wide condition that makes the permission prompt the only honest
     * thing the screen can show.
     */
    fun isBlind(canSeeNotifications: Boolean, canSeeUsage: Boolean): Boolean =
        !canSeeNotifications && !canSeeUsage

    /**
     * How a three-valued [resolveRunning] result is spelled on the wire.
     *
     * Lives here rather than inline at the call site because three separate
     * tests had each copied the old two-valued expression
     * (`if (app.running) "running" else "stopped"`) into themselves. A copy
     * cannot fail when production changes, so they would have gone on asserting
     * the two-valued behaviour after the third value existed.
     *
     * "unknown" is NOT "stopped": the server maps a falsy running to "stopped",
     * so collapsing them here would hand the fleet page exactly the false claim
     * this module removes from the phone.
     */
    fun wireStatus(running: Boolean?): String = when (running) {
        true -> "running"
        false -> "stopped"
        null -> "unknown"
    }
}

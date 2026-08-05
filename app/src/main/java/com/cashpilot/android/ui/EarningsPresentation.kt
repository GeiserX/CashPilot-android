package com.cashpilot.android.ui

import com.cashpilot.android.model.Earnings
import com.cashpilot.android.util.FormatUtils

/**
 * What the earnings card should say, decided outside the Composable.
 *
 * These rules are the whole value of the earnings feature, and every one of
 * them is a thing a display can silently discard:
 *
 *  - a missing figure is **not** zero,
 *  - a kept figure is **not** a current one,
 *  - an account balance is **not** a device's earnings.
 *
 * Left inside `@Composable` code they could only be checked by rendering the
 * UI, which this module has no harness for — so they would go unverified,
 * which is how the entire earnings pipeline came to be built and then never
 * displayed. As a plain function they are ordinary unit tests.
 */
object EarningsPresentation {

    /** What the card shows in its main slot. */
    enum class Mode {
        /** No server paired. Standalone is supported, so this is normal, not an error. */
        NEEDS_SERVER,

        /** Paired, but nothing has ever been read. NOT "earned zero". */
        NOTHING_READ,

        /** A real figure, possibly a kept one — see [isStale]. */
        FIGURE,
    }

    fun mode(earnings: Earnings?, serverConfigured: Boolean): Mode = when {
        !serverConfigured -> Mode.NEEDS_SERVER
        // Covers both "no heartbeat carried figures" (null earnings) and
        // "figures arrived but no platform had a reading" (null total). Both
        // mean nothing was measured.
        earnings?.totalUsd == null -> Mode.NOTHING_READ
        else -> Mode.FIGURE
    }

    /**
     * Whether the figure on screen must be labelled as possibly out of date.
     *
     * Only ever true when there IS a figure: "stale" beside "nothing read yet"
     * would be noise, since no measurement is being presented as current.
     */
    fun isStale(earnings: Earnings?, serverConfigured: Boolean, asOfMillis: Long, nowMillis: Long): Boolean =
        mode(earnings, serverConfigured) == Mode.FIGURE &&
            FormatUtils.earningsAreStale(asOfMillis, nowMillis)

    /**
     * Whether a per-app figure belongs on this app's card.
     *
     * Only for apps actually on the device and switched on. Beside a
     * NOT_INSTALLED app it would be noise; beside a DISABLED one it is stale by
     * design, because the app was deliberately switched off.
     */
    fun showsPerAppEarnings(state: AppState): Boolean =
        state == AppState.RUNNING || state == AppState.STOPPED
}

package com.cashpilot.android.util

import java.time.Instant

/** Format byte counts into human-readable strings. */
object FormatUtils {

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }

    fun parseIso(iso: String): Long =
        try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            0L
        }

    /**
     * How a platform's earnings should read, or null when there is nothing to say.
     *
     * The rules here are the whole point, and every one of them is a thing the
     * server took care to express and a client can easily throw away:
     *
     *  - `null` usd means NOTHING HAS EVER BEEN READ for that platform. It renders
     *    as an em-dash, never as "$0.00". A confident zero next to a service the
     *    user is running is a lie, and it is the exact defect class the server side
     *    of this project has spent dozens of fixes removing.
     *  - a genuine 0.0 IS a measurement and renders as $0.00.
     *  - a platform whose app also runs on another machine cannot be attributed to
     *    this device, so the caller is told to say so rather than implying it.
     */
    fun formatPlatformEarnings(usd: Double?): String = if (usd == null) "\u2014" else "$" + String.format("%.2f", usd)

    /**
     * The device-level total line, or null when there is nothing honest to show.
     *
     * Null total means no platform on this device has ever been read; showing
     * "$0.00" there would state a measurement nobody took.
     */
    fun formatEarningsTotal(totalUsd: Double?, windowDays: Int): String? =
        totalUsd?.let { "$" + String.format("%.2f", it) + " in the last " + windowDays + " days" }

    /**
     * Whether the figures are stale enough that the UI must say so.
     *
     * A phone is offline often. Showing the last known figure is kinder than
     * blanking it on every blip — but only if it is labelled, so the user is never
     * told a stale number is current.
     */
    fun earningsAreStale(asOfMillis: Long, nowMillis: Long, maxAgeMillis: Long = 60 * 60 * 1000L): Boolean =
        asOfMillis <= 0L || (nowMillis - asOfMillis) > maxAgeMillis
}

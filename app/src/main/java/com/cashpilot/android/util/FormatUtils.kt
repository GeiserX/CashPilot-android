package com.cashpilot.android.util

import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.util.Currency
import java.util.Locale

/** Format byte counts into human-readable strings. */
object FormatUtils {

    /** The four units [formatBytes] picks from, smallest first. */
    enum class ByteUnit { B, KB, MB, GB }

    /**
     * A byte count with its unit. The number is written the [locale]'s way
     * ("1,5" in German); [label] joins it to the unit, English by default, so
     * the screens can pass the unit in the user's language.
     */
    fun formatBytes(
        bytes: Long,
        locale: Locale = Locale.US,
        label: (String, ByteUnit) -> String = { n, u -> "$n ${u.name}" },
    ): String = when {
        bytes < 1024 -> label(String.format(locale, "%d", bytes), ByteUnit.B)
        bytes < 1024 * 1024 -> label(String.format(locale, "%d", bytes / 1024), ByteUnit.KB)
        bytes < 1024 * 1024 * 1024 -> label(String.format(locale, "%.1f", bytes / (1024.0 * 1024.0)), ByteUnit.MB)
        else -> label(String.format(locale, "%.2f", bytes / (1024.0 * 1024.0 * 1024.0)), ByteUnit.GB)
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
    fun formatPlatformEarnings(usd: Double?, locale: Locale = Locale.US): String =
        if (usd == null) "\u2014" else usdFormat(locale).format(usd)

    /** US dollars written the [locale]'s way: "$4.12" in the US, "4,12 $" in Germany. Always two decimals. */
    private fun usdFormat(locale: Locale): NumberFormat =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("USD")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            // Half up, as String.format did before: 1.125 shows as 1.13, not 1.12.
            roundingMode = RoundingMode.HALF_UP
        }

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

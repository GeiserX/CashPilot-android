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
}

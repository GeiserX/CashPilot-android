package com.cashpilot.android.model

/** In-memory representation of user settings (persisted via DataStore). */
data class Settings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val heartbeatIntervalSeconds: Int = 30,
    val enabledSlugs: Set<String> = KnownApps.all.map { it.slug }.toSet(),
    val setupCompleted: Boolean = false,
)

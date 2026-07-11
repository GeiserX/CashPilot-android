package com.cashpilot.android.model

/** In-memory representation of user settings (persisted via DataStore). */
data class Settings(
    val serverUrl: String = "",
    val apiKey: String = "",
    val heartbeatIntervalSeconds: Int = 30,
    val enabledSlugs: Set<String> = KnownApps.all.map { it.slug }.toSet(),
    val setupCompleted: Boolean = false,
    /**
     * Per-worker fleet key issued by the server on enrollment. Empty until this
     * device has enrolled; once set it is used for auth instead of [apiKey] (the
     * shared key becomes an enrollment-only bootstrap credential).
     *
     * Kept LAST so it does not shift the positional component order used by
     * `Settings` destructuring in tests.
     */
    val workerKey: String = "",
) {
    /** The key to authenticate heartbeats with: our own once enrolled, else the shared bootstrap key. */
    val activeKey: String get() = workerKey.ifBlank { apiKey }
}

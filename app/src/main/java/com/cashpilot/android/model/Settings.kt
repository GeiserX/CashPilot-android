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

    /**
     * The settings with a new server address. A different address drops this
     * phone's own key: it was issued by the old server and must never be sent
     * to the new one.
     */
    fun withServerUrl(url: String): Settings =
        if (url.trim() == serverUrl.trim()) copy(serverUrl = url) else copy(serverUrl = url, workerKey = "")

    /**
     * The settings with a new shared key typed in. A different key drops this
     * phone's own key, so the next report enrolls with the key just typed and a
     * connection test proves that key, not the one issued earlier.
     */
    fun withApiKey(key: String): Settings = if (key.trim() == apiKey.trim()) copy(apiKey = key) else copy(apiKey = key, workerKey = "")
}

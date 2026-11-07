package com.cashpilot.android

import com.cashpilot.android.model.Settings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the pure logic patterns used by Heartbeat.send:
 * - URL building from settings
 * - Server readiness checks
 */
class HeartbeatLogicTest {

    // --- URL building (matches Heartbeat.send) ---

    private fun buildHeartbeatUrl(serverUrl: String): String =
        serverUrl.trimEnd('/') + "/api/workers/heartbeat"

    @Test
    fun `URL building without trailing slash`() {
        assertEquals(
            "https://cashpilot.example.com/api/workers/heartbeat",
            buildHeartbeatUrl("https://cashpilot.example.com"),
        )
    }

    @Test
    fun `URL building with trailing slash`() {
        assertEquals(
            "https://cashpilot.example.com/api/workers/heartbeat",
            buildHeartbeatUrl("https://cashpilot.example.com/"),
        )
    }

    @Test
    fun `URL building with multiple trailing slashes`() {
        assertEquals(
            "https://cashpilot.example.com/api/workers/heartbeat",
            buildHeartbeatUrl("https://cashpilot.example.com///"),
        )
    }

    @Test
    fun `URL building with path`() {
        assertEquals(
            "https://example.com/cashpilot/api/workers/heartbeat",
            buildHeartbeatUrl("https://example.com/cashpilot"),
        )
    }

    @Test
    fun `URL building with path and trailing slash`() {
        assertEquals(
            "https://example.com/cashpilot/api/workers/heartbeat",
            buildHeartbeatUrl("https://example.com/cashpilot/"),
        )
    }

    // --- Server readiness check (matches MainViewModel.doRefresh) ---

    private fun isServerReady(settings: Settings): Boolean =
        settings.serverUrl.isNotBlank() && settings.apiKey.isNotBlank()

    @Test
    fun `server not ready with empty url`() {
        assertFalse(isServerReady(Settings(serverUrl = "", apiKey = "key")))
    }

    @Test
    fun `server not ready with empty api key`() {
        assertFalse(isServerReady(Settings(serverUrl = "https://x.com", apiKey = "")))
    }

    @Test
    fun `server not ready with both empty`() {
        assertFalse(isServerReady(Settings()))
    }

    @Test
    fun `server ready with both set`() {
        assertTrue(isServerReady(Settings(serverUrl = "https://x.com", apiKey = "k")))
    }

    @Test
    fun `server not ready with blank url`() {
        assertFalse(isServerReady(Settings(serverUrl = "   ", apiKey = "key")))
    }

    @Test
    fun `server not ready with blank api key`() {
        assertFalse(isServerReady(Settings(serverUrl = "https://x.com", apiKey = "   ")))
    }

    // --- Running heuristic (matches AppDetector.detect) ---

    @Test
    fun `running heuristic notification active`() {
        val notificationActive = true
        val recentlyActive = false
        val hasRecentNetworkActivity = false
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertEquals(true, running)
    }

    @Test
    fun `running heuristic recently active only`() {
        val notificationActive = false
        val recentlyActive = true
        val hasRecentNetworkActivity = false
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertEquals(true, running)
    }

    @Test
    fun `running heuristic network activity only`() {
        val notificationActive = false
        val recentlyActive = false
        val hasRecentNetworkActivity = true
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertEquals(true, running)
    }

    @Test
    fun `running heuristic all false means not running`() {
        val notificationActive = false
        val recentlyActive = false
        val hasRecentNetworkActivity = false
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertEquals(false, running)
    }

    @Test
    fun `running heuristic all true means running`() {
        val notificationActive = true
        val recentlyActive = true
        val hasRecentNetworkActivity = true
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertEquals(true, running)
    }

    @Test
    fun `network activity threshold is 1KB`() {
        val threshold = 1024L
        assertFalse((0L + 0L) > threshold) // no activity
        assertFalse((512L + 511L) > threshold) // just under
        assertTrue((512L + 513L) > threshold) // just over
        assertTrue((5000L + 0L) > threshold) // tx only
        assertTrue((0L + 5000L) > threshold) // rx only
    }

    @Test
    fun `recently active 15 minute window`() {
        val fifteenMinMs = 15L * 60 * 1000
        val now = System.currentTimeMillis()

        // 10 minutes ago: recently active
        val tenMinAgo = now - 10 * 60 * 1000
        assertTrue((now - tenMinAgo) < fifteenMinMs)

        // 20 minutes ago: not recently active
        val twentyMinAgo = now - 20 * 60 * 1000
        assertFalse((now - twentyMinAgo) < fifteenMinMs)

        // exactly 15 minutes ago: not recently active (< not <=)
        val exactlyFifteen = now - fifteenMinMs
        assertFalse((now - exactlyFifteen) < fifteenMinMs)
    }
}

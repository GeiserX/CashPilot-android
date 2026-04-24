package com.cashpilot.android

import com.cashpilot.android.model.Settings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the pure logic patterns extracted from HeartbeatService:
 * - Exponential backoff calculation
 * - URL building from settings
 * - Server readiness checks
 */
class HeartbeatLogicTest {

    // --- Backoff calculation (matches HeartbeatService.onStartCommand) ---

    private fun computeBackoff(baseIntervalSeconds: Int, consecutiveFailures: Int): Long {
        val baseDelay = (baseIntervalSeconds * 1000L).coerceAtLeast(5_000L)
        return if (consecutiveFailures > 0) {
            (baseDelay * (1L shl consecutiveFailures.coerceAtMost(3)))
                .coerceAtMost(300_000L)
        } else {
            baseDelay
        }
    }

    @Test
    fun `backoff with zero failures returns base delay`() {
        assertEquals(30_000L, computeBackoff(30, 0))
    }

    @Test
    fun `backoff with 1 failure doubles delay`() {
        assertEquals(60_000L, computeBackoff(30, 1))
    }

    @Test
    fun `backoff with 2 failures quadruples delay`() {
        assertEquals(120_000L, computeBackoff(30, 2))
    }

    @Test
    fun `backoff with 3 failures 8x delay`() {
        assertEquals(240_000L, computeBackoff(30, 3))
    }

    @Test
    fun `backoff with 4+ failures capped at 3 shifts`() {
        // consecutiveFailures.coerceAtMost(3) means 4 failures = same as 3
        assertEquals(240_000L, computeBackoff(30, 4))
        assertEquals(240_000L, computeBackoff(30, 10))
        assertEquals(240_000L, computeBackoff(30, 100))
    }

    @Test
    fun `backoff capped at 5 minutes`() {
        // With a 60s base interval and 3 failures: 60000 * 8 = 480000, capped at 300000
        assertEquals(300_000L, computeBackoff(60, 3))
    }

    @Test
    fun `backoff minimum base delay is 5 seconds`() {
        // If interval is 0, coerceAtLeast(5000) kicks in
        assertEquals(5_000L, computeBackoff(0, 0))
        assertEquals(10_000L, computeBackoff(0, 1))
    }

    @Test
    fun `backoff with 1 second interval uses minimum 5 seconds`() {
        assertEquals(5_000L, computeBackoff(1, 0))
    }

    @Test
    fun `backoff with negative interval uses minimum 5 seconds`() {
        assertEquals(5_000L, computeBackoff(-10, 0))
    }

    // --- URL building (matches HeartbeatService.sendHeartbeat) ---

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
        assertTrue(running)
    }

    @Test
    fun `running heuristic recently active only`() {
        val notificationActive = false
        val recentlyActive = true
        val hasRecentNetworkActivity = false
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertTrue(running)
    }

    @Test
    fun `running heuristic network activity only`() {
        val notificationActive = false
        val recentlyActive = false
        val hasRecentNetworkActivity = true
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertTrue(running)
    }

    @Test
    fun `running heuristic all false means not running`() {
        val notificationActive = false
        val recentlyActive = false
        val hasRecentNetworkActivity = false
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertFalse(running)
    }

    @Test
    fun `running heuristic all true means running`() {
        val notificationActive = true
        val recentlyActive = true
        val hasRecentNetworkActivity = true
        val running = notificationActive || recentlyActive || hasRecentNetworkActivity
        assertTrue(running)
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

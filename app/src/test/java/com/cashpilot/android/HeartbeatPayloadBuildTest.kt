package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the full heartbeat payload construction pattern as used by HeartbeatService.sendHeartbeat.
 * This exercises the model classes with realistic data matching production usage.
 */
class HeartbeatPayloadBuildTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Mirrors the container-building logic from HeartbeatService.sendHeartbeat */
    private fun buildContainers(apps: List<AppStatus>): List<AppContainer> =
        apps.map { app ->
            AppContainer(
                slug = app.slug,
                name = "cashpilot-${app.slug}",
                status = if (app.running) "running" else "stopped",
                labels = mapOf(
                    "cashpilot.managed" to "true",
                    "cashpilot.service" to app.slug,
                ),
            )
        }

    /** Mirrors the heartbeat building logic from HeartbeatService.sendHeartbeat */
    private fun buildHeartbeat(
        deviceName: String,
        apps: List<AppStatus>,
    ): WorkerHeartbeat {
        val containers = buildContainers(apps)
        return WorkerHeartbeat(
            name = deviceName,
            containers = containers,
            apps = apps,
            systemInfo = SystemInfo(
                os = "Android",
                arch = "arm64-v8a",
                osVersion = "Android 15 (API 35)",
                deviceType = "android",
            ),
        )
    }

    @Test
    fun `build heartbeat with all known apps running`() {
        val apps = KnownApps.all.map { knownApp ->
            AppStatus(
                slug = knownApp.slug,
                running = true,
                notificationActive = true,
                netTx24h = 1024L * 1024,
                netRx24h = 2048L * 1024,
            )
        }
        val heartbeat = buildHeartbeat("Samsung Galaxy S24 (abc12345)", apps)

        assertEquals(KnownApps.all.size, heartbeat.apps.size)
        assertEquals(KnownApps.all.size, heartbeat.containers.size)
        heartbeat.containers.forEach { container ->
            assertEquals("running", container.status)
            assertTrue(container.name.startsWith("cashpilot-"))
            assertEquals("true", container.labels["cashpilot.managed"])
        }
    }

    @Test
    fun `build heartbeat with all known apps stopped`() {
        val apps = KnownApps.all.map { knownApp ->
            AppStatus(slug = knownApp.slug, running = false)
        }
        val heartbeat = buildHeartbeat("Pixel 9 (xyz98765)", apps)

        heartbeat.containers.forEach { container ->
            assertEquals("stopped", container.status)
        }
        heartbeat.apps.forEach { app ->
            assertFalse(app.running)
            assertFalse(app.notificationActive)
            assertEquals(0L, app.netTx24h)
        }
    }

    @Test
    fun `build heartbeat with mixed running and stopped`() {
        val apps = KnownApps.all.mapIndexed { index, knownApp ->
            AppStatus(
                slug = knownApp.slug,
                running = index % 2 == 0,
                notificationActive = index % 3 == 0,
                netTx24h = if (index % 2 == 0) 5000L else 0L,
                netRx24h = if (index % 2 == 0) 10000L else 0L,
            )
        }
        val heartbeat = buildHeartbeat("OnePlus 12 (def45678)", apps)

        val runningCount = heartbeat.containers.count { it.status == "running" }
        val stoppedCount = heartbeat.containers.count { it.status == "stopped" }
        assertEquals(heartbeat.containers.size, runningCount + stoppedCount)
        assertTrue(runningCount > 0)
        assertTrue(stoppedCount > 0)
    }

    @Test
    fun `build heartbeat with empty app list`() {
        val heartbeat = buildHeartbeat("Empty Device (000)", emptyList())
        assertTrue(heartbeat.containers.isEmpty())
        assertTrue(heartbeat.apps.isEmpty())
        assertEquals("Android", heartbeat.systemInfo.os)
    }

    @Test
    fun `build heartbeat with single app`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true, notificationActive = true, netTx24h = 999, netRx24h = 1999),
        )
        val heartbeat = buildHeartbeat("Single App Device (aaa)", apps)
        assertEquals(1, heartbeat.containers.size)
        assertEquals("earnapp", heartbeat.containers[0].slug)
        assertEquals("cashpilot-earnapp", heartbeat.containers[0].name)
        assertEquals("running", heartbeat.containers[0].status)
    }

    @Test
    fun `built heartbeat serializes to valid JSON`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true, notificationActive = true, netTx24h = 5000, netRx24h = 10000),
            AppStatus(slug = "iproyal", running = false),
            AppStatus(slug = "grass", running = true, netTx24h = 3000),
        )
        val heartbeat = buildHeartbeat("Test Device (bbb)", apps)
        val encoded = json.encodeToString(heartbeat)

        // Verify JSON structure
        assertTrue(encoded.contains("\"name\":\"Test Device (bbb)\""))
        assertTrue(encoded.contains("\"system_info\""))
        assertTrue(encoded.contains("\"containers\""))
        assertTrue(encoded.contains("\"apps\""))
        assertTrue(encoded.contains("\"cashpilot.managed\""))
        assertTrue(encoded.contains("\"net_tx_24h\""))

        // Verify round-trip
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals(heartbeat, decoded)
    }

    @Test
    fun `URL building from settings`() {
        val urls = listOf(
            "https://cashpilot.example.com" to "https://cashpilot.example.com/api/workers/heartbeat",
            "https://cashpilot.example.com/" to "https://cashpilot.example.com/api/workers/heartbeat",
            "https://cashpilot.example.com///" to "https://cashpilot.example.com/api/workers/heartbeat",
            "http://localhost:3000" to "http://localhost:3000/api/workers/heartbeat",
            "https://example.com/cashpilot" to "https://example.com/cashpilot/api/workers/heartbeat",
        )
        urls.forEach { (input, expected) ->
            val result = input.trimEnd('/') + "/api/workers/heartbeat"
            assertEquals(expected, result, "URL building failed for input: $input")
        }
    }

    @Test
    fun `container labels always contain required keys`() {
        val apps = KnownApps.all.map { AppStatus(slug = it.slug, running = true) }
        val containers = buildContainers(apps)

        containers.forEach { container ->
            assertTrue(container.labels.containsKey("cashpilot.managed"), "Missing cashpilot.managed for ${container.slug}")
            assertTrue(container.labels.containsKey("cashpilot.service"), "Missing cashpilot.service for ${container.slug}")
            assertEquals("true", container.labels["cashpilot.managed"])
            assertEquals(container.slug, container.labels["cashpilot.service"])
        }
    }

    @Test
    fun `settings server readiness for heartbeat`() {
        // Both set = ready
        assertTrue(isReady(Settings(serverUrl = "https://x.com", apiKey = "key")))
        // Missing URL
        assertFalse(isReady(Settings(serverUrl = "", apiKey = "key")))
        // Missing key
        assertFalse(isReady(Settings(serverUrl = "https://x.com", apiKey = "")))
        // Both empty
        assertFalse(isReady(Settings()))
        // Whitespace-only
        assertFalse(isReady(Settings(serverUrl = "   ", apiKey = "key")))
        assertFalse(isReady(Settings(serverUrl = "https://x.com", apiKey = "  ")))
    }

    private fun isReady(s: Settings): Boolean =
        s.serverUrl.isNotBlank() && s.apiKey.isNotBlank()

    @Test
    fun `enabled slugs filtering mirrors detectAll pattern`() {
        val enabledSlugs = setOf("earnapp", "iproyal", "grass")
        val filtered = KnownApps.all
            .filter { it.slug in enabledSlugs }

        assertEquals(3, filtered.size)
        assertTrue(filtered.all { it.slug in enabledSlugs })
    }

    @Test
    fun `enabled slugs with empty set returns no apps`() {
        val filtered = KnownApps.all.filter { it.slug in emptySet<String>() }
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `enabled slugs with all slugs returns all apps`() {
        val allSlugs = KnownApps.all.map { it.slug }.toSet()
        val filtered = KnownApps.all.filter { it.slug in allSlugs }
        assertEquals(KnownApps.all.size, filtered.size)
    }

    @Test
    fun `running count matches HeartbeatService notification format`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true),
            AppStatus(slug = "iproyal", running = true),
            AppStatus(slug = "grass", running = false),
            AppStatus(slug = "titan", running = true),
            AppStatus(slug = "nodle", running = false),
        )
        val runningCount = apps.count { it.running }
        val notificationText = "$runningCount/${apps.size} apps running"
        assertEquals("3/5 apps running", notificationText)
    }
}

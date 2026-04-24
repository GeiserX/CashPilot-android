package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.FleetSummary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the data class contract: destructuring (componentN), copy, toString, hashCode, equals.
 * Jacoco tracks coverage of these generated methods.
 */
class DataClassContractTest {

    // --- AppStatus destructuring ---

    @Test
    fun `AppStatus destructuring`() {
        val status = AppStatus(
            slug = "earnapp",
            running = true,
            notificationActive = true,
            netTx24h = 500L,
            netRx24h = 1000L,
            lastActive = "2026-01-01T00:00:00Z",
        )
        val (slug, running, notifActive, tx, rx, lastActive) = status
        assertEquals("earnapp", slug)
        assertTrue(running)
        assertTrue(notifActive)
        assertEquals(500L, tx)
        assertEquals(1000L, rx)
        assertEquals("2026-01-01T00:00:00Z", lastActive)
    }

    @Test
    fun `AppStatus destructuring with defaults`() {
        val status = AppStatus(slug = "test", running = false)
        val (slug, running, notifActive, tx, rx, lastActive) = status
        assertEquals("test", slug)
        assertFalse(running)
        assertFalse(notifActive)
        assertEquals(0L, tx)
        assertEquals(0L, rx)
        assertNull(lastActive)
    }

    // --- AppContainer destructuring ---

    @Test
    fun `AppContainer destructuring`() {
        val container = AppContainer(
            slug = "iproyal",
            name = "cashpilot-iproyal",
            status = "running",
            image = "iproyal:1.0",
            labels = mapOf("k" to "v"),
        )
        val (slug, name, status, image, labels) = container
        assertEquals("iproyal", slug)
        assertEquals("cashpilot-iproyal", name)
        assertEquals("running", status)
        assertEquals("iproyal:1.0", image)
        assertEquals(mapOf("k" to "v"), labels)
    }

    @Test
    fun `AppContainer destructuring with defaults`() {
        val container = AppContainer(slug = "s", name = "n", status = "stopped")
        val (slug, name, status, image, labels) = container
        assertEquals("s", slug)
        assertEquals("n", name)
        assertEquals("stopped", status)
        assertEquals("", image)
        assertTrue(labels.isEmpty())
    }

    // --- SystemInfo destructuring ---

    @Test
    fun `SystemInfo destructuring`() {
        val info = SystemInfo(
            os = "Android",
            arch = "arm64-v8a",
            osVersion = "Android 15",
            deviceType = "android",
            apps = listOf(AppStatus(slug = "test", running = true)),
        )
        val (os, arch, osVersion, deviceType, apps) = info
        assertEquals("Android", os)
        assertEquals("arm64-v8a", arch)
        assertEquals("Android 15", osVersion)
        assertEquals("android", deviceType)
        assertEquals(1, apps.size)
    }

    @Test
    fun `SystemInfo destructuring with defaults`() {
        val info = SystemInfo()
        val (os, arch, osVersion, deviceType, apps) = info
        assertEquals("", os)
        assertEquals("", arch)
        assertEquals("", osVersion)
        assertEquals("android", deviceType)
        assertTrue(apps.isEmpty())
    }

    // --- WorkerHeartbeat destructuring ---

    @Test
    fun `WorkerHeartbeat destructuring`() {
        val hb = WorkerHeartbeat(
            name = "device",
            url = "https://example.com",
            containers = listOf(AppContainer(slug = "s", name = "n", status = "running")),
            apps = listOf(AppStatus(slug = "s", running = true)),
            systemInfo = SystemInfo(os = "Android"),
        )
        val (name, url, containers, apps, systemInfo) = hb
        assertEquals("device", name)
        assertEquals("https://example.com", url)
        assertEquals(1, containers.size)
        assertEquals(1, apps.size)
        assertEquals("Android", systemInfo.os)
    }

    @Test
    fun `WorkerHeartbeat destructuring with defaults`() {
        val hb = WorkerHeartbeat(name = "test")
        val (name, url, containers, apps, systemInfo) = hb
        assertEquals("test", name)
        assertEquals("", url)
        assertTrue(containers.isEmpty())
        assertTrue(apps.isEmpty())
        assertEquals("android", systemInfo.deviceType)
    }

    // --- MonitoredApp destructuring ---

    @Test
    fun `MonitoredApp destructuring with referral`() {
        val app = MonitoredApp("earnapp", "com.brd.earnapp.play", "EarnApp", "https://ref.com")
        val (slug, pkg, display, referral) = app
        assertEquals("earnapp", slug)
        assertEquals("com.brd.earnapp.play", pkg)
        assertEquals("EarnApp", display)
        assertEquals("https://ref.com", referral)
    }

    @Test
    fun `MonitoredApp destructuring without referral`() {
        val app = MonitoredApp("mysterium", "network.mysterium.provider", "MystNodes")
        val (slug, pkg, display, referral) = app
        assertEquals("mysterium", slug)
        assertEquals("network.mysterium.provider", pkg)
        assertEquals("MystNodes", display)
        assertNull(referral)
    }

    // --- Settings destructuring ---

    @Test
    fun `Settings destructuring`() {
        val settings = Settings(
            serverUrl = "https://example.com",
            apiKey = "key123",
            heartbeatIntervalSeconds = 60,
            enabledSlugs = setOf("earnapp", "iproyal"),
            setupCompleted = true,
        )
        val (url, key, interval, slugs, setup) = settings
        assertEquals("https://example.com", url)
        assertEquals("key123", key)
        assertEquals(60, interval)
        assertEquals(setOf("earnapp", "iproyal"), slugs)
        assertTrue(setup)
    }

    @Test
    fun `Settings destructuring with defaults`() {
        val settings = Settings()
        val (url, key, interval, slugs, setup) = settings
        assertEquals("", url)
        assertEquals("", key)
        assertEquals(30, interval)
        assertTrue(slugs.isNotEmpty()) // all KnownApps enabled
        assertFalse(setup)
    }

    // --- FleetSummary destructuring ---

    @Test
    fun `FleetSummary destructuring`() {
        val summary = FleetSummary(
            running = 3,
            stopped = 2,
            notInstalled = 5,
            disabled = 1,
            totalTx = 999L,
            totalRx = 888L,
        )
        val (running, stopped, notInstalled, disabled, tx, rx) = summary
        assertEquals(3, running)
        assertEquals(2, stopped)
        assertEquals(5, notInstalled)
        assertEquals(1, disabled)
        assertEquals(999L, tx)
        assertEquals(888L, rx)
    }

    // --- AppDisplayInfo destructuring ---

    @Test
    fun `AppDisplayInfo destructuring`() {
        val app = MonitoredApp("s", "p", "d")
        val status = AppStatus(slug = "s", running = true)
        val info = AppDisplayInfo(app = app, state = AppState.RUNNING, status = status)
        val (appField, state, statusField) = info
        assertEquals(app, appField)
        assertEquals(AppState.RUNNING, state)
        assertEquals(status, statusField)
    }

    @Test
    fun `AppDisplayInfo destructuring with null status`() {
        val app = MonitoredApp("s", "p", "d")
        val info = AppDisplayInfo(app = app, state = AppState.NOT_INSTALLED)
        val (appField, state, statusField) = info
        assertEquals(app, appField)
        assertEquals(AppState.NOT_INSTALLED, state)
        assertNull(statusField)
    }

    // --- Cross-class equality and toString ---

    @Test
    fun `different data classes with same field values are not equal`() {
        // Just ensure no accidental cross-class equality
        val status = AppStatus(slug = "test", running = true)
        val container = AppContainer(slug = "test", name = "test", status = "true")
        assertNotEquals(status as Any, container as Any)
    }

    @Test
    fun `all model classes produce meaningful toString`() {
        val status = AppStatus(slug = "s", running = true)
        val container = AppContainer(slug = "s", name = "n", status = "running")
        val info = SystemInfo(os = "Android")
        val heartbeat = WorkerHeartbeat(name = "device")
        val app = MonitoredApp("s", "p", "d")
        val settings = Settings(serverUrl = "https://x.com")
        val fleet = FleetSummary(running = 1)
        val display = AppDisplayInfo(app = app, state = AppState.RUNNING)

        // Each toString should contain the class name (Kotlin data class convention)
        assertTrue(status.toString().contains("AppStatus"))
        assertTrue(container.toString().contains("AppContainer"))
        assertTrue(info.toString().contains("SystemInfo"))
        assertTrue(heartbeat.toString().contains("WorkerHeartbeat"))
        assertTrue(app.toString().contains("MonitoredApp"))
        assertTrue(settings.toString().contains("Settings"))
        assertTrue(fleet.toString().contains("FleetSummary"))
        assertTrue(display.toString().contains("AppDisplayInfo"))
    }

    @Test
    fun `hashCode differs for different objects`() {
        val a = AppStatus(slug = "a", running = true)
        val b = AppStatus(slug = "b", running = false)
        // Not guaranteed by contract, but extremely likely for different data
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy creates new independent instance`() {
        val original = AppStatus(slug = "test", running = true, netTx24h = 100)
        val copy = original.copy()
        assertEquals(original, copy)
        assertNotSame(original, copy)
    }
}

package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    // --- WorkerHeartbeat ---

    @Test
    fun `WorkerHeartbeat serializes name correctly`() {
        val hb = WorkerHeartbeat(name = "Pixel 8")
        val encoded = json.encodeToString(hb)
        assertTrue(encoded.contains("\"name\":\"Pixel 8\""))
    }

    @Test
    fun `WorkerHeartbeat round-trip with defaults`() {
        val original = WorkerHeartbeat(name = "test-device")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WorkerHeartbeat round-trip with full payload`() {
        val original = WorkerHeartbeat(
            name = "Samsung Galaxy S24",
            url = "https://example.com",
            containers = listOf(
                AppContainer(slug = "earnapp", name = "cashpilot-earnapp", status = "running"),
            ),
            apps = listOf(
                AppStatus(slug = "earnapp", running = true, notificationActive = true),
            ),
            systemInfo = SystemInfo(os = "Android", arch = "arm64-v8a", osVersion = "Android 14"),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `WorkerHeartbeat uses SerialName for system_info`() {
        val hb = WorkerHeartbeat(name = "test")
        val encoded = json.encodeToString(hb)
        assertTrue(encoded.contains("\"system_info\""))
        assertFalse(encoded.contains("\"systemInfo\""))
    }

    @Test
    fun `WorkerHeartbeat decodes with unknown keys`() {
        val jsonStr = """{"name":"test","unknown_field":"value","containers":[],"apps":[],"system_info":{"os":"","arch":"","os_version":"","device_type":"android","apps":[]}}"""
        val decoded = json.decodeFromString<WorkerHeartbeat>(jsonStr)
        assertEquals("test", decoded.name)
    }

    // --- AppContainer ---

    @Test
    fun `AppContainer round-trip`() {
        val original = AppContainer(
            slug = "iproyal",
            name = "cashpilot-iproyal",
            status = "running",
            image = "iproyal:latest",
            labels = mapOf("cashpilot.managed" to "true"),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppContainer>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `AppContainer defaults serialize correctly`() {
        val container = AppContainer(slug = "test", name = "test", status = "stopped")
        val encoded = json.encodeToString(container)
        assertTrue(encoded.contains("\"image\":\"\""))
    }

    @Test
    fun `AppContainer with empty labels`() {
        val container = AppContainer(slug = "s", name = "n", status = "running")
        val encoded = json.encodeToString(container)
        val decoded = json.decodeFromString<AppContainer>(encoded)
        assertTrue(decoded.labels.isEmpty())
    }

    @Test
    fun `AppContainer with multiple labels`() {
        val labels = mapOf("key1" to "val1", "key2" to "val2", "key3" to "val3")
        val container = AppContainer(slug = "s", name = "n", status = "running", labels = labels)
        val encoded = json.encodeToString(container)
        val decoded = json.decodeFromString<AppContainer>(encoded)
        assertEquals(3, decoded.labels.size)
        assertEquals("val2", decoded.labels["key2"])
    }

    // --- SystemInfo ---

    @Test
    fun `SystemInfo round-trip`() {
        val original = SystemInfo(
            os = "Android",
            arch = "arm64-v8a",
            osVersion = "Android 14 (API 34)",
            deviceType = "android",
            apps = listOf(AppStatus(slug = "earnapp", running = true)),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SystemInfo>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `SystemInfo uses SerialName for os_version and device_type`() {
        val info = SystemInfo(osVersion = "Android 14", deviceType = "android")
        val encoded = json.encodeToString(info)
        assertTrue(encoded.contains("\"os_version\""))
        assertTrue(encoded.contains("\"device_type\""))
        assertFalse(encoded.contains("\"osVersion\""))
        assertFalse(encoded.contains("\"deviceType\""))
    }

    @Test
    fun `SystemInfo defaults round-trip`() {
        val original = SystemInfo()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SystemInfo>(encoded)
        assertEquals(original, decoded)
    }

    // --- AppStatus ---

    @Test
    fun `AppStatus round-trip running`() {
        val original = AppStatus(
            slug = "earnapp",
            running = true,
            notificationActive = true,
            netTx24h = 1024000,
            netRx24h = 5120000,
            lastActive = "2024-01-15T10:30:00Z",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `AppStatus round-trip stopped with defaults`() {
        val original = AppStatus(slug = "test", running = false)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertEquals(original, decoded)
        assertNull(decoded.lastActive)
        assertEquals(0L, decoded.netTx24h)
        assertEquals(0L, decoded.netRx24h)
    }

    @Test
    fun `AppStatus uses SerialName for snake_case fields`() {
        val status = AppStatus(
            slug = "test",
            running = true,
            notificationActive = true,
            netTx24h = 100,
            netRx24h = 200,
            lastActive = "2024-01-01T00:00:00Z",
        )
        val encoded = json.encodeToString(status)
        assertTrue(encoded.contains("\"notification_active\""))
        assertTrue(encoded.contains("\"net_tx_24h\""))
        assertTrue(encoded.contains("\"net_rx_24h\""))
        assertTrue(encoded.contains("\"last_active\""))
    }

    @Test
    fun `AppStatus null lastActive serializes`() {
        val status = AppStatus(slug = "test", running = false, lastActive = null)
        val encoded = json.encodeToString(status)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertNull(decoded.lastActive)
    }

    // --- MonitoredApp ---

    @Test
    fun `MonitoredApp round-trip with referral`() {
        val original = MonitoredApp(
            slug = "earnapp",
            packageName = "com.brd.earnapp.play",
            displayName = "EarnApp",
            referralUrl = "https://earnapp.com/i/TSMD9wSm",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonitoredApp>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `MonitoredApp round-trip without referral`() {
        val original = MonitoredApp(
            slug = "mysterium",
            packageName = "network.mysterium.provider",
            displayName = "MystNodes",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonitoredApp>(encoded)
        assertEquals(original, decoded)
        assertNull(decoded.referralUrl)
    }

    @Test
    fun `MonitoredApp null referralUrl serializes and deserializes`() {
        val app = MonitoredApp("s", "p", "d", referralUrl = null)
        val encoded = json.encodeToString(app)
        val decoded = json.decodeFromString<MonitoredApp>(encoded)
        assertNull(decoded.referralUrl)
    }

    // --- Cross-model integration ---

    @Test
    fun `full heartbeat payload serialization matches server expectations`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true, notificationActive = true, netTx24h = 5000, netRx24h = 10000),
            AppStatus(slug = "iproyal", running = false),
        )
        val containers = apps.map {
            AppContainer(
                slug = it.slug,
                name = "cashpilot-${it.slug}",
                status = if (it.running) "running" else "stopped",
                labels = mapOf("cashpilot.managed" to "true", "cashpilot.service" to it.slug),
            )
        }
        val heartbeat = WorkerHeartbeat(
            name = "Samsung Galaxy S24 (abc12345)",
            containers = containers,
            apps = apps,
            systemInfo = SystemInfo(
                os = "Android",
                arch = "arm64-v8a",
                osVersion = "Android 14 (API 34)",
                deviceType = "android",
            ),
        )
        val encoded = json.encodeToString(heartbeat)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)

        assertEquals(heartbeat.name, decoded.name)
        assertEquals(2, decoded.containers.size)
        assertEquals(2, decoded.apps.size)
        assertEquals("running", decoded.containers[0].status)
        assertEquals("stopped", decoded.containers[1].status)
        assertTrue(decoded.apps[0].running)
        assertFalse(decoded.apps[1].running)
        assertEquals("Android", decoded.systemInfo.os)
    }
}

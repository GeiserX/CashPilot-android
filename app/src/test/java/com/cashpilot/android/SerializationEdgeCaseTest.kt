package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.model.MonitoredApp
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests serialization edge cases: malformed JSON, missing required fields,
 * extra fields, empty collections, and boundary values.
 */
class SerializationEdgeCaseTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val strictJson = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    // --- Decoding with missing optional fields ---

    @Test
    fun `AppStatus decodes with only required fields`() {
        val jsonStr = """{"slug":"test","running":true}"""
        val status = json.decodeFromString<AppStatus>(jsonStr)
        assertEquals("test", status.slug)
        assertTrue(status.running)
        assertFalse(status.notificationActive)
        assertEquals(0L, status.netTx24h)
        assertEquals(0L, status.netRx24h)
        assertNull(status.lastActive)
    }

    @Test
    fun `AppContainer decodes with only required fields`() {
        val jsonStr = """{"slug":"test","name":"n","status":"running"}"""
        val container = json.decodeFromString<AppContainer>(jsonStr)
        assertEquals("test", container.slug)
        assertEquals("", container.image)
        assertTrue(container.labels.isEmpty())
    }

    @Test
    fun `SystemInfo decodes with only required defaults`() {
        val jsonStr = """{"os":"","arch":"","os_version":"","device_type":"android","apps":[]}"""
        val info = json.decodeFromString<SystemInfo>(jsonStr)
        assertEquals("android", info.deviceType)
        assertTrue(info.apps.isEmpty())
    }

    @Test
    fun `WorkerHeartbeat decodes with minimal JSON`() {
        val jsonStr = """{"name":"dev","url":"","containers":[],"apps":[],"system_info":{"os":"","arch":"","os_version":"","device_type":"android","apps":[]}}"""
        val hb = json.decodeFromString<WorkerHeartbeat>(jsonStr)
        assertEquals("dev", hb.name)
        assertTrue(hb.containers.isEmpty())
    }

    // --- Decoding with extra unknown fields ---

    @Test
    fun `AppStatus ignores unknown fields`() {
        val jsonStr = """{"slug":"test","running":false,"unknown_int":42,"unknown_str":"hello"}"""
        val status = json.decodeFromString<AppStatus>(jsonStr)
        assertEquals("test", status.slug)
        assertFalse(status.running)
    }

    @Test
    fun `AppContainer ignores unknown fields`() {
        val jsonStr = """{"slug":"s","name":"n","status":"running","extra_field":true}"""
        val container = json.decodeFromString<AppContainer>(jsonStr)
        assertEquals("s", container.slug)
    }

    @Test
    fun `SystemInfo ignores unknown fields`() {
        val jsonStr = """{"os":"Linux","arch":"x86","os_version":"v1","device_type":"docker","apps":[],"memory":"16GB"}"""
        val info = json.decodeFromString<SystemInfo>(jsonStr)
        assertEquals("Linux", info.os)
        assertEquals("docker", info.deviceType)
    }

    // --- Strict JSON rejects unknown fields ---

    @Test
    fun `strict json rejects unknown fields in AppStatus`() {
        val jsonStr = """{"slug":"test","running":false,"bad_field":1}"""
        assertThrows<SerializationException> {
            strictJson.decodeFromString<AppStatus>(jsonStr)
        }
    }

    // --- Missing required fields ---

    @Test
    fun `AppStatus missing slug throws`() {
        val jsonStr = """{"running":true}"""
        assertThrows<SerializationException> {
            json.decodeFromString<AppStatus>(jsonStr)
        }
    }

    @Test
    fun `AppStatus missing running throws`() {
        val jsonStr = """{"slug":"test"}"""
        assertThrows<SerializationException> {
            json.decodeFromString<AppStatus>(jsonStr)
        }
    }

    @Test
    fun `AppContainer missing slug throws`() {
        val jsonStr = """{"name":"n","status":"running"}"""
        assertThrows<SerializationException> {
            json.decodeFromString<AppContainer>(jsonStr)
        }
    }

    @Test
    fun `AppContainer missing name throws`() {
        val jsonStr = """{"slug":"s","status":"running"}"""
        assertThrows<SerializationException> {
            json.decodeFromString<AppContainer>(jsonStr)
        }
    }

    @Test
    fun `AppContainer missing status throws`() {
        val jsonStr = """{"slug":"s","name":"n"}"""
        assertThrows<SerializationException> {
            json.decodeFromString<AppContainer>(jsonStr)
        }
    }

    @Test
    fun `WorkerHeartbeat missing name throws`() {
        val jsonStr = """{"url":"","containers":[],"apps":[]}"""
        assertThrows<SerializationException> {
            json.decodeFromString<WorkerHeartbeat>(jsonStr)
        }
    }

    // --- Boundary values ---

    @Test
    fun `AppStatus with negative network values round-trips`() {
        val original = AppStatus(slug = "test", running = true, netTx24h = -1, netRx24h = -100)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertEquals(-1L, decoded.netTx24h)
        assertEquals(-100L, decoded.netRx24h)
    }

    @Test
    fun `AppStatus with empty slug round-trips`() {
        val original = AppStatus(slug = "", running = false)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertEquals("", decoded.slug)
    }

    @Test
    fun `AppContainer with very long name round-trips`() {
        val longName = "a".repeat(10000)
        val original = AppContainer(slug = "s", name = longName, status = "running")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppContainer>(encoded)
        assertEquals(longName, decoded.name)
    }

    @Test
    fun `WorkerHeartbeat with unicode name round-trips`() {
        val original = WorkerHeartbeat(name = "Samsung \uD83D\uDCF1 Galaxy")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals("Samsung \uD83D\uDCF1 Galaxy", decoded.name)
    }

    @Test
    fun `AppStatus with lastActive round-trips`() {
        val original = AppStatus(
            slug = "earnapp",
            running = true,
            lastActive = "2026-04-25T12:00:00.123Z",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStatus>(encoded)
        assertEquals("2026-04-25T12:00:00.123Z", decoded.lastActive)
    }

    @Test
    fun `SystemInfo with non-android deviceType round-trips`() {
        val original = SystemInfo(deviceType = "docker", os = "Linux")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SystemInfo>(encoded)
        assertEquals("docker", decoded.deviceType)
    }

    @Test
    fun `AppContainer with special characters in labels round-trips`() {
        val labels = mapOf(
            "key/with/slashes" to "value=with=equals",
            "key.with.dots" to "value with spaces",
            "empty" to "",
        )
        val original = AppContainer(slug = "s", name = "n", status = "running", labels = labels)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppContainer>(encoded)
        assertEquals(3, decoded.labels.size)
        assertEquals("value=with=equals", decoded.labels["key/with/slashes"])
        assertEquals("", decoded.labels["empty"])
    }

    // --- MonitoredApp serialization ---

    @Test
    fun `MonitoredApp with empty strings round-trips`() {
        val original = MonitoredApp(slug = "", packageName = "", displayName = "")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonitoredApp>(encoded)
        assertEquals("", decoded.slug)
        assertEquals("", decoded.packageName)
        assertEquals("", decoded.displayName)
        assertNull(decoded.referralUrl)
    }

    @Test
    fun `MonitoredApp with empty referralUrl string round-trips`() {
        val original = MonitoredApp("s", "p", "d", referralUrl = "")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<MonitoredApp>(encoded)
        assertEquals("", decoded.referralUrl)
    }

    // --- Complex nested structures ---

    @Test
    fun `WorkerHeartbeat with nested apps in SystemInfo round-trips`() {
        val systemApps = listOf(
            AppStatus(slug = "earnapp", running = true, notificationActive = true, netTx24h = 500),
            AppStatus(slug = "iproyal", running = false, lastActive = "2026-01-01T00:00:00Z"),
        )
        val original = WorkerHeartbeat(
            name = "device",
            systemInfo = SystemInfo(
                os = "Android",
                arch = "arm64-v8a",
                osVersion = "Android 15 (API 35)",
                deviceType = "android",
                apps = systemApps,
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals(2, decoded.systemInfo.apps.size)
        assertTrue(decoded.systemInfo.apps[0].running)
        assertFalse(decoded.systemInfo.apps[1].running)
        assertEquals("2026-01-01T00:00:00Z", decoded.systemInfo.apps[1].lastActive)
    }

    @Test
    fun `WorkerHeartbeat with large number of containers round-trips`() {
        val containers = (1..100).map {
            AppContainer(
                slug = "app-$it",
                name = "cashpilot-app-$it",
                status = if (it % 2 == 0) "running" else "stopped",
                labels = mapOf("index" to "$it"),
            )
        }
        val original = WorkerHeartbeat(name = "stress-test", containers = containers)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WorkerHeartbeat>(encoded)
        assertEquals(100, decoded.containers.size)
        assertEquals("running", decoded.containers[1].status) // index 2, even
        assertEquals("stopped", decoded.containers[0].status) // index 1, odd
    }
}

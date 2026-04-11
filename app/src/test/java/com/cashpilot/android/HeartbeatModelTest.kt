package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HeartbeatModelTest {

    @Test
    fun `worker heartbeat defaults`() {
        val hb = WorkerHeartbeat(name = "test-worker")
        assertEquals("test-worker", hb.name)
        assertEquals("", hb.url)
        assertTrue(hb.containers.isEmpty())
        assertTrue(hb.apps.isEmpty())
        assertEquals("android", hb.systemInfo.deviceType)
    }

    @Test
    fun `app container with running status`() {
        val container = AppContainer(
            slug = "earnapp",
            name = "cashpilot-earnapp",
            status = "running",
        )
        assertEquals("earnapp", container.slug)
        assertEquals("running", container.status)
        assertEquals("", container.image)
        assertTrue(container.labels.isEmpty())
    }

    @Test
    fun `app container with labels`() {
        val container = AppContainer(
            slug = "iproyal",
            name = "cashpilot-iproyal",
            status = "stopped",
            labels = mapOf("cashpilot.managed" to "true", "cashpilot.service" to "iproyal"),
        )
        assertEquals("true", container.labels["cashpilot.managed"])
        assertEquals("iproyal", container.labels["cashpilot.service"])
    }

    @Test
    fun `system info defaults`() {
        val info = SystemInfo()
        assertEquals("", info.os)
        assertEquals("", info.arch)
        assertEquals("", info.osVersion)
        assertEquals("android", info.deviceType)
        assertTrue(info.apps.isEmpty())
    }

    @Test
    fun `system info with values`() {
        val info = SystemInfo(
            os = "Android",
            arch = "arm64-v8a",
            osVersion = "Android 14 (API 34)",
        )
        assertEquals("Android", info.os)
        assertEquals("arm64-v8a", info.arch)
    }

    @Test
    fun `app status running`() {
        val status = AppStatus(
            slug = "earnapp",
            running = true,
            notificationActive = true,
            netTx24h = 1024000,
            netRx24h = 5120000,
        )
        assertTrue(status.running)
        assertTrue(status.notificationActive)
        assertEquals(1024000, status.netTx24h)
    }

    @Test
    fun `app status stopped`() {
        val status = AppStatus(
            slug = "traffmonetizer",
            running = false,
        )
        assertFalse(status.running)
        assertFalse(status.notificationActive)
        assertEquals(0, status.netTx24h)
        assertEquals(0, status.netRx24h)
        assertNull(status.lastActive)
    }

    @Test
    fun `heartbeat with full payload`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true),
            AppStatus(slug = "iproyal", running = false),
        )
        val containers = apps.map {
            AppContainer(
                slug = it.slug,
                name = "cashpilot-${it.slug}",
                status = if (it.running) "running" else "stopped",
            )
        }
        val hb = WorkerHeartbeat(
            name = "Pixel 8",
            containers = containers,
            apps = apps,
            systemInfo = SystemInfo(os = "Android", arch = "arm64-v8a"),
        )
        assertEquals(2, hb.containers.size)
        assertEquals(2, hb.apps.size)
        assertEquals("running", hb.containers[0].status)
        assertEquals("stopped", hb.containers[1].status)
    }
}

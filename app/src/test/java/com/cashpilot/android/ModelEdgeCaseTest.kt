package com.cashpilot.android

import com.cashpilot.android.model.AppContainer
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.FleetSummary
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelEdgeCaseTest {

    // --- AppStatus edge cases ---

    @Test
    fun `AppStatus with max Long network values`() {
        val status = AppStatus(
            slug = "test",
            running = true,
            netTx24h = Long.MAX_VALUE,
            netRx24h = Long.MAX_VALUE,
        )
        assertEquals(Long.MAX_VALUE, status.netTx24h)
        assertEquals(Long.MAX_VALUE, status.netRx24h)
    }

    @Test
    fun `AppStatus with zero network values`() {
        val status = AppStatus(slug = "test", running = true)
        assertEquals(0L, status.netTx24h)
        assertEquals(0L, status.netRx24h)
    }

    @Test
    fun `AppStatus data class copy modifies only specified fields`() {
        val original = AppStatus(
            slug = "earnapp",
            running = true,
            notificationActive = true,
            netTx24h = 500,
            netRx24h = 1000,
            lastActive = "2024-01-15T10:00:00Z",
        )
        val modified = original.copy(running = false)
        assertFalse(modified.running)
        assertTrue(modified.notificationActive) // not changed
        assertEquals(500, modified.netTx24h)
        assertEquals("2024-01-15T10:00:00Z", modified.lastActive)
    }

    @Test
    fun `AppStatus equality`() {
        val a = AppStatus(slug = "test", running = true, netTx24h = 100)
        val b = AppStatus(slug = "test", running = true, netTx24h = 100)
        assertEquals(a, b)
    }

    @Test
    fun `AppStatus inequality on different slug`() {
        val a = AppStatus(slug = "test1", running = true)
        val b = AppStatus(slug = "test2", running = true)
        assertNotEquals(a, b)
    }

    @Test
    fun `AppStatus inequality on running flag`() {
        val a = AppStatus(slug = "test", running = true)
        val b = AppStatus(slug = "test", running = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `AppStatus hashCode consistent with equals`() {
        val a = AppStatus(slug = "test", running = true, netTx24h = 100)
        val b = AppStatus(slug = "test", running = true, netTx24h = 100)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `AppStatus toString contains slug`() {
        val status = AppStatus(slug = "earnapp", running = true)
        assertTrue(status.toString().contains("earnapp"))
    }

    // --- AppContainer edge cases ---

    @Test
    fun `AppContainer equality`() {
        val a = AppContainer(slug = "s", name = "n", status = "running")
        val b = AppContainer(slug = "s", name = "n", status = "running")
        assertEquals(a, b)
    }

    @Test
    fun `AppContainer inequality on status`() {
        val a = AppContainer(slug = "s", name = "n", status = "running")
        val b = AppContainer(slug = "s", name = "n", status = "stopped")
        assertNotEquals(a, b)
    }

    @Test
    fun `AppContainer copy preserves labels`() {
        val original = AppContainer(
            slug = "s",
            name = "n",
            status = "running",
            labels = mapOf("key" to "value"),
        )
        val copy = original.copy(status = "stopped")
        assertEquals("stopped", copy.status)
        assertEquals(mapOf("key" to "value"), copy.labels)
    }

    @Test
    fun `AppContainer toString contains slug`() {
        val container = AppContainer(slug = "myslug", name = "n", status = "running")
        assertTrue(container.toString().contains("myslug"))
    }

    // --- WorkerHeartbeat edge cases ---

    @Test
    fun `WorkerHeartbeat with empty containers and apps`() {
        val hb = WorkerHeartbeat(name = "device")
        assertTrue(hb.containers.isEmpty())
        assertTrue(hb.apps.isEmpty())
    }

    @Test
    fun `WorkerHeartbeat with many containers`() {
        val containers = (1..20).map {
            AppContainer(slug = "app$it", name = "n$it", status = "running")
        }
        val hb = WorkerHeartbeat(name = "device", containers = containers)
        assertEquals(20, hb.containers.size)
    }

    @Test
    fun `WorkerHeartbeat equality`() {
        val a = WorkerHeartbeat(name = "d", url = "u")
        val b = WorkerHeartbeat(name = "d", url = "u")
        assertEquals(a, b)
    }

    @Test
    fun `WorkerHeartbeat inequality on name`() {
        val a = WorkerHeartbeat(name = "device1")
        val b = WorkerHeartbeat(name = "device2")
        assertNotEquals(a, b)
    }

    @Test
    fun `WorkerHeartbeat copy changes name only`() {
        val original = WorkerHeartbeat(
            name = "old",
            url = "https://example.com",
            systemInfo = SystemInfo(os = "Android"),
        )
        val copy = original.copy(name = "new")
        assertEquals("new", copy.name)
        assertEquals("https://example.com", copy.url)
        assertEquals("Android", copy.systemInfo.os)
    }

    // --- SystemInfo edge cases ---

    @Test
    fun `SystemInfo equality`() {
        val a = SystemInfo(os = "Android", arch = "arm64")
        val b = SystemInfo(os = "Android", arch = "arm64")
        assertEquals(a, b)
    }

    @Test
    fun `SystemInfo with apps list`() {
        val info = SystemInfo(
            apps = listOf(
                AppStatus(slug = "a", running = true),
                AppStatus(slug = "b", running = false),
            ),
        )
        assertEquals(2, info.apps.size)
    }

    @Test
    fun `SystemInfo default deviceType is android`() {
        assertEquals("android", SystemInfo().deviceType)
    }

    // --- MonitoredApp edge cases ---

    @Test
    fun `MonitoredApp with all fields`() {
        val app = MonitoredApp(
            slug = "test",
            packageName = "com.test.app",
            displayName = "Test App",
            referralUrl = "https://referral.com",
        )
        assertEquals("test", app.slug)
        assertEquals("com.test.app", app.packageName)
        assertEquals("Test App", app.displayName)
        assertEquals("https://referral.com", app.referralUrl)
    }

    @Test
    fun `MonitoredApp hashCode consistent`() {
        val a = MonitoredApp("s", "p", "d")
        val b = MonitoredApp("s", "p", "d")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `MonitoredApp toString contains display name`() {
        val app = MonitoredApp("s", "p", "My App Name")
        assertTrue(app.toString().contains("My App Name"))
    }

    @Test
    fun `MonitoredApp copy changes referralUrl`() {
        val original = MonitoredApp("s", "p", "d", referralUrl = null)
        val updated = original.copy(referralUrl = "https://new.com")
        assertNull(original.referralUrl)
        assertEquals("https://new.com", updated.referralUrl)
    }

    // --- FleetSummary edge cases ---

    @Test
    fun `FleetSummary with all zeros`() {
        val summary = FleetSummary()
        assertEquals(0, summary.running)
        assertEquals(0, summary.stopped)
        assertEquals(0, summary.notInstalled)
        assertEquals(0, summary.disabled)
        assertEquals(0L, summary.totalTx)
        assertEquals(0L, summary.totalRx)
    }

    @Test
    fun `FleetSummary equality`() {
        val a = FleetSummary(running = 3, stopped = 1, totalTx = 500)
        val b = FleetSummary(running = 3, stopped = 1, totalTx = 500)
        assertEquals(a, b)
    }

    @Test
    fun `FleetSummary inequality`() {
        val a = FleetSummary(running = 3)
        val b = FleetSummary(running = 4)
        assertNotEquals(a, b)
    }

    @Test
    fun `FleetSummary hashCode consistent`() {
        val a = FleetSummary(running = 2, totalTx = 1000)
        val b = FleetSummary(running = 2, totalTx = 1000)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `FleetSummary copy changes only specified`() {
        val original = FleetSummary(running = 5, stopped = 3, totalTx = 999)
        val copy = original.copy(running = 6)
        assertEquals(6, copy.running)
        assertEquals(3, copy.stopped)
        assertEquals(999, copy.totalTx)
    }

    @Test
    fun `FleetSummary with large bandwidth values`() {
        val summary = FleetSummary(totalTx = 100L * 1024 * 1024 * 1024, totalRx = 50L * 1024 * 1024 * 1024)
        assertEquals(100L * 1024 * 1024 * 1024, summary.totalTx)
    }

    // --- AppDisplayInfo edge cases ---

    @Test
    fun `AppDisplayInfo equality`() {
        val app = MonitoredApp("s", "p", "d")
        val a = AppDisplayInfo(app = app, state = AppState.RUNNING)
        val b = AppDisplayInfo(app = app, state = AppState.RUNNING)
        assertEquals(a, b)
    }

    @Test
    fun `AppDisplayInfo inequality on state`() {
        val app = MonitoredApp("s", "p", "d")
        val a = AppDisplayInfo(app = app, state = AppState.RUNNING)
        val b = AppDisplayInfo(app = app, state = AppState.STOPPED)
        assertNotEquals(a, b)
    }

    @Test
    fun `AppDisplayInfo with status`() {
        val app = MonitoredApp("s", "p", "d")
        val status = AppStatus(slug = "s", running = true, netTx24h = 500)
        val info = AppDisplayInfo(app = app, state = AppState.RUNNING, status = status)
        assertEquals(500, info.status?.netTx24h)
    }

    @Test
    fun `AppDisplayInfo copy changes state`() {
        val app = MonitoredApp("s", "p", "d")
        val original = AppDisplayInfo(app = app, state = AppState.RUNNING)
        val copy = original.copy(state = AppState.DISABLED)
        assertEquals(AppState.DISABLED, copy.state)
        assertEquals(app, copy.app)
    }

    @Test
    fun `AppDisplayInfo toString contains state`() {
        val info = AppDisplayInfo(
            app = MonitoredApp("s", "p", "d"),
            state = AppState.NOT_INSTALLED,
        )
        assertTrue(info.toString().contains("NOT_INSTALLED"))
    }

    // --- AppState enum ---

    @Test
    fun `AppState has exactly 4 values`() {
        assertEquals(4, AppState.entries.size)
    }

    @Test
    fun `AppState values are RUNNING, STOPPED, NOT_INSTALLED, DISABLED`() {
        val names = AppState.entries.map { it.name }
        assertTrue("RUNNING" in names)
        assertTrue("STOPPED" in names)
        assertTrue("NOT_INSTALLED" in names)
        assertTrue("DISABLED" in names)
    }

    @Test
    fun `AppState valueOf works`() {
        assertEquals(AppState.RUNNING, AppState.valueOf("RUNNING"))
        assertEquals(AppState.STOPPED, AppState.valueOf("STOPPED"))
        assertEquals(AppState.NOT_INSTALLED, AppState.valueOf("NOT_INSTALLED"))
        assertEquals(AppState.DISABLED, AppState.valueOf("DISABLED"))
    }

    // --- KnownApps integration tests ---

    @Test
    fun `all KnownApps slugs are lowercase`() {
        KnownApps.all.forEach { app ->
            assertEquals(app.slug, app.slug.lowercase(), "Slug should be lowercase: ${app.slug}")
        }
    }

    @Test
    fun `all KnownApps package names follow Android conventions`() {
        KnownApps.all.forEach { app ->
            assertTrue(
                app.packageName.contains("."),
                "Package name should contain dots: ${app.packageName}"
            )
        }
    }

    @Test
    fun `all KnownApps referral URLs are https when present`() {
        KnownApps.all.forEach { app ->
            app.referralUrl?.let { url ->
                assertTrue(
                    url.startsWith("https://"),
                    "Referral URL should be HTTPS: $url for ${app.slug}"
                )
            }
        }
    }

    @Test
    fun `KnownApps count is consistent`() {
        assertEquals(KnownApps.all.size, KnownApps.byPackage.size)
        assertEquals(KnownApps.all.size, KnownApps.bySlug.size)
    }

    @Test
    fun `byPackage and bySlug contain same apps`() {
        val fromPackage = KnownApps.byPackage.values.toSet()
        val fromSlug = KnownApps.bySlug.values.toSet()
        assertEquals(fromPackage, fromSlug)
    }

    @Test
    fun `each KnownApp in bySlug is findable by its slug`() {
        KnownApps.all.forEach { app ->
            val found = KnownApps.bySlug[app.slug]
            assertNotNull(found, "Should find app by slug: ${app.slug}")
            assertEquals(app, found)
        }
    }

    @Test
    fun `each KnownApp in byPackage is findable by its packageName`() {
        KnownApps.all.forEach { app ->
            val found = KnownApps.byPackage[app.packageName]
            assertNotNull(found, "Should find app by package: ${app.packageName}")
            assertEquals(app, found)
        }
    }

    @Test
    fun `bySlug returns null for unknown slug`() {
        assertNull(KnownApps.bySlug["nonexistent-app"])
    }

    @Test
    fun `byPackage returns null for unknown package`() {
        assertNull(KnownApps.byPackage["com.nonexistent.package"])
    }

    @Test
    fun `KnownApps list is not empty`() {
        assertTrue(KnownApps.all.isNotEmpty())
    }

    @Test
    fun `specific known apps exist`() {
        val slugs = KnownApps.all.map { it.slug }
        assertTrue("earnapp" in slugs)
        assertTrue("iproyal" in slugs)
        assertTrue("grass" in slugs)
        assertTrue("titan" in slugs)
    }

    // --- Container building pattern (from HeartbeatService) ---

    @Test
    fun `container building from app status running`() {
        val app = AppStatus(slug = "earnapp", running = true)
        val container = AppContainer(
            slug = app.slug,
            name = "cashpilot-${app.slug}",
            status = if (app.running) "running" else "stopped",
            labels = mapOf(
                "cashpilot.managed" to "true",
                "cashpilot.service" to app.slug,
            ),
        )
        assertEquals("earnapp", container.slug)
        assertEquals("cashpilot-earnapp", container.name)
        assertEquals("running", container.status)
        assertEquals("true", container.labels["cashpilot.managed"])
        assertEquals("earnapp", container.labels["cashpilot.service"])
    }

    @Test
    fun `container building from app status stopped`() {
        val app = AppStatus(slug = "iproyal", running = false)
        val container = AppContainer(
            slug = app.slug,
            name = "cashpilot-${app.slug}",
            status = if (app.running) "running" else "stopped",
        )
        assertEquals("stopped", container.status)
    }

    @Test
    fun `building containers from mixed app statuses`() {
        val apps = listOf(
            AppStatus(slug = "earnapp", running = true),
            AppStatus(slug = "iproyal", running = false),
            AppStatus(slug = "grass", running = true),
        )
        val containers = apps.map { app ->
            AppContainer(
                slug = app.slug,
                name = "cashpilot-${app.slug}",
                status = if (app.running) "running" else "stopped",
            )
        }
        assertEquals(3, containers.size)
        assertEquals("running", containers[0].status)
        assertEquals("stopped", containers[1].status)
        assertEquals("running", containers[2].status)
    }
}

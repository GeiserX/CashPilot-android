package com.cashpilot.android

import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KnownAppsTest {

    @Test
    fun `all apps have unique slugs`() {
        val slugs = KnownApps.all.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size, "Duplicate slugs found")
    }

    @Test
    fun `all apps have unique package names`() {
        val packages = KnownApps.all.map { it.packageName }
        assertEquals(packages.size, packages.toSet().size, "Duplicate package names found")
    }

    @Test
    fun `byPackage map contains all entries`() {
        assertEquals(KnownApps.all.size, KnownApps.byPackage.size)
    }

    @Test
    fun `bySlug map contains all entries`() {
        assertEquals(KnownApps.all.size, KnownApps.bySlug.size)
    }

    @Test
    fun `byPackage lookup returns correct app`() {
        val app = KnownApps.byPackage["com.brd.earnapp.play"]
        assertNotNull(app)
        assertEquals("earnapp", app!!.slug)
        assertEquals("EarnApp", app.displayName)
    }

    @Test
    fun `bySlug lookup returns correct app`() {
        val app = KnownApps.bySlug["honeygain"]
        assertNull(app, "honeygain is not in the current known apps list")
    }

    @Test
    fun `all apps have non-empty slug and packageName`() {
        KnownApps.all.forEach { app ->
            assertTrue(app.slug.isNotBlank(), "App has blank slug: $app")
            assertTrue(app.packageName.isNotBlank(), "App has blank packageName: $app")
            assertTrue(app.displayName.isNotBlank(), "App has blank displayName: $app")
        }
    }

    @Test
    fun `iproyal has referral url`() {
        val app = KnownApps.bySlug["iproyal"]
        assertNotNull(app)
        assertNotNull(app!!.referralUrl)
        assertTrue(app.referralUrl!!.startsWith("https://"))
    }

    @Test
    fun `mysterium has no referral url`() {
        val app = KnownApps.bySlug["mysterium"]
        assertNotNull(app)
        assertNull(app!!.referralUrl)
    }

    @Test
    fun `monitored app data class equality`() {
        val a = MonitoredApp("test", "com.test", "Test App")
        val b = MonitoredApp("test", "com.test", "Test App")
        assertEquals(a, b)
    }

    @Test
    fun `monitored app with different referral urls are not equal`() {
        val a = MonitoredApp("test", "com.test", "Test", referralUrl = "https://a.com")
        val b = MonitoredApp("test", "com.test", "Test", referralUrl = "https://b.com")
        assertNotEquals(a, b)
    }
}

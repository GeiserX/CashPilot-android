package com.cashpilot.android

import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the Settings copy/transform patterns used throughout the app
 * (MainViewModel.updateSettings, toggleApp, updateServerUrl, updateApiKey, SetupScreen finishSetup).
 */
class SettingsTransformTest {

    @Test
    fun `updateServerUrl transform preserves other fields`() {
        val original = Settings(
            serverUrl = "https://old.com",
            apiKey = "my-key",
            heartbeatIntervalSeconds = 60,
            enabledSlugs = setOf("earnapp"),
            setupCompleted = true,
        )
        val updated = original.copy(serverUrl = "https://new.com")
        assertEquals("https://new.com", updated.serverUrl)
        assertEquals("my-key", updated.apiKey)
        assertEquals(60, updated.heartbeatIntervalSeconds)
        assertEquals(setOf("earnapp"), updated.enabledSlugs)
        assertTrue(updated.setupCompleted)
    }

    @Test
    fun `updateApiKey transform preserves other fields`() {
        val original = Settings(
            serverUrl = "https://server.com",
            apiKey = "old-key",
            heartbeatIntervalSeconds = 30,
        )
        val updated = original.copy(apiKey = "new-key")
        assertEquals("https://server.com", updated.serverUrl)
        assertEquals("new-key", updated.apiKey)
        assertEquals(30, updated.heartbeatIntervalSeconds)
    }

    @Test
    fun `toggle app removes slug when present`() {
        val slugs = setOf("earnapp", "iproyal", "mysterium")
        val settings = Settings(enabledSlugs = slugs)
        val updated = settings.enabledSlugs.toMutableSet()
        updated.remove("iproyal")
        val result = settings.copy(enabledSlugs = updated.toSet())
        assertEquals(setOf("earnapp", "mysterium"), result.enabledSlugs)
    }

    @Test
    fun `toggle app adds slug when absent`() {
        val slugs = setOf("earnapp")
        val settings = Settings(enabledSlugs = slugs)
        val updated = settings.enabledSlugs.toMutableSet()
        updated.add("iproyal")
        val result = settings.copy(enabledSlugs = updated.toSet())
        assertEquals(setOf("earnapp", "iproyal"), result.enabledSlugs)
    }

    @Test
    fun `toggle app on empty set adds slug`() {
        val settings = Settings(enabledSlugs = emptySet())
        val updated = settings.enabledSlugs.toMutableSet()
        updated.add("grass")
        val result = settings.copy(enabledSlugs = updated.toSet())
        assertEquals(setOf("grass"), result.enabledSlugs)
    }

    @Test
    fun `finishSetup transform sets all three fields`() {
        val settings = Settings()
        val updated = settings.copy(
            serverUrl = "https://cashpilot.example.com",
            apiKey = "test-api-key",
            setupCompleted = true,
        )
        assertEquals("https://cashpilot.example.com", updated.serverUrl)
        assertEquals("test-api-key", updated.apiKey)
        assertTrue(updated.setupCompleted)
    }

    @Test
    fun `settings with custom heartbeat interval`() {
        val settings = Settings(heartbeatIntervalSeconds = 120)
        assertEquals(120, settings.heartbeatIntervalSeconds)
    }

    @Test
    fun `settings hash code consistent with equals`() {
        val a = Settings(serverUrl = "https://a.com", apiKey = "key")
        val b = Settings(serverUrl = "https://a.com", apiKey = "key")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `settings toString contains field values`() {
        val settings = Settings(serverUrl = "https://test.com")
        val str = settings.toString()
        assertTrue(str.contains("https://test.com"))
        assertTrue(str.contains("Settings"))
    }

    @Test
    fun `default enabledSlugs matches all KnownApps`() {
        val settings = Settings()
        KnownApps.all.forEach { app ->
            assertTrue(
                app.slug in settings.enabledSlugs,
                "Expected slug '${app.slug}' in default enabledSlugs"
            )
        }
    }

    @Test
    fun `settings with empty enabledSlugs`() {
        val settings = Settings(enabledSlugs = emptySet())
        assertTrue(settings.enabledSlugs.isEmpty())
    }

    @Test
    fun `settings with subset of enabledSlugs`() {
        val settings = Settings(enabledSlugs = setOf("earnapp", "grass"))
        assertEquals(2, settings.enabledSlugs.size)
        assertTrue("earnapp" in settings.enabledSlugs)
        assertTrue("grass" in settings.enabledSlugs)
        assertFalse("iproyal" in settings.enabledSlugs)
    }

    @Test
    fun `multiple sequential transforms`() {
        var settings = Settings()
        settings = settings.copy(serverUrl = "https://step1.com")
        settings = settings.copy(apiKey = "step2-key")
        settings = settings.copy(heartbeatIntervalSeconds = 45)
        settings = settings.copy(setupCompleted = true)

        assertEquals("https://step1.com", settings.serverUrl)
        assertEquals("step2-key", settings.apiKey)
        assertEquals(45, settings.heartbeatIntervalSeconds)
        assertTrue(settings.setupCompleted)
    }

    @Test
    fun `copy with same values produces equal object`() {
        val original = Settings(
            serverUrl = "https://x.com",
            apiKey = "k",
            heartbeatIntervalSeconds = 30,
            enabledSlugs = setOf("earnapp"),
            setupCompleted = true,
        )
        val copy = original.copy()
        assertEquals(original, copy)
    }
}

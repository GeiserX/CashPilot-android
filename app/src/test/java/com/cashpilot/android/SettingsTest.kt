package com.cashpilot.android

import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `default settings has empty server url`() {
        val settings = Settings()
        assertEquals("", settings.serverUrl)
    }

    @Test
    fun `default settings has empty api key`() {
        val settings = Settings()
        assertEquals("", settings.apiKey)
    }

    @Test
    fun `default heartbeat interval is 30 seconds`() {
        val settings = Settings()
        assertEquals(30, settings.heartbeatIntervalSeconds)
    }

    @Test
    fun `default enabled slugs contains all known apps`() {
        val settings = Settings()
        val allSlugs = KnownApps.all.map { it.slug }.toSet()
        assertEquals(allSlugs, settings.enabledSlugs)
    }

    @Test
    fun `default setup is not completed`() {
        val settings = Settings()
        assertFalse(settings.setupCompleted)
    }

    @Test
    fun `copy preserves values`() {
        val original = Settings(
            serverUrl = "https://example.com",
            apiKey = "test-key",
            heartbeatIntervalSeconds = 60,
            setupCompleted = true,
        )
        val copy = original.copy(heartbeatIntervalSeconds = 120)
        assertEquals("https://example.com", copy.serverUrl)
        assertEquals("test-key", copy.apiKey)
        assertEquals(120, copy.heartbeatIntervalSeconds)
        assertTrue(copy.setupCompleted)
    }

    @Test
    fun `settings equality`() {
        val a = Settings(serverUrl = "https://a.com", apiKey = "key1")
        val b = Settings(serverUrl = "https://a.com", apiKey = "key1")
        assertEquals(a, b)
    }

    @Test
    fun `settings inequality on different api key`() {
        val a = Settings(apiKey = "key1")
        val b = Settings(apiKey = "key2")
        assertNotEquals(a, b)
    }
}

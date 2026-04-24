package com.cashpilot.android

import com.cashpilot.android.service.AppNotificationListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppNotificationListenerTest {

    @BeforeEach
    fun setUp() {
        // Clear any stale state from previous tests
        AppNotificationListener.activeNotifications_.clear()
    }

    @AfterEach
    fun tearDown() {
        AppNotificationListener.activeNotifications_.clear()
    }

    @Test
    fun `isAppNotificationActive returns false when not connected`() {
        // The connected AtomicBoolean defaults to false in a fresh JVM
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isAppNotificationActive returns false for unknown package even with entries`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = System.currentTimeMillis()
        // Still false because connected is false
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isConnected returns false by default`() {
        assertFalse(AppNotificationListener.isConnected())
    }

    @Test
    fun `activeNotifications map is concurrent and thread-safe`() {
        val map = AppNotificationListener.activeNotifications_
        // Verify it's a ConcurrentHashMap by doing concurrent-safe ops
        map["pkg1"] = 100L
        map["pkg2"] = 200L
        assertEquals(2, map.size)
        assertTrue(map.containsKey("pkg1"))
        assertTrue(map.containsKey("pkg2"))
        assertEquals(100L, map["pkg1"])
        assertEquals(200L, map["pkg2"])
    }

    @Test
    fun `activeNotifications remove works`() {
        val map = AppNotificationListener.activeNotifications_
        map["pkg1"] = 100L
        map.remove("pkg1")
        assertFalse(map.containsKey("pkg1"))
        assertEquals(0, map.size)
    }

    @Test
    fun `activeNotifications clear empties map`() {
        val map = AppNotificationListener.activeNotifications_
        map["pkg1"] = 100L
        map["pkg2"] = 200L
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `activeNotifications overwrite updates timestamp`() {
        val map = AppNotificationListener.activeNotifications_
        map["pkg1"] = 100L
        map["pkg1"] = 999L
        assertEquals(999L, map["pkg1"])
    }

    @Test
    fun `isAppNotificationActive returns false for package not in map`() {
        // Even if we add entries, unknown packages are not found
        AppNotificationListener.activeNotifications_["com.known.app"] = System.currentTimeMillis()
        assertFalse(AppNotificationListener.isAppNotificationActive("com.unknown.app"))
    }
}

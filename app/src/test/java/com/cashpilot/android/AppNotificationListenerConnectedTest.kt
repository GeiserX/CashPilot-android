package com.cashpilot.android

import com.cashpilot.android.service.AppNotificationListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests AppNotificationListener companion methods when the listener IS connected.
 * Uses reflection to set the private `connected` AtomicBoolean to true,
 * exercising the branch where isAppNotificationActive actually checks the map.
 */
class AppNotificationListenerConnectedTest {

    private lateinit var connectedField: java.lang.reflect.Field

    @BeforeEach
    fun setUp() {
        AppNotificationListener.activeNotifications_.clear()
        // The `connected` AtomicBoolean is a private static field on the outer class
        connectedField = AppNotificationListener::class.java
            .getDeclaredField("connected")
        connectedField.isAccessible = true
        (connectedField.get(null) as AtomicBoolean).set(true)
    }

    @AfterEach
    fun tearDown() {
        (connectedField.get(null) as AtomicBoolean).set(false)
        AppNotificationListener.activeNotifications_.clear()
    }

    @Test
    fun `isConnected returns true when connected`() {
        assertTrue(AppNotificationListener.isConnected())
    }

    @Test
    fun `isAppNotificationActive returns true when connected and package in map`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = System.currentTimeMillis()
        assertTrue(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isAppNotificationActive returns false when connected but package not in map`() {
        assertFalse(AppNotificationListener.isAppNotificationActive("com.unknown.app"))
    }

    @Test
    fun `isAppNotificationActive returns false when connected and map is empty`() {
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isAppNotificationActive with multiple packages returns correct results`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = 1000L
        AppNotificationListener.activeNotifications_["com.iproyal.android"] = 2000L

        assertTrue(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
        assertTrue(AppNotificationListener.isAppNotificationActive("com.iproyal.android"))
        assertFalse(AppNotificationListener.isAppNotificationActive("com.other.app"))
    }

    @Test
    fun `isAppNotificationActive after removing package returns false`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = System.currentTimeMillis()
        assertTrue(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))

        AppNotificationListener.activeNotifications_.remove("com.brd.earnapp.play")
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isAppNotificationActive after clear returns false`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = System.currentTimeMillis()
        AppNotificationListener.activeNotifications_.clear()
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }

    @Test
    fun `isAppNotificationActive with all known app packages`() {
        // Add all known app packages
        com.cashpilot.android.model.KnownApps.all.forEach { app ->
            AppNotificationListener.activeNotifications_[app.packageName] = System.currentTimeMillis()
        }

        // All should be active
        com.cashpilot.android.model.KnownApps.all.forEach { app ->
            assertTrue(
                AppNotificationListener.isAppNotificationActive(app.packageName),
                "Expected ${app.packageName} to be active"
            )
        }
    }

    @Test
    fun `connected toggle affects isAppNotificationActive`() {
        AppNotificationListener.activeNotifications_["com.brd.earnapp.play"] = System.currentTimeMillis()

        // Connected = true, should be active
        assertTrue(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))

        // Disconnect
        (connectedField.get(null) as AtomicBoolean).set(false)
        assertFalse(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))

        // Reconnect
        (connectedField.get(null) as AtomicBoolean).set(true)
        assertTrue(AppNotificationListener.isAppNotificationActive("com.brd.earnapp.play"))
    }
}

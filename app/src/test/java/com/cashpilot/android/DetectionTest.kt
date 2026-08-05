package com.cashpilot.android

import com.cashpilot.android.service.Detection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The rule that stops the app inventing a diagnosis.
 *
 * Detection used to be one line — `notificationActive || recentlyActive ||
 * hasRecentNetworkActivity` — and all three degrade to `false` when the
 * permissions are denied. So a device with access revoked reported **every**
 * earning app as STOPPED: a claim nobody measured, about apps that were very
 * likely running fine.
 */
class DetectionTest {

    // ------------------------------------------------- positives are absolute

    @Test
    fun `a live notification proves it is running even with usage access denied`() {
        // A running app does not become less running because a DIFFERENT
        // permission was denied. One positive is enough.
        assertEquals(
            true,
            Detection.resolveRunning(
                canSeeNotifications = true,
                canSeeUsage = false,
                notificationActive = true,
                recentlyActive = false,
                hasRecentNetworkActivity = false,
            ),
        )
    }

    @Test
    fun `network activity alone proves it is running`() {
        assertEquals(
            true,
            Detection.resolveRunning(
                canSeeNotifications = false,
                canSeeUsage = true,
                notificationActive = false,
                recentlyActive = false,
                hasRecentNetworkActivity = true,
            ),
        )
    }

    // ------------------------------------------- negatives need full sight

    @Test
    fun `nothing seen with FULL access is a real stopped`() {
        // The distinction is worthless if a genuine negative stops being
        // reported. With every source available, silence means stopped.
        assertEquals(
            false,
            Detection.resolveRunning(
                canSeeNotifications = true,
                canSeeUsage = true,
                notificationActive = false,
                recentlyActive = false,
                hasRecentNetworkActivity = false,
            ),
        )
    }

    @Test
    fun `nothing seen with NO access is unknown, not stopped`() {
        // THE BUG. Every signal reads false because nothing could be read.
        assertNull(
            Detection.resolveRunning(
                canSeeNotifications = false,
                canSeeUsage = false,
                notificationActive = false,
                recentlyActive = false,
                hasRecentNetworkActivity = false,
            ),
        )
    }

    @Test
    fun `nothing seen with only notification access is still unknown`() {
        // Most bandwidth apps run with NO visible notification and are caught
        // solely by network activity, which needs usage access. So a "false"
        // here would be exactly the app we failed to see.
        assertNull(
            Detection.resolveRunning(
                canSeeNotifications = true,
                canSeeUsage = false,
                notificationActive = false,
                recentlyActive = false,
                hasRecentNetworkActivity = false,
            ),
        )
    }

    @Test
    fun `nothing seen with only usage access is still unknown`() {
        assertNull(
            Detection.resolveRunning(
                canSeeNotifications = false,
                canSeeUsage = true,
                notificationActive = false,
                recentlyActive = false,
                hasRecentNetworkActivity = false,
            ),
        )
    }

    // ------------------------------------------------------------- blindness

    @Test
    fun `blind only when no source at all is available`() {
        assertTrue(Detection.isBlind(canSeeNotifications = false, canSeeUsage = false))
        assertFalse(Detection.isBlind(canSeeNotifications = true, canSeeUsage = false))
        assertFalse(Detection.isBlind(canSeeNotifications = false, canSeeUsage = true))
        assertFalse(Detection.isBlind(canSeeNotifications = true, canSeeUsage = true))
    }

    // ----------------------------------------------------------- wire format

    @Test
    fun `unknown is spelled unknown on the wire, never stopped`() {
        // The server maps a falsy running to "stopped", so sending false while
        // blind hands the fleet page the same false claim.
        assertEquals("unknown", Detection.wireStatus(null))
        assertEquals("running", Detection.wireStatus(true))
        assertEquals("stopped", Detection.wireStatus(false))
    }

    @Test
    fun `every wire status is distinct`() {
        val all = listOf(true, false, null).map(Detection::wireStatus)
        assertEquals(all.size, all.distinct().size, "two states collapsed into one wire value: $all")
    }
}

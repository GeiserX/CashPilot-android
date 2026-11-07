package com.cashpilot.android

import com.cashpilot.android.service.Heartbeat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HeartbeatCompanionTest {

    @Test
    fun `lastHeartbeat initial value is zero`() {
        assertEquals(0L, Heartbeat.lastHeartbeat.value)
    }

    @Test
    fun `lastHeartbeatFailed initial value is false`() {
        assertFalse(Heartbeat.lastHeartbeatFailed.value)
    }

    @Test
    fun `lastHeartbeat is a StateFlow`() {
        // Verify it's accessible as a non-null StateFlow
        val flow = Heartbeat.lastHeartbeat
        assertNotNull(flow)
        assertNotNull(flow.value)
    }

    @Test
    fun `lastHeartbeatFailed is a StateFlow`() {
        val flow = Heartbeat.lastHeartbeatFailed
        assertNotNull(flow)
        assertNotNull(flow.value)
    }
}

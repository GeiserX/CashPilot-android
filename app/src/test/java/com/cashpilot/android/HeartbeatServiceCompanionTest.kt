package com.cashpilot.android

import com.cashpilot.android.service.HeartbeatService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HeartbeatServiceCompanionTest {

    @Test
    fun `lastHeartbeat initial value is zero`() {
        assertEquals(0L, HeartbeatService.lastHeartbeat.value)
    }

    @Test
    fun `lastHeartbeatFailed initial value is false`() {
        assertFalse(HeartbeatService.lastHeartbeatFailed.value)
    }

    @Test
    fun `lastHeartbeat is a StateFlow`() {
        // Verify it's accessible as a non-null StateFlow
        val flow = HeartbeatService.lastHeartbeat
        assertNotNull(flow)
        assertNotNull(flow.value)
    }

    @Test
    fun `lastHeartbeatFailed is a StateFlow`() {
        val flow = HeartbeatService.lastHeartbeatFailed
        assertNotNull(flow)
        assertNotNull(flow.value)
    }
}

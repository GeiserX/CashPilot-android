package com.cashpilot.android

import com.cashpilot.android.service.HeartbeatService
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Extended tests for HeartbeatService companion StateFlows.
 * Covers the StateFlow contract: initial values, type checking, and immutability.
 */
class HeartbeatServiceStateFlowTest {

    @Test
    fun `lastHeartbeat is StateFlow of Long`() {
        val flow: StateFlow<Long> = HeartbeatService.lastHeartbeat
        assertNotNull(flow)
        assertTrue(flow.value is Long)
    }

    @Test
    fun `lastHeartbeatFailed is StateFlow of Boolean`() {
        val flow: StateFlow<Boolean> = HeartbeatService.lastHeartbeatFailed
        assertNotNull(flow)
        assertTrue(flow.value is Boolean)
    }

    @Test
    fun `lastHeartbeat initial value is exactly zero`() {
        assertEquals(0L, HeartbeatService.lastHeartbeat.value)
    }

    @Test
    fun `lastHeartbeatFailed initial value is exactly false`() {
        assertEquals(false, HeartbeatService.lastHeartbeatFailed.value)
    }

    @Test
    fun `lastHeartbeat and lastHeartbeatFailed are distinct flows`() {
        assertNotSame(HeartbeatService.lastHeartbeat, HeartbeatService.lastHeartbeatFailed)
    }

    @Test
    fun `lastHeartbeat returns same instance on multiple accesses`() {
        val flow1 = HeartbeatService.lastHeartbeat
        val flow2 = HeartbeatService.lastHeartbeat
        assertSame(flow1, flow2)
    }

    @Test
    fun `lastHeartbeatFailed returns same instance on multiple accesses`() {
        val flow1 = HeartbeatService.lastHeartbeatFailed
        val flow2 = HeartbeatService.lastHeartbeatFailed
        assertSame(flow1, flow2)
    }
}

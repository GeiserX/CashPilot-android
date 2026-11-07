package com.cashpilot.android

import com.cashpilot.android.service.Heartbeat
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Extended tests for Heartbeat StateFlows.
 * Covers the StateFlow contract: initial values, type checking, and immutability.
 */
class HeartbeatStateFlowTest {

    @Test
    fun `lastHeartbeat is StateFlow of Long`() {
        val flow: StateFlow<Long> = Heartbeat.lastHeartbeat
        assertNotNull(flow)
        assertTrue(flow.value is Long)
    }

    @Test
    fun `lastHeartbeatFailed is StateFlow of Boolean`() {
        val flow: StateFlow<Boolean> = Heartbeat.lastHeartbeatFailed
        assertNotNull(flow)
        assertTrue(flow.value is Boolean)
    }

    @Test
    fun `lastHeartbeat initial value is exactly zero`() {
        assertEquals(0L, Heartbeat.lastHeartbeat.value)
    }

    @Test
    fun `lastHeartbeatFailed initial value is exactly false`() {
        assertEquals(false, Heartbeat.lastHeartbeatFailed.value)
    }

    @Test
    fun `lastHeartbeat and lastHeartbeatFailed are distinct flows`() {
        assertNotSame(Heartbeat.lastHeartbeat, Heartbeat.lastHeartbeatFailed)
    }

    @Test
    fun `lastHeartbeat returns same instance on multiple accesses`() {
        val flow1 = Heartbeat.lastHeartbeat
        val flow2 = Heartbeat.lastHeartbeat
        assertSame(flow1, flow2)
    }

    @Test
    fun `lastHeartbeatFailed returns same instance on multiple accesses`() {
        val flow1 = Heartbeat.lastHeartbeatFailed
        val flow2 = Heartbeat.lastHeartbeatFailed
        assertSame(flow1, flow2)
    }
}

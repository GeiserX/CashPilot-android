package com.cashpilot.android

import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.service.HeartbeatService
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Per-worker fleet key: active-key selection, enrollment capture, response parsing. */
class PerWorkerKeyTest {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Settings.activeKey -------------------------------------------------

    @Test
    fun `activeKey uses the shared api key before enrollment`() {
        assertEquals("shared", Settings(apiKey = "shared", workerKey = "").activeKey)
    }

    @Test
    fun `activeKey prefers the per-worker key once enrolled`() {
        assertEquals("own", Settings(apiKey = "shared", workerKey = "own").activeKey)
    }

    // --- HeartbeatService.keyToPersist -------------------------------------

    @Test
    fun `keyToPersist adopts a newly issued key`() {
        assertEquals("issued", HeartbeatService.keyToPersist(current = "", issued = "issued"))
    }

    @Test
    fun `keyToPersist ignores an unchanged key`() {
        assertNull(HeartbeatService.keyToPersist(current = "own", issued = "own"))
    }

    @Test
    fun `keyToPersist ignores absent or blank keys`() {
        assertNull(HeartbeatService.keyToPersist(current = "own", issued = null))
        assertNull(HeartbeatService.keyToPersist(current = "own", issued = ""))
        assertNull(HeartbeatService.keyToPersist(current = "own", issued = "   "))
    }

    // --- HeartbeatService.settingsAfterHeartbeat ----------------------------

    @Test
    fun `settingsAfterHeartbeat adopts a newly issued worker key`() {
        val settings = Settings(apiKey = "shared", workerKey = "")
        val body = WorkerHeartbeatResponse(status = "ok", workerKey = "issued")
        assertEquals("issued", HeartbeatService.settingsAfterHeartbeat(settings, body)?.workerKey)
    }

    @Test
    fun `settingsAfterHeartbeat returns null when the worker key is unchanged, blank, or absent`() {
        val settings = Settings(apiKey = "shared", workerKey = "own")
        assertNull(HeartbeatService.settingsAfterHeartbeat(settings, WorkerHeartbeatResponse(status = "ok", workerKey = "own")))
        assertNull(HeartbeatService.settingsAfterHeartbeat(settings, WorkerHeartbeatResponse(status = "ok", workerKey = "")))
        assertNull(HeartbeatService.settingsAfterHeartbeat(settings, WorkerHeartbeatResponse(status = "ok", workerKey = null)))
    }

    @Test
    fun `settingsAfterHeartbeat compares against the current worker key, not the api key`() {
        val settings = Settings(apiKey = "different", workerKey = "own")
        val body = WorkerHeartbeatResponse(status = "ok", workerKey = "own")
        assertNull(HeartbeatService.settingsAfterHeartbeat(settings, body))
    }

    // --- WorkerHeartbeatResponse deserialization ---------------------------

    @Test
    fun `response parses worker_key and worker_id when present`() {
        val r = json.decodeFromString<WorkerHeartbeatResponse>(
            """{"status":"ok","worker_id":1,"worker_key":"abc123"}""",
        )
        assertEquals("abc123", r.workerKey)
        assertEquals(1L, r.workerId)
    }

    @Test
    fun `response has null worker_key when absent and ignores unknown fields`() {
        val r = json.decodeFromString<WorkerHeartbeatResponse>(
            """{"status":"ok","worker_id":2,"some_future_field":"ignored"}""",
        )
        assertNull(r.workerKey)
        assertEquals(2L, r.workerId)
    }
}

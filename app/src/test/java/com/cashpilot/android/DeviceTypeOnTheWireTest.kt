package com.cashpilot.android

import com.cashpilot.android.model.HeartbeatJson
import com.cashpilot.android.model.SystemInfo
import com.cashpilot.android.model.WorkerHeartbeat
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The server tells a phone from a Docker host by `system_info.device_type`.
 *
 * Encoded with the client's own [HeartbeatJson], not a test copy: the other
 * payload tests set `encodeDefaults = true`, which production never did, so
 * they passed while every real heartbeat left the field out.
 */
class DeviceTypeOnTheWireTest {

    @Test
    fun `a heartbeat sends device_type android`() {
        val beat = WorkerHeartbeat(
            name = "OPPO CPH1951",
            systemInfo = SystemInfo(os = "Android", arch = "arm64-v8a", version = "0.3.0"),
        )
        val wire = HeartbeatJson.parseToJsonElement(HeartbeatJson.encodeToString(WorkerHeartbeat.serializer(), beat))
        val info = wire.jsonObject["system_info"]!!.jsonObject
        assertEquals("android", info["device_type"]?.jsonPrimitive?.content)
    }
}

package com.cashpilot.android

import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.PlatformEarnings
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.service.HeartbeatService
import com.cashpilot.android.util.FormatUtils
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CashPilot-android-35t (client half): the app now has a concept of money.
 *
 * The server sends per-platform earnings on the heartbeat — the one call this
 * client is already authenticated for. What matters here is that the client
 * does not throw away the care the server took:
 *
 *  - `null` usd means NOTHING WAS EVER READ. It must render as an em-dash, not
 *    as "$0.00". A confident zero beside a service the user is running is a lie.
 *  - a genuine `0.0` IS a measurement and renders as $0.00.
 *  - a platform also running on another machine cannot be attributed to this
 *    device, and the payload says so.
 *  - a phone is offline often, so the last figures are kept — but they must be
 *    labelled stale rather than passed off as current.
 */
class EarningsDisplayTest {

    private val json = Json { ignoreUnknownKeys = true }

    // --- unknown is never zero -------------------------------------------

    @Test
    fun `a platform with no reading renders as an em-dash`() {
        assertEquals("—", FormatUtils.formatPlatformEarnings(null))
    }

    @Test
    fun `a genuine zero renders as a real figure`() {
        assertEquals("$0.00", FormatUtils.formatPlatformEarnings(0.0))
    }

    @Test
    fun `a real figure renders with two decimals`() {
        assertEquals("$12.50", FormatUtils.formatPlatformEarnings(12.5))
    }

    @Test
    fun `no total is shown when nothing has been read`() {
        assertNull(FormatUtils.formatEarningsTotal(null, 30))
    }

    @Test
    fun `a total names its window so it cannot be misread`() {
        assertEquals("$3.75 in the last 30 days", FormatUtils.formatEarningsTotal(3.75, 30))
    }

    // --- staleness --------------------------------------------------------

    @Test
    fun `figures never received are stale`() {
        assertTrue(FormatUtils.earningsAreStale(asOfMillis = 0L, nowMillis = 1_000_000L))
    }

    @Test
    fun `fresh figures are not stale`() {
        val now = 10_000_000L
        assertFalse(FormatUtils.earningsAreStale(asOfMillis = now - 60_000L, nowMillis = now))
    }

    @Test
    fun `figures older than the window are stale`() {
        val now = 10_000_000L
        assertTrue(FormatUtils.earningsAreStale(asOfMillis = now - (2 * 60 * 60 * 1000L), nowMillis = now))
    }

    // --- keeping the last known value ------------------------------------

    @Test
    fun `a response without earnings keeps the previous figures`() {
        val previous = Earnings(totalUsd = 5.0)
        assertEquals(previous, HeartbeatService.earningsToKeep(previous, null))
    }

    @Test
    fun `a response with earnings replaces them`() {
        val fresh = Earnings(totalUsd = 9.0)
        assertEquals(fresh, HeartbeatService.earningsToKeep(Earnings(totalUsd = 5.0), fresh))
    }

    @Test
    fun `nothing known stays nothing known`() {
        assertNull(HeartbeatService.earningsToKeep(null, null))
    }

    // --- the wire format --------------------------------------------------

    @Test
    fun `the client parses what the server actually sends`() {
        val body = json.decodeFromString<WorkerHeartbeatResponse>(
            """
            {"status":"ok","worker_id":3,"earnings":{
              "window_days":30,"currency":"USD",
              "platforms":[
                {"slug":"grass","usd":12.5,"shared_with_other_workers":true},
                {"slug":"titan","usd":null,"shared_with_other_workers":false}
              ],
              "total_usd":12.5,"platforms_without_readings":["titan"]}}
            """.trimIndent(),
        )
        val earnings = requireNotNull(body.earnings)
        assertEquals(30, earnings.windowDays)
        assertEquals(12.5, earnings.totalUsd)
        assertEquals(listOf("titan"), earnings.platformsWithoutReadings)
        assertEquals(2, earnings.platforms.size)
        assertNull(earnings.platforms[1].usd, "an unread platform must stay null, not become 0.0")
        assertTrue(earnings.platforms[0].sharedWithOtherWorkers)
    }

    @Test
    fun `an older server that sends no earnings parses as unknown`() {
        val body = json.decodeFromString<WorkerHeartbeatResponse>("""{"status":"ok","worker_id":1}""")
        assertNull(body.earnings, "a missing key must read as unknown, never as an empty set of figures")
    }

    @Test
    fun `a shared platform is flagged so the UI cannot imply device attribution`() {
        val p = PlatformEarnings(slug = "grass", usd = 10.0, sharedWithOtherWorkers = true)
        assertTrue(p.sharedWithOtherWorkers)
    }
}

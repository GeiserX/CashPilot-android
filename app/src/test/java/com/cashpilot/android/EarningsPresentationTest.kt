package com.cashpilot.android

import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.PlatformEarnings
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.EarningsPresentation
import com.cashpilot.android.ui.EarningsPresentation.Mode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The rules that make the earnings display honest.
 *
 * Every one of these was already expressed on the server and preserved through
 * the heartbeat, the model and the service — and then thrown away, because no
 * UI read any of it. These tests exist so that the display cannot quietly undo
 * what the rest of the stack was careful about.
 */
class EarningsPresentationTest {

    private val hour = 60 * 60 * 1000L
    private val now = 1_700_000_000_000L

    private fun earnings(total: Double?, without: List<String> = emptyList()) =
        Earnings(windowDays = 30, currency = "USD", platforms = emptyList(), totalUsd = total, platformsWithoutReadings = without)

    // ------------------------------------------------------------- mode

    @Test
    fun `no server is a normal state, not a missing figure`() {
        // Standalone is supported: there is no server to have asked, so saying
        // "nothing read yet" would blame the wrong thing.
        assertEquals(Mode.NEEDS_SERVER, EarningsPresentation.mode(null, serverConfigured = false))
        assertEquals(Mode.NEEDS_SERVER, EarningsPresentation.mode(earnings(12.5), serverConfigured = false))
    }

    @Test
    fun `paired but never measured is NOT a zero`() {
        // THE RULE. A confident $0.00 beside a running app claims the provider
        // paid nothing — a measurement nobody took.
        assertEquals(Mode.NOTHING_READ, EarningsPresentation.mode(null, serverConfigured = true))
        assertEquals(Mode.NOTHING_READ, EarningsPresentation.mode(earnings(null), serverConfigured = true))
    }

    @Test
    fun `a real zero is a measurement and is shown as a figure`() {
        // 0.0 is not null. The provider was read and reported nothing earned,
        // which the user is entitled to see as $0.00.
        assertEquals(Mode.FIGURE, EarningsPresentation.mode(earnings(0.0), serverConfigured = true))
    }

    @Test
    fun `a positive total is a figure`() {
        assertEquals(Mode.FIGURE, EarningsPresentation.mode(earnings(12.5), serverConfigured = true))
    }

    @Test
    fun `unread platforms do not suppress a real total`() {
        // Some platforms unread is normal; the total covers the ones that were.
        assertEquals(
            Mode.FIGURE,
            EarningsPresentation.mode(earnings(3.0, without = listOf("titan", "uprock")), serverConfigured = true),
        )
    }

    // ------------------------------------------------------------ staleness

    @Test
    fun `a figure older than an hour is labelled`() {
        assertTrue(
            EarningsPresentation.isStale(earnings(5.0), true, asOfMillis = now - 2 * hour, nowMillis = now),
        )
    }

    @Test
    fun `a fresh figure is not labelled`() {
        assertFalse(
            EarningsPresentation.isStale(earnings(5.0), true, asOfMillis = now - 60_000, nowMillis = now),
        )
    }

    @Test
    fun `never received counts as stale`() {
        // asOf == 0 means no heartbeat ever carried figures. If a kept value is
        // somehow on screen it must not be presented as current.
        assertTrue(EarningsPresentation.isStale(earnings(5.0), true, asOfMillis = 0L, nowMillis = now))
    }

    @Test
    fun `staleness is never claimed when there is no figure`() {
        // "May be out of date" beside "nothing read yet" is noise: no
        // measurement is being presented as current.
        assertFalse(EarningsPresentation.isStale(null, true, asOfMillis = 0L, nowMillis = now))
        assertFalse(EarningsPresentation.isStale(earnings(null), true, asOfMillis = 0L, nowMillis = now))
    }

    @Test
    fun `staleness is never claimed without a server`() {
        assertFalse(EarningsPresentation.isStale(earnings(5.0), false, asOfMillis = 0L, nowMillis = now))
    }

    // --------------------------------------------------------- per-app rows

    @Test
    fun `only apps on this device show a figure`() {
        assertTrue(EarningsPresentation.showsPerAppEarnings(AppState.RUNNING))
        assertTrue(EarningsPresentation.showsPerAppEarnings(AppState.STOPPED))
    }

    @Test
    fun `an app that is not installed shows no figure`() {
        assertFalse(EarningsPresentation.showsPerAppEarnings(AppState.NOT_INSTALLED))
    }

    @Test
    fun `a disabled app shows no figure`() {
        // It was switched off deliberately, so any figure is stale by design.
        assertFalse(EarningsPresentation.showsPerAppEarnings(AppState.DISABLED))
    }

    // ------------------------------------------------------- shared balances

    @Test
    fun `the shared flag survives on the model the card reads`() {
        // A provider reports one balance per account, so a platform running on
        // several machines cannot be attributed to this phone. The card must be
        // able to see that; this pins the field it reads.
        val shared = PlatformEarnings(slug = "earnapp", usd = 4.0, sharedWithOtherWorkers = true)
        assertTrue(shared.sharedWithOtherWorkers)
        assertFalse(PlatformEarnings(slug = "grass", usd = 1.0).sharedWithOtherWorkers)
    }

    @Test
    fun `a platform with no reading stays null rather than defaulting to zero`() {
        assertEquals(null, PlatformEarnings(slug = "titan").usd)
    }
}

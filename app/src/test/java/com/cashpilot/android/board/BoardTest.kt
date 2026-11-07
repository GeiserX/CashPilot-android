package com.cashpilot.android.board

import com.cashpilot.android.board.Situations.NOW
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import com.cashpilot.android.model.WorkerHeartbeatResponse
import com.cashpilot.android.service.Heartbeat
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.EarningsPresentation
import com.cashpilot.android.ui.board.Board
import com.cashpilot.android.ui.board.Demo
import com.cashpilot.android.ui.board.NOT_MEASURED
import com.cashpilot.android.ui.board.NO_READING_NOTE
import com.cashpilot.android.ui.board.PairResult
import com.cashpilot.android.ui.board.SHARED_FOOTNOTE
import com.cashpilot.android.ui.board.ServerState
import com.cashpilot.android.ui.board.TaskKind
import com.cashpilot.android.ui.board.Tone
import com.cashpilot.android.ui.board.Words
import com.cashpilot.android.ui.board.appEarnings
import com.cashpilot.android.ui.board.earningsLine
import com.cashpilot.android.ui.board.footnotes
import com.cashpilot.android.ui.board.measuredUse
import com.cashpilot.android.ui.board.pairMessage
import com.cashpilot.android.ui.board.pairResultOf
import com.cashpilot.android.ui.board.rowLine
import com.cashpilot.android.ui.board.serverLine
import com.cashpilot.android.ui.board.serverSetting
import com.cashpilot.android.ui.board.tasks
import com.cashpilot.android.ui.board.tasksToShow
import com.cashpilot.android.ui.board.verdict
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The state-to-UI mapping: what the status board says for each situation.
 * Every assertion here is a sentence or colour a user reads.
 */
class BoardTest {

    // ---- Server line --------------------------------------------------------

    @Test
    fun `no server is a calm, muted line, never an error`() {
        val line = Situations.standalone.serverLine()
        assertEquals(Tone.MUTED, line.tone)
        assertEquals("No server · everything stays on this phone", line.text)
    }

    @Test
    fun `a URL with no key is not a pairing`() {
        val b = Situations.board(Situations.apps(emptyMap(), emptySet()), settings = Settings(serverUrl = "https://x.example"))
        assertEquals(ServerState.NONE, b.server)
    }

    @Test
    fun `a working server says how old the last report is and how often they go`() {
        assertEquals("Last report 6 min ago · sent every 15 min", Situations.healthy.serverLine().text)
        assertEquals(Tone.OK, Situations.healthy.serverLine().tone)
    }

    @Test
    fun `paired with no report yet is waiting, not failing`() {
        val b = Situations.board(Situations.apps(emptyMap(), emptySet()), lastHeartbeat = 0)
        assertEquals("Paired with cashpilot.home.example · waiting for the first report", b.serverLine().text)
        assertEquals(Tone.MUTED, b.serverLine().tone)
    }

    @Test
    fun `a failing server says for how long`() {
        assertEquals("No report has reached cashpilot.home.example for 25 min", Situations.problems.serverLine().text)
        assertEquals(Tone.WARN, Situations.problems.serverLine().tone)
    }

    @Test
    fun `http to a public host is named as the reason nothing is sent`() {
        val b = Situations.board(
            Situations.apps(emptyMap(), emptySet()),
            settings = Settings(serverUrl = "http://8.8.8.8:8080", apiKey = "k", setupCompleted = true),
            failed = true,
        )
        assertEquals(ServerState.NEEDS_HTTPS, b.server)
        assertTrue(b.serverLine().text.contains("https://"))
        assertEquals(TaskKind.SERVER, b.tasks().single().kind)
    }

    @Test
    fun `a private http address is a normal pairing`() {
        val b = Situations.board(
            Situations.apps(emptyMap(), emptySet()),
            settings = Settings(serverUrl = "http://192.168.1.20:8080", apiKey = "k", setupCompleted = true),
        )
        assertEquals(ServerState.OK, b.server)
        assertEquals("192.168.1.20", b.host)
    }

    // ---- Verdict ------------------------------------------------------------

    @Test
    fun `stopped apps lead the verdict, in red, with their count`() {
        val v = Situations.problems.verdict()
        assertEquals(Tone.BAD, v.tone)
        assertEquals(2, v.count)
        assertEquals("2 of 6 apps stopped", v.title)
    }

    @Test
    fun `a healthy phone is a calm verdict`() {
        val v = Situations.healthy.verdict()
        assertEquals(Tone.OK, v.tone)
        assertEquals("All 6 apps running", v.title)
        assertEquals("Nothing needs you right now.", v.sub)
    }

    @Test
    fun `unknown apps are never counted as running`() {
        // One access missing: some apps resolve UNKNOWN. "All running" would be a claim nobody measured.
        val apps = Situations.apps(
            mapOf("earnapp" to Situations.running("earnapp", 1, 1), "grass" to null),
            setOf("earnapp", "grass"),
        )
        val v = Situations.board(apps, usage = false).verdict()
        assertEquals("1 of 2 apps running", v.title)
        assertEquals(Tone.WARN, v.tone)
    }

    @Test
    fun `everything running but the server failing is not a green verdict`() {
        val b = Situations.board(Situations.apps(mapOf("earnapp" to Situations.running("earnapp", 1, 1)), setOf("earnapp")), failed = true)
        val v = b.verdict()
        assertEquals(Tone.WARN, v.tone)
        assertEquals("Your app is running", v.title)
    }

    // ---- Tasks (the ordered list) -------------------------------------------

    @Test
    fun `tasks come in order - stopped apps by name, then server, then battery`() {
        val t = Situations.problems.tasks()
        assertEquals(
            listOf(TaskKind.STOPPED_APP, TaskKind.STOPPED_APP, TaskKind.SERVER, TaskKind.BATTERY),
            t.map { it.kind },
        )
        // Android knows when an app was last opened, not when it stopped, so no
        // task claims a stop time and none is ranked by one.
        assertEquals("grass", t[0].slug)
        assertEquals("traffmonetizer", t[1].slug)
        assertEquals("Traffmonetizer stopped", t[1].title)
    }

    @Test
    fun `every installed app switched off reads as nothing watched, never all running`() {
        val installed = setOf("earnapp", "grass")
        val b = Situations.board(Situations.apps(emptyMap(), installed, disabled = installed))
        assertTrue(b.watched.isEmpty())
        val v = b.verdict()
        assertEquals("No apps watched", v.title)
        assertEquals(Tone.MUTED, v.tone)
    }

    @Test
    fun `two or more problems show the list`() {
        assertEquals(4, Situations.problems.tasksToShow().size)
    }

    @Test
    fun `a single stopped app shows no list, its row carries the Open button`() {
        assertEquals(1, Situations.standalone.tasks().size)
        assertTrue(Situations.standalone.tasksToShow().isEmpty())
    }

    @Test
    fun `a single non-app problem is shown on its own`() {
        val b = Situations.board(Situations.apps(mapOf("earnapp" to Situations.running("earnapp", 1, 1)), setOf("earnapp")), battery = false)
        assertEquals(listOf(TaskKind.BATTERY), b.tasksToShow().map { it.kind })
    }

    @Test
    fun `both accesses missing is the blind screen, not two tasks`() {
        assertTrue(Situations.blind.blind)
        assertTrue(Situations.blind.tasks().none { it.kind == TaskKind.NOTIFICATIONS || it.kind == TaskKind.USAGE })
    }

    @Test
    fun `one access missing is a task`() {
        val b = Situations.board(Situations.apps(emptyMap(), setOf("earnapp")), notif = false)
        assertEquals(listOf(TaskKind.NOTIFICATIONS), b.tasks().map { it.kind })
    }

    @Test
    fun `a healthy phone has nothing to do`() {
        assertTrue(Situations.healthy.tasks().isEmpty())
    }

    // ---- Rows -----------------------------------------------------------------

    @Test
    fun `the board lists only installed, watched apps, problems first`() {
        val b = Situations.problems
        assertEquals(6, b.watched.size)
        assertEquals(AppState.STOPPED, b.watched.first().state)
        assertEquals(4, b.notInstalled.size)
    }

    @Test
    fun `data use is labelled as the last 24 hours, not today`() {
        val earnapp = Situations.healthy.watched.first { it.slug == "earnapp" }
        assertEquals("Running · 450.0 MB in 24 h", earnapp.rowLine(NOW))
    }

    @Test
    fun `a stopped row says when it was last opened`() {
        val grass = Situations.problems.watched.first { it.slug == "grass" }
        assertEquals("Stopped · last opened 3 h ago", grass.rowLine(NOW))
    }

    @Test
    fun `referral URLs reach the screen byte for byte`() {
        Situations.healthy.apps.forEach { row ->
            assertEquals(KnownApps.bySlug.getValue(row.slug).referralUrl, row.referralUrl, row.slug)
        }
        Demo.board(NOW).apps.forEach { row ->
            assertEquals(KnownApps.bySlug.getValue(row.slug).referralUrl, row.referralUrl, row.slug)
        }
    }

    // ---- Earnings ---------------------------------------------------------------

    @Test
    fun `no server asks for one instead of showing a number`() {
        val e = Situations.standalone.earningsLine()
        assertFalse(e.isFigure)
        assertEquals("Add a server to see figures", e.value)
    }

    @Test
    fun `paired but never read is not zero`() {
        val b = Situations.board(Situations.apps(emptyMap(), setOf("earnapp")), earnings = null)
        assertEquals(EarningsPresentation.Mode.NOTHING_READ, b.earningsMode)
        assertEquals("Nothing read yet", b.earningsLine().value)
    }

    @Test
    fun `a fresh figure is shown plainly`() {
        val e = Situations.healthy.earningsLine()
        assertTrue(e.isFigure)
        assertEquals("$15.38", e.value)
        assertNull(e.staleNote)
    }

    @Test
    fun `an old figure says how old`() {
        assertEquals("From 7 h ago", Situations.problems.earningsLine().staleNote)
    }

    @Test
    fun `one footnote for whole-account balances, and one for missing readings`() {
        assertEquals(listOf(SHARED_FOOTNOTE, NO_READING_NOTE), Situations.healthy.footnotes())
        assertTrue(Situations.standalone.footnotes().isEmpty())
    }

    @Test
    fun `a whole-account figure is starred in the detail`() {
        val b = Situations.healthy
        assertEquals("$6.40 *", b.appEarnings(b.apps.first { it.slug == "mysterium" }))
        assertEquals("$4.12", b.appEarnings(b.apps.first { it.slug == "earnapp" }))
        assertEquals("Nothing read yet", b.appEarnings(b.apps.first { it.slug == "grass" }))
    }

    @Test
    fun `an app that is not installed shows no figure`() {
        val b = Situations.healthy
        assertEquals("—", b.appEarnings(b.apps.first { it.state == AppState.NOT_INSTALLED }))
    }

    // ---- Settings and pairing -------------------------------------------------

    @Test
    fun `the API key is masked to its last four characters`() {
        assertEquals("•••• •••• 9a7b", Words.maskKey("example-key-9a7b"))
        assertEquals("••••", Words.maskKey("short"))
        assertEquals("Not set", Words.maskKey(""))
    }

    @Test
    fun `the settings server row uses the 15-minute wording`() {
        assertEquals("Reports every 15 min · last report 6 min ago", Situations.healthy.serverSetting().text)
    }

    @Test
    fun `each send outcome maps to its own pairing message`() {
        assertEquals(PairResult.OK, pairResultOf(Heartbeat.Outcome.SENT))
        assertEquals(PairResult.KEY_REFUSED, pairResultOf(Heartbeat.Outcome.KEY_REFUSED))
        assertEquals(PairResult.UNREACHABLE, pairResultOf(Heartbeat.Outcome.UNREACHABLE))
        assertEquals(PairResult.URL_REFUSED, pairResultOf(Heartbeat.Outcome.URL_REFUSED))
        assertEquals(PairResult.REJECTED, pairResultOf(Heartbeat.Outcome.REJECTED))
        assertEquals(PairResult.NOT_CASHPILOT, pairResultOf(Heartbeat.Outcome.NOT_CASHPILOT))
        val titles = PairResult.entries.mapNotNull { pairMessage(it, "h", "Phone")?.title }
        assertEquals(titles.size, titles.toSet().size, "two results share a message: $titles")
    }

    @Test
    fun `only a CashPilot answer counts as accepted, not any 200`() {
        assertTrue(Heartbeat.acceptedBy(WorkerHeartbeatResponse(status = "ok", workerId = 7)))
        assertFalse(Heartbeat.acceptedBy(WorkerHeartbeatResponse())) // a 200 whose JSON is something else
        assertFalse(Heartbeat.acceptedBy(WorkerHeartbeatResponse(status = "ok"))) // some other endpoint's ok: nothing registered
        assertFalse(Heartbeat.acceptedBy(null)) // a login page: not JSON at all
    }

    @Test
    fun `a new server address drops the phone's own key, so it never goes to another host`() {
        val enrolled = Settings(serverUrl = "https://a.example", apiKey = "fleet", workerKey = "issued-by-a")
        assertEquals("issued-by-a", enrolled.withServerUrl("https://a.example ").workerKey)
        val moved = enrolled.withServerUrl("https://b.example")
        assertEquals("", moved.workerKey)
        assertEquals("fleet", moved.activeKey)
    }

    @Test
    fun `a report overdue by more than an hour is late, not fine`() {
        val late = Situations.board(Situations.apps(emptyMap(), setOf("earnapp")), lastHeartbeat = NOW - 2 * 60 * 60 * 1000L)
        assertEquals(ServerState.LATE, late.server)
        assertEquals(Tone.WARN, late.serverLine().tone)
        assertEquals("Last report 2 h ago · reports are late", late.serverLine().text)
        assertTrue(late.tasks().any { it.kind == TaskKind.SERVER && it.title == "Reports are late" })
        val onTime = Situations.board(Situations.apps(emptyMap(), setOf("earnapp")), lastHeartbeat = NOW - 50 * 60 * 1000L)
        assertEquals(ServerState.OK, onTime.server)
    }

    @Test
    fun `no reading shows as not measured, never 0 B or never`() {
        val b = Situations.board(Situations.apps(emptyMap(), setOf("earnapp", "grass"), disabled = setOf("grass")))
        val grass = b.apps.first { it.slug == "grass" }
        assertEquals(NOT_MEASURED to NOT_MEASURED, b.measuredUse(grass))
        val blindUsage = Situations.board(Situations.apps(mapOf("earnapp" to Situations.running("earnapp", 1, 1)), setOf("earnapp")), usage = false)
        assertEquals(NOT_MEASURED to NOT_MEASURED, blindUsage.measuredUse(blindUsage.apps.first { it.slug == "earnapp" }))
        val earnapp = Situations.healthy.apps.first { it.slug == "earnapp" }
        assertEquals("450.0 MB", Situations.healthy.measuredUse(earnapp).first)
    }

    @Test
    fun `a new shared key drops the phone's own key, the same key keeps it`() {
        val enrolled = Settings(serverUrl = "https://s.example", apiKey = "old", workerKey = "issued")
        assertEquals("issued", enrolled.withApiKey("old").workerKey)
        val changed = enrolled.withApiKey("new")
        assertEquals("", changed.workerKey)
        assertEquals("new", changed.activeKey)
    }

    @Test
    fun `401 and 403 are a refused key, anything else is a rejection`() {
        assertEquals(Heartbeat.Outcome.KEY_REFUSED, Heartbeat.outcomeOf(401))
        assertEquals(Heartbeat.Outcome.KEY_REFUSED, Heartbeat.outcomeOf(403))
        assertEquals(Heartbeat.Outcome.REJECTED, Heartbeat.outcomeOf(500))
        assertEquals(Heartbeat.Outcome.REJECTED, Heartbeat.outcomeOf(404))
    }

    // ---- Words ------------------------------------------------------------------

    @Test
    fun `ages read in plain words`() {
        assertEquals("never", Words.ago(0, NOW))
        assertEquals("just now", Words.ago(NOW - 30_000, NOW))
        assertEquals("25 min ago", Words.ago(NOW - 25 * 60_000, NOW))
        assertEquals("3 h ago", Words.ago(NOW - 3 * 3_600_000, NOW))
        assertEquals("yesterday", Words.ago(NOW - 26 * 3_600_000, NOW))
        assertEquals("4 days ago", Words.ago(NOW - 4 * 86_400_000L, NOW))
        assertEquals("for 25 min", Words.forHowLong(NOW - 25 * 60_000, NOW))
    }

    @Test
    fun `the welcome screen counts the rest of the supported apps`() {
        assertEquals("EarnApp, IPRoyal Pawns, Grass, MystNodes and ${KnownApps.all.size - 4} more", Words.supportedSample())
    }

    @Test
    fun `no screen promises income`() {
        // The store listing and the screens describe what the app watches,
        // never what anyone will earn.
        val promises = Regex("""(?i)earn up to|guarantee|make money|passive income of|\bget paid\b|free money""")
        val offenders = File("src/main/java/com/cashpilot/android/ui").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { f -> promises.findAll(f.readText()).map { "${f.name}: ${it.value}" } }
            .toList()
        assertTrue(offenders.isEmpty(), "income promises on screen: $offenders")
    }

    @Test
    fun `the no-income-promise check can fail`() {
        // A check that cannot fail is not a check.
        assertTrue(Regex("""(?i)earn up to""").containsMatchIn("Earn up to \$50"))
    }
}

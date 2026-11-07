package com.cashpilot.android.ui.board

import com.cashpilot.android.model.Earnings
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import com.cashpilot.android.service.Heartbeat
import com.cashpilot.android.ui.AppDisplayInfo
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.EarningsPresentation
import com.cashpilot.android.util.FormatUtils
import com.cashpilot.android.util.ServerUrlPolicy
import java.net.URI

/**
 * Everything the screens say, decided outside Compose.
 *
 * The screens only lay out what this file returns: which sentence leads the
 * home screen, which colour it gets, what goes in the task list and in which
 * order, how old a report reads. Each of those is a claim to the user, and as
 * plain functions they are unit tests (BoardTest) instead of something only a
 * screenshot could show.
 *
 * Time is passed in as [Board.now], never read from the clock here, so the same
 * input always gives the same words. That is also what keeps the screen goldens
 * stable from one day to the next.
 */

/** How a line should be coloured. The theme turns this into a colour. */
enum class Tone { OK, WARN, BAD, MUTED }

enum class ServerState {
    /** No server, or no key. A supported way to use the app, not an error. */
    NONE,

    /** An http:// address on the public internet. The key is never sent there. */
    NEEDS_HTTPS,
    OK,
    FAILING,
}

data class AppRow(
    val slug: String,
    val name: String,
    val packageName: String,
    val state: AppState,
    val lastActive: Long?,
    val bytes24h: Long,
    /** Null means the server has never read this provider. Not zero. */
    val usd: Double?,
    /** The provider reports one balance for the whole account, not this phone. */
    val shared: Boolean,
    val referralUrl: String?,
)

data class Board(
    val apps: List<AppRow>,
    val server: ServerState,
    val host: String,
    val lastReport: Long,
    val total: Double?,
    val windowDays: Int,
    val earningsAsOf: Long,
    /** Decided by [EarningsPresentation]: no server, nothing read, or a figure. */
    val earningsMode: EarningsPresentation.Mode,
    /** A figure that may be out of date. Never true without a figure. */
    val stale: Boolean,
    val notif: Boolean,
    val usage: Boolean,
    val battery: Boolean,
    val now: Long,
) {
    /** Installed and switched on: the apps the board is about. */
    val watched get() = apps.filter { it.state != AppState.NOT_INSTALLED && it.state != AppState.DISABLED }
    val running get() = watched.filter { it.state == AppState.RUNNING }
    val stopped get() = watched.filter { it.state == AppState.STOPPED }
    val unknown get() = watched.filter { it.state == AppState.UNKNOWN }
    val notInstalled get() = apps.filter { it.state == AppState.NOT_INSTALLED }
    val installed get() = apps.filter { it.state != AppState.NOT_INSTALLED }

    /** Neither permission: every app reads unknown, so the home screen has nothing true to show. */
    val blind get() = !notif && !usage
    val paired get() = server != ServerState.NONE
    val anyShared get() = watched.any { it.shared && it.usd != null }
    val withoutReading get() = watched.filter { it.usd == null }

    companion object {
        fun from(
            apps: List<AppDisplayInfo>,
            settings: Settings,
            lastHeartbeat: Long,
            heartbeatFailed: Boolean,
            earnings: Earnings?,
            earningsAsOf: Long,
            notif: Boolean,
            usage: Boolean,
            battery: Boolean,
            now: Long,
        ): Board {
            val url = settings.serverUrl.trim()
            val configured = url.isNotBlank() && settings.activeKey.isNotBlank()
            return Board(
                apps = apps.map { info ->
                    val p = earnings?.platforms?.firstOrNull { it.slug == info.app.slug }
                    AppRow(
                        slug = info.app.slug,
                        name = info.app.displayName,
                        packageName = info.app.packageName,
                        state = info.state,
                        lastActive = info.status?.lastActive?.let(FormatUtils::parseIso)?.takeIf { it > 0 },
                        bytes24h = (info.status?.netTx24h ?: 0) + (info.status?.netRx24h ?: 0),
                        usd = p?.usd,
                        shared = p?.sharedWithOtherWorkers == true,
                        referralUrl = info.app.referralUrl,
                    )
                },
                server = when {
                    !configured -> ServerState.NONE
                    !ServerUrlPolicy.allowsToken(url) -> ServerState.NEEDS_HTTPS
                    heartbeatFailed -> ServerState.FAILING
                    else -> ServerState.OK
                },
                host = hostOf(url),
                lastReport = lastHeartbeat,
                total = earnings?.totalUsd,
                windowDays = earnings?.windowDays ?: 30,
                earningsAsOf = earningsAsOf,
                earningsMode = EarningsPresentation.mode(earnings, configured),
                stale = EarningsPresentation.isStale(earnings, configured, earningsAsOf, now),
                notif = notif,
                usage = usage,
                battery = battery,
                now = now,
            )
        }

        fun hostOf(url: String): String =
            runCatching { URI(url.trim()).host }.getOrNull()?.removeSurrounding("[", "]") ?: url.trim()
    }
}

// ---------------------------------------------------------------------------
// Words
// ---------------------------------------------------------------------------

object Words {
    fun money(usd: Double?): String = FormatUtils.formatPlatformEarnings(usd)

    fun apps(n: Int): String = if (n == 1) "1 app" else "$n apps"

    fun ago(millis: Long?, now: Long): String {
        if (millis == null || millis <= 0L) return "never"
        val d = ((now - millis) / 1000).coerceAtLeast(0)
        return when {
            d < 60 -> "just now"
            d < 3600 -> "${d / 60} min ago"
            d < 86_400 -> "${d / 3600} h ago"
            d < 2 * 86_400 -> "yesterday"
            else -> "${d / 86_400} days ago"
        }
    }

    /** "for 25 min", "for 3 h": how long something has been wrong. */
    fun forHowLong(millis: Long?, now: Long): String = when (val a = ago(millis, now)) {
        "yesterday" -> "since yesterday"
        "just now" -> "for a moment"
        "never" -> "yet"
        else -> "for " + a.removeSuffix(" ago")
    }

    /** The last four characters of the key, the rest as dots. Never more. */
    fun maskKey(key: String): String = when {
        key.isBlank() -> "Not set"
        key.length <= 8 -> "••••"
        else -> "•••• •••• " + key.takeLast(4)
    }

    /** The well-known apps the welcome screen names, and how many more there are. */
    fun supportedSample(): String {
        val names = listOf("earnapp", "iproyal", "grass", "mysterium").mapNotNull { KnownApps.bySlug[it]?.displayName }
        return names.joinToString(", ") + " and ${KnownApps.all.size - names.size} more"
    }
}

fun Board.ago(millis: Long?) = Words.ago(millis, now)

fun AppRow.stateWord(now: Long): String = when (state) {
    AppState.RUNNING -> "Running"
    AppState.STOPPED -> "Stopped" + (lastActive?.let { " · last opened " + Words.ago(it, now) } ?: "")
    AppState.UNKNOWN -> "Can't see it"
    AppState.DISABLED -> "Not watched"
    AppState.NOT_INSTALLED -> "Not installed"
}

/** The second line of a board row. Data use is the last 24 hours, so it says so. */
fun AppRow.rowLine(now: Long): String =
    if (state == AppState.RUNNING && bytes24h > 0) "Running · ${FormatUtils.formatBytes(bytes24h)} in 24 h" else stateWord(now)

fun AppState.tone(): Tone = when (this) {
    AppState.RUNNING -> Tone.OK
    AppState.STOPPED -> Tone.BAD
    AppState.UNKNOWN -> Tone.WARN
    AppState.DISABLED, AppState.NOT_INSTALLED -> Tone.MUTED
}

data class Line(val tone: Tone, val text: String)

/** The one line about the server, the same words on every screen that shows it. */
fun Board.serverLine(): Line = when (server) {
    ServerState.NONE -> Line(Tone.MUTED, "No server · everything stays on this phone")
    ServerState.NEEDS_HTTPS -> Line(Tone.WARN, "Not sending to $host · it needs https://")
    ServerState.OK -> if (lastReport <= 0L) {
        Line(Tone.MUTED, "Paired with $host · waiting for the first report")
    } else {
        Line(Tone.OK, "Last report ${ago(lastReport)} · sent every 15 min")
    }
    ServerState.FAILING -> if (lastReport <= 0L) {
        Line(Tone.WARN, "Can't reach $host · no report has gone through yet")
    } else {
        Line(Tone.WARN, "Can't reach $host ${Words.forHowLong(lastReport, now)}")
    }
}

/** The headline of the status board: the answer to "is everything running?". */
data class Verdict(val tone: Tone, val count: Int?, val title: String, val sub: String)

fun Board.verdict(): Verdict {
    val n = watched.size
    val all = if (n == 1) "Your app is running" else "All $n apps running"
    return when {
        n == 0 -> Verdict(
            Tone.MUTED, null, "No apps watched",
            "Every installed app is switched off. Turn them back on in Settings, Apps to watch.",
        )
        stopped.isNotEmpty() -> Verdict(
            Tone.BAD, stopped.size, "${stopped.size} of ${Words.apps(n)} stopped",
            "Opening a stopped app usually starts it again.",
        )
        unknown.isNotEmpty() -> Verdict(
            Tone.WARN, null, "${running.size} of ${Words.apps(n)} running",
            "CashPilot can't see the other ${unknown.size} until its access is back on.",
        )
        server == ServerState.FAILING || server == ServerState.NEEDS_HTTPS -> Verdict(
            Tone.WARN, null, all, "But your server isn't getting this phone's reports.",
        )
        else -> Verdict(Tone.OK, null, all, "Nothing needs you right now.")
    }
}

enum class TaskKind { STOPPED_APP, SERVER, NOTIFICATIONS, USAGE, BATTERY }

data class Task(val kind: TaskKind, val tone: Tone, val title: String, val body: String, val action: String, val slug: String? = null)

/**
 * Everything that needs the user, most urgent first: stopped apps (by name:
 * Android tells us when an app was last opened, never when it stopped), then
 * the server, then missing access, then battery.
 *
 * Both accesses missing is the blind screen, not a pair of tasks.
 */
fun Board.tasks(): List<Task> {
    val out = mutableListOf<Task>()
    stopped.sortedBy { it.name }.forEach {
        out += Task(
            TaskKind.STOPPED_APP, Tone.BAD,
            "${it.name} stopped",
            "Opening it usually starts it again.", "Open ${it.name}", it.slug,
        )
    }
    when (server) {
        ServerState.FAILING -> out += Task(
            TaskKind.SERVER, Tone.WARN, "Your server isn't answering",
            if (lastReport <= 0L) "No report has reached $host yet." else "Nothing has reached $host ${Words.forHowLong(lastReport, now)}.",
            "Check connection",
        )
        ServerState.NEEDS_HTTPS -> out += Task(
            TaskKind.SERVER, Tone.WARN, "Reports are not being sent",
            "$host is on the public internet, so CashPilot sends the key only over https://.",
            "Fix the address",
        )
        else -> Unit
    }
    if (!blind && !notif) {
        out += Task(TaskKind.NOTIFICATIONS, Tone.BAD, "Notification access is off", "CashPilot can't tell which apps are running until it's back on.", "Turn on")
    }
    if (!blind && !usage) {
        out += Task(TaskKind.USAGE, Tone.WARN, "Usage access is off", "No last-seen times or data use until it's back on.", "Turn on")
    }
    if (!battery) {
        out += Task(
            TaskKind.BATTERY, Tone.WARN, "Reports may arrive late",
            "Battery optimisation is on for CashPilot, so Android can hold reports back while the screen is off.",
            "Open battery settings",
        )
    }
    return out
}

/**
 * What the home screen shows above the app list.
 *
 * Two or more problems: the ordered list, so the user works top to bottom.
 * One stopped app: nothing extra, its row already carries the Open button.
 * One other problem: that one card on its own.
 */
fun Board.tasksToShow(): List<Task> {
    val all = tasks()
    return when {
        all.size >= 2 -> all
        all.size == 1 && all[0].kind != TaskKind.STOPPED_APP -> all
        else -> emptyList()
    }
}

/** The earnings strip: label, value and whether the value is a real figure. */
data class EarningsLine(val label: String, val value: String, val isFigure: Boolean, val staleNote: String?)

fun Board.earningsLine(): EarningsLine {
    val label = "Your server reports, last $windowDays days"
    return when (earningsMode) {
        EarningsPresentation.Mode.NEEDS_SERVER -> EarningsLine(label, "Add a server to see figures", false, null)
        EarningsPresentation.Mode.NOTHING_READ -> EarningsLine(label, "Nothing read yet", false, null)
        EarningsPresentation.Mode.FIGURE ->
            EarningsLine(label, Words.money(total), true, if (stale) "From ${ago(earningsAsOf)}" else null)
    }
}

/** The single footnote for figures that are a whole account's balance, not this phone's. */
const val SHARED_FOOTNOTE =
    "* Runs on more than one of your devices. The provider reports one balance for the account, so this is not this phone alone."

const val NO_READING_NOTE = "No reading means your server has sent no figure for that provider. It is not \$0."

fun Board.footnotes(): List<String> = buildList {
    if (earningsMode == EarningsPresentation.Mode.FIGURE && anyShared) add(SHARED_FOOTNOTE)
    if (paired && withoutReading.isNotEmpty()) add(NO_READING_NOTE)
}

/** The value in the app detail's earnings row, starred when it is a whole account. */
fun Board.appEarnings(a: AppRow): String = when {
    !paired -> "Needs a server"
    !EarningsPresentation.showsPerAppEarnings(a.state) -> "\u2014"
    a.usd == null -> "Nothing read yet"
    a.shared -> Words.money(a.usd) + " *"
    else -> Words.money(a.usd)
}

/** The Server row at the top of Settings. */
fun Board.serverSetting(): Line = when (server) {
    ServerState.NONE -> Line(Tone.MUTED, "Everything stays on this phone · add a server for figures")
    ServerState.NEEDS_HTTPS -> Line(Tone.WARN, "Not sending · this address needs https://")
    ServerState.FAILING -> Line(Tone.WARN, if (lastReport <= 0L) "Not answering · no report yet" else "Not answering · last report ${ago(lastReport)}")
    ServerState.OK -> if (lastReport <= 0L) {
        Line(Tone.MUTED, "Paired · waiting for the first report")
    } else {
        Line(Tone.OK, "Reports every 15 min · last report ${ago(lastReport)}")
    }
}

// ---------------------------------------------------------------------------
// Pairing: what a connection test says.
// ---------------------------------------------------------------------------

enum class PairResult { NONE, TESTING, OK, KEY_REFUSED, UNREACHABLE, URL_REFUSED, REJECTED, NOT_CASHPILOT }

fun pairResultOf(outcome: Heartbeat.Outcome): PairResult = when (outcome) {
    Heartbeat.Outcome.SENT -> PairResult.OK
    Heartbeat.Outcome.KEY_REFUSED -> PairResult.KEY_REFUSED
    Heartbeat.Outcome.UNREACHABLE -> PairResult.UNREACHABLE
    Heartbeat.Outcome.URL_REFUSED -> PairResult.URL_REFUSED
    Heartbeat.Outcome.REJECTED -> PairResult.REJECTED
    Heartbeat.Outcome.NOT_CASHPILOT -> PairResult.NOT_CASHPILOT
    Heartbeat.Outcome.NOT_CONFIGURED -> PairResult.NONE
}

data class PairMessage(val tone: Tone, val title: String, val body: String)

fun pairMessage(result: PairResult, host: String, deviceName: String): PairMessage? = when (result) {
    PairResult.NONE -> null
    PairResult.TESTING -> PairMessage(Tone.MUTED, "Testing…", "Sending one report to $host.")
    PairResult.OK -> PairMessage(Tone.OK, "Connected", "Your server answered and accepted the key. This phone appears in Fleet as $deviceName.")
    PairResult.KEY_REFUSED -> PairMessage(
        Tone.BAD, "The server refused this API key",
        "The address is right: the server answered. Copy the key again from Fleet → Reveal API Key.",
    )
    PairResult.UNREACHABLE -> PairMessage(
        Tone.BAD, "No answer from $host",
        "Check the address, and that this phone can reach it: on the same network, or over your VPN.",
    )
    PairResult.URL_REFUSED -> PairMessage(
        Tone.BAD, "This address needs https://",
        "CashPilot sends the key over http:// only to your own network: 192.168.x.x, 10.x.x.x, a .local or .lan name, or Tailscale.",
    )
    PairResult.REJECTED -> PairMessage(
        Tone.BAD, "The server answered with an error",
        "It is reachable but did not accept the report. Check that it runs a recent CashPilot release.",
    )
    PairResult.NOT_CASHPILOT -> PairMessage(
        Tone.BAD, "$host answered, but not as CashPilot",
        "Something else answers at this address, such as a login page or another app. Use the address you open the CashPilot dashboard at.",
    )
}

package com.cashpilot.android.ui.board

import com.cashpilot.android.R
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

    /** The last report went through, but none has gone out for over [LATE_AFTER_MS]. */
    LATE,

    /** The last report did not get through: no answer, or the server refused it. */
    FAILING,
}

/** Four missed 15-minute periods: past what Android's normal deferral explains. */
const val LATE_AFTER_MS = 60 * 60 * 1000L

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
    /** False when detection has no reading for this app (switched off, or no access): its data use and last-opened time are unknown, not zero. */
    val measured: Boolean = true,
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
                        measured = info.status != null,
                    )
                },
                server = when {
                    !configured -> ServerState.NONE
                    !ServerUrlPolicy.allowsToken(url) -> ServerState.NEEDS_HTTPS
                    heartbeatFailed -> ServerState.FAILING
                    lastHeartbeat > 0L && now - lastHeartbeat > LATE_AFTER_MS -> ServerState.LATE
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
    fun money(s: Strings, usd: Double?): String = FormatUtils.formatPlatformEarnings(usd, s.locale)

    fun bytes(s: Strings, bytes: Long): String = FormatUtils.formatBytes(bytes, s.locale) { n, unit ->
        s.get(
            when (unit) {
                FormatUtils.ByteUnit.B -> R.string.bytes_b
                FormatUtils.ByteUnit.KB -> R.string.bytes_kb
                FormatUtils.ByteUnit.MB -> R.string.bytes_mb
                FormatUtils.ByteUnit.GB -> R.string.bytes_gb
            },
            n,
        )
    }

    /** How long ago, in whole units: what [ago] and [forHowLong] both count in. */
    private sealed interface Age {
        data object Never : Age
        data object JustNow : Age
        data object Yesterday : Age
        data class Minutes(val n: Int) : Age
        data class Hours(val n: Int) : Age
        data class Days(val n: Int) : Age
    }

    private fun age(millis: Long?, now: Long): Age {
        if (millis == null || millis <= 0L) return Age.Never
        val d = ((now - millis) / 1000).coerceAtLeast(0)
        return when {
            d < 60 -> Age.JustNow
            d < 3600 -> Age.Minutes((d / 60).toInt())
            d < 86_400 -> Age.Hours((d / 3600).toInt())
            d < 2 * 86_400 -> Age.Yesterday
            else -> Age.Days((d / 86_400).toInt())
        }
    }

    fun ago(s: Strings, millis: Long?, now: Long): String = when (val a = age(millis, now)) {
        Age.Never -> s.get(R.string.ago_never)
        Age.JustNow -> s.get(R.string.ago_just_now)
        Age.Yesterday -> s.get(R.string.ago_yesterday)
        is Age.Minutes -> s.plural(R.plurals.ago_minutes, a.n, a.n)
        is Age.Hours -> s.plural(R.plurals.ago_hours, a.n, a.n)
        is Age.Days -> s.plural(R.plurals.ago_days, a.n, a.n)
    }

    /** "for 25 min", "for 3 h": how long something has been wrong. */
    fun forHowLong(s: Strings, millis: Long?, now: Long): String = when (val a = age(millis, now)) {
        Age.Never -> s.get(R.string.for_yet)
        Age.JustNow -> s.get(R.string.for_a_moment)
        Age.Yesterday -> s.get(R.string.since_yesterday)
        is Age.Minutes -> s.plural(R.plurals.for_minutes, a.n, a.n)
        is Age.Hours -> s.plural(R.plurals.for_hours, a.n, a.n)
        is Age.Days -> s.plural(R.plurals.for_days, a.n, a.n)
    }

    /** The last four characters of the key, the rest as dots. Never more. */
    fun maskKey(s: Strings, key: String): String = when {
        key.isBlank() -> s.get(R.string.not_set)
        key.length <= 8 -> "••••"
        else -> "•••• •••• " + key.takeLast(4)
    }

    /** The well-known apps the welcome screen names, and how many more there are. */
    fun supportedSample(s: Strings): String {
        val names = listOf("earnapp", "iproyal", "grass", "mysterium").mapNotNull { KnownApps.bySlug[it]?.displayName }
        val more = KnownApps.all.size - names.size
        return s.plural(R.plurals.supported_sample, more, names.joinToString(s.get(R.string.list_separator)), more)
    }

    /** The supported apps, by name, for the disclosures. */
    fun supportedNames(s: Strings): String =
        s.get(R.string.supported_names, KnownApps.all.joinToString(s.get(R.string.list_separator)) { it.displayName })
}

fun Board.ago(s: Strings, millis: Long?) = Words.ago(s, millis, now)

fun AppRow.stateWord(s: Strings, now: Long): String = when (state) {
    AppState.RUNNING -> s.get(R.string.state_running)
    AppState.STOPPED -> lastActive?.let { s.get(R.string.state_stopped_last_opened, Words.ago(s, it, now)) } ?: s.get(R.string.state_stopped)
    AppState.UNKNOWN -> s.get(R.string.state_unknown)
    AppState.DISABLED -> s.get(R.string.state_disabled)
    AppState.NOT_INSTALLED -> s.get(R.string.state_not_installed)
}

/** The second line of a board row. Data use is the last 24 hours, so it says so. */
fun AppRow.rowLine(s: Strings, now: Long): String =
    if (state == AppState.RUNNING && bytes24h > 0) s.get(R.string.row_running_data, Words.bytes(s, bytes24h)) else stateWord(s, now)

fun AppState.tone(): Tone = when (this) {
    AppState.RUNNING -> Tone.OK
    AppState.STOPPED -> Tone.BAD
    AppState.UNKNOWN -> Tone.WARN
    AppState.DISABLED, AppState.NOT_INSTALLED -> Tone.MUTED
}

data class Line(val tone: Tone, val text: String)

/** The one line about the server, the same words on every screen that shows it. */
fun Board.serverLine(s: Strings): Line = when (server) {
    ServerState.NONE -> Line(Tone.MUTED, s.get(R.string.server_line_none))
    ServerState.NEEDS_HTTPS -> Line(Tone.WARN, s.get(R.string.server_line_needs_https, host))
    ServerState.OK -> if (lastReport <= 0L) {
        Line(Tone.MUTED, s.get(R.string.server_line_waiting, host))
    } else {
        Line(Tone.OK, s.get(R.string.server_line_ok, ago(s, lastReport)))
    }
    ServerState.LATE -> Line(Tone.WARN, s.get(R.string.server_line_late, ago(s, lastReport)))
    ServerState.FAILING -> if (lastReport <= 0L) {
        Line(Tone.WARN, s.get(R.string.server_line_failing_never, host))
    } else {
        Line(Tone.WARN, s.get(R.string.server_line_failing, host, Words.forHowLong(s, lastReport, now)))
    }
}

/** The headline of the status board: the answer to "is everything running?". */
data class Verdict(val tone: Tone, val count: Int?, val title: String, val sub: String)

fun Board.verdict(s: Strings): Verdict {
    val n = watched.size
    val all = if (n == 1) s.get(R.string.verdict_one_running) else s.plural(R.plurals.verdict_all_running, n, n)
    return when {
        n == 0 -> Verdict(Tone.MUTED, null, s.get(R.string.verdict_none_title), s.get(R.string.verdict_none_sub))
        stopped.isNotEmpty() -> Verdict(
            Tone.BAD, stopped.size, s.plural(R.plurals.verdict_stopped_title, n, stopped.size, n),
            s.get(R.string.verdict_stopped_sub),
        )
        unknown.isNotEmpty() -> Verdict(
            Tone.WARN, null, s.plural(R.plurals.verdict_unknown_title, n, running.size, n),
            s.plural(R.plurals.verdict_unknown_sub, unknown.size, unknown.size),
        )
        server == ServerState.FAILING || server == ServerState.NEEDS_HTTPS ->
            Verdict(Tone.WARN, null, all, s.get(R.string.verdict_server_sub))
        server == ServerState.LATE ->
            Verdict(Tone.WARN, null, all, s.get(R.string.verdict_late_sub, Words.forHowLong(s, lastReport, now)))
        else -> Verdict(Tone.OK, null, all, s.get(R.string.verdict_ok_sub))
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
fun Board.tasks(s: Strings): List<Task> {
    val out = mutableListOf<Task>()
    stopped.sortedBy { it.name }.forEach {
        out += Task(
            TaskKind.STOPPED_APP, Tone.BAD,
            s.get(R.string.task_stopped_title, it.name),
            s.get(R.string.task_stopped_body), s.get(R.string.action_open_app, it.name), it.slug,
        )
    }
    val check = s.get(R.string.action_check_connection)
    when (server) {
        ServerState.FAILING -> out += Task(
            TaskKind.SERVER, Tone.WARN, s.get(R.string.task_failing_title),
            if (lastReport <= 0L) {
                s.get(R.string.task_failing_body_never, host)
            } else {
                s.get(R.string.task_failing_body, host, Words.forHowLong(s, lastReport, now))
            },
            check,
        )
        ServerState.LATE -> out += Task(
            TaskKind.SERVER, Tone.WARN, s.get(R.string.task_late_title),
            s.get(R.string.task_late_body, Words.forHowLong(s, lastReport, now)),
            check,
        )
        ServerState.NEEDS_HTTPS -> out += Task(
            TaskKind.SERVER, Tone.WARN, s.get(R.string.task_https_title),
            s.get(R.string.task_https_body, host),
            s.get(R.string.action_fix_address),
        )
        else -> Unit
    }
    if (!blind && !notif) {
        out += Task(TaskKind.NOTIFICATIONS, Tone.BAD, s.get(R.string.task_notif_title), s.get(R.string.task_notif_body), s.get(R.string.action_turn_on))
    }
    if (!blind && !usage) {
        out += Task(TaskKind.USAGE, Tone.WARN, s.get(R.string.task_usage_title), s.get(R.string.task_usage_body), s.get(R.string.action_turn_on))
    }
    if (!battery) {
        out += Task(
            TaskKind.BATTERY, Tone.WARN, s.get(R.string.task_battery_title),
            s.get(R.string.task_battery_body),
            s.get(R.string.action_open_battery),
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
fun Board.tasksToShow(s: Strings): List<Task> {
    val all = tasks(s)
    return when {
        all.size >= 2 -> all
        all.size == 1 && all[0].kind != TaskKind.STOPPED_APP -> all
        else -> emptyList()
    }
}

/** The earnings strip: label, value and whether the value is a real figure. */
data class EarningsLine(val label: String, val value: String, val isFigure: Boolean, val staleNote: String?)

fun Board.earningsLine(s: Strings): EarningsLine {
    val label = s.plural(R.plurals.earnings_label, windowDays, windowDays)
    return when (earningsMode) {
        EarningsPresentation.Mode.NEEDS_SERVER -> EarningsLine(label, s.get(R.string.earnings_needs_server), false, null)
        EarningsPresentation.Mode.NOTHING_READ -> EarningsLine(label, s.get(R.string.nothing_read_yet), false, null)
        EarningsPresentation.Mode.FIGURE ->
            EarningsLine(label, Words.money(s, total), true, if (stale) s.get(R.string.earnings_from, ago(s, earningsAsOf)) else null)
    }
}

/** Data use and last-opened time: only real readings, which need usage access and a detected app. */
fun Board.measuredUse(s: Strings, a: AppRow): Pair<String, String> =
    if (a.measured && usage) {
        Words.bytes(s, a.bytes24h) to ago(s, a.lastActive)
    } else {
        s.get(R.string.not_measured) to s.get(R.string.not_measured)
    }

/**
 * The footnotes under the earnings strip: one for figures that are a whole
 * account's balance, not this phone's, and one for providers with no reading.
 */
fun Board.footnotes(s: Strings): List<String> = buildList {
    if (earningsMode == EarningsPresentation.Mode.FIGURE && anyShared) add(s.get(R.string.shared_footnote))
    if (paired && withoutReading.isNotEmpty()) add(s.get(R.string.no_reading_note))
}

/** The value in the app detail's earnings row, starred when it is a whole account. */
fun Board.appEarnings(s: Strings, a: AppRow): String = when {
    !paired -> s.get(R.string.app_earnings_needs_server)
    !EarningsPresentation.showsPerAppEarnings(a.state) -> "—"
    a.usd == null -> s.get(R.string.nothing_read_yet)
    a.shared -> s.get(R.string.app_earnings_shared, Words.money(s, a.usd))
    else -> Words.money(s, a.usd)
}

/** The Server row at the top of Settings. */
fun Board.serverSetting(s: Strings): Line = when (server) {
    ServerState.NONE -> Line(Tone.MUTED, s.get(R.string.settings_server_none))
    ServerState.NEEDS_HTTPS -> Line(Tone.WARN, s.get(R.string.settings_server_needs_https))
    ServerState.FAILING -> Line(
        Tone.WARN,
        if (lastReport <= 0L) s.get(R.string.settings_server_failing_never) else s.get(R.string.settings_server_failing, ago(s, lastReport)),
    )
    ServerState.LATE -> Line(Tone.WARN, s.get(R.string.settings_server_late, ago(s, lastReport)))
    ServerState.OK -> if (lastReport <= 0L) {
        Line(Tone.MUTED, s.get(R.string.settings_server_waiting))
    } else {
        Line(Tone.OK, s.get(R.string.settings_server_ok, ago(s, lastReport)))
    }
}

// ---------------------------------------------------------------------------
// Pairing: what a connection test says.
// ---------------------------------------------------------------------------

enum class PairResult { NONE, TESTING, OK, KEY_REFUSED, UNREACHABLE, URL_REFUSED, REJECTED, NOT_CASHPILOT, SAVE_FAILED }

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

fun pairMessage(s: Strings, result: PairResult, host: String, deviceName: String): PairMessage? = when (result) {
    PairResult.NONE -> null
    PairResult.TESTING -> PairMessage(Tone.MUTED, s.get(R.string.testing), s.get(R.string.pair_testing_body, host))
    PairResult.OK -> PairMessage(Tone.OK, s.get(R.string.pair_ok_title), s.get(R.string.pair_ok_body, deviceName))
    PairResult.KEY_REFUSED -> PairMessage(Tone.BAD, s.get(R.string.pair_key_refused_title), s.get(R.string.pair_key_refused_body))
    PairResult.UNREACHABLE -> PairMessage(Tone.BAD, s.get(R.string.pair_unreachable_title, host), s.get(R.string.pair_unreachable_body))
    PairResult.URL_REFUSED -> PairMessage(Tone.BAD, s.get(R.string.pair_url_refused_title), s.get(R.string.pair_url_refused_body))
    PairResult.REJECTED -> PairMessage(Tone.BAD, s.get(R.string.pair_rejected_title), s.get(R.string.pair_rejected_body))
    PairResult.NOT_CASHPILOT -> PairMessage(Tone.BAD, s.get(R.string.pair_not_cashpilot_title, host), s.get(R.string.pair_not_cashpilot_body))
    PairResult.SAVE_FAILED -> PairMessage(Tone.BAD, s.get(R.string.pair_save_failed_title), s.get(R.string.pair_save_failed_body))
}

package com.cashpilot.android.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.theme.LocalPalette
import com.cashpilot.android.ui.theme.Tnum

/** What the home screens can ask for. The activity wires these to real intents. */
data class BoardActions(
    val openApp: (AppRow) -> Unit = {},
    val showApp: (String) -> Unit = {},
    val openSettings: () -> Unit = {},
    val showSupported: () -> Unit = {},
    val showAllSupported: () -> Unit = {},
    val install: (AppRow) -> Unit = {},
    val allowNotifications: () -> Unit = {},
    val allowUsage: () -> Unit = {},
    val openBattery: () -> Unit = {},
    val checkServer: () -> Unit = {},
    val startDemo: () -> Unit = {},
    val toggleApp: (String) -> Unit = {},
)

/**
 * The home screen: the status board.
 *
 * One sentence answers the question the app is opened for ("is everything
 * running?"), then one quiet row per app. Colour appears only where something
 * is wrong, so a healthy phone is a calm screen and a broken one is not.
 */
@Composable
fun HomeScreen(b: Board, actions: BoardActions) {
    when {
        // Every known app gets a row once the first detection has run; before
        // that, an empty list is "not looked yet", never "nothing installed".
        b.apps.isEmpty() -> return Box(Modifier.fillMaxSize())
        b.installed.isEmpty() -> return NoAppsHome(b, actions)
        b.blind -> return BlindHome(b, actions)
    }
    val c = LocalPalette.current
    val s = strings()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.app_name)) {
            IconButton(onClick = actions.openSettings, modifier = Modifier.size(48.dp)) {
                Ic(R.drawable.ic_settings, size = 24, description = stringResource(R.string.settings_title))
            }
        }
        Gap(12)
        VerdictBlock(b)
        Gap(24)
        Column(Modifier.padding(horizontal = 16.dp)) {
            val tasks = b.tasksToShow(s)
            if (tasks.size >= 2) {
                Heading(
                    pluralStringResource(R.plurals.home_tasks_heading, tasks.size, tasks.size),
                    Modifier.padding(start = 4.dp, bottom = 10.dp),
                    MaterialTheme.typography.titleMedium,
                )
            }
            tasks.forEachIndexed { i, t -> TaskCard(t, first = i == 0, numbered = tasks.size >= 2, index = i + 1, b = b, actions = actions) }
            if (tasks.isNotEmpty()) Gap(8)
            if (b.watched.isNotEmpty()) {
                Panel(pad = PaddingValues(vertical = 4.dp)) {
                    b.watched.forEachIndexed { i, a ->
                        if (i > 0) Divider(Modifier.padding(start = 70.dp))
                        BoardRow(a, b.now, actions)
                    }
                }
            } else {
                Panel {
                    Text(stringResource(R.string.home_all_switched_off), color = c.text2)
                }
            }
            Gap(12)
            EarningsStrip(b, actions)
            b.footnotes(s).forEach {
                Gap(8)
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
            if (b.notInstalled.isNotEmpty()) {
                Gap(8)
                LinkRow(pluralStringResource(R.plurals.home_other_supported, b.notInstalled.size, b.notInstalled.size), actions.showSupported)
            }
            Gap(24)
        }
    }
}

@Composable
private fun VerdictBlock(b: Board) {
    val c = LocalPalette.current
    val v = b.verdict(strings())
    val ring = toneColor(v.tone)
    Column(Modifier.padding(horizontal = 20.dp)) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).border(5.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (v.count != null) {
                Text(
                    String.format(LocalConfiguration.current.locales[0], "%d", v.count),
                    Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.headlineSmall.merge(Tnum),
                    fontWeight = FontWeight.Bold,
                    color = ring,
                )
            } else {
                Ic(if (v.tone == Tone.OK) R.drawable.ic_check_circle else R.drawable.ic_warning, tint = ring, size = 32)
            }
        }
        Gap(16)
        Heading(v.title, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp))
        Gap(6)
        Text(v.sub, style = MaterialTheme.typography.bodyLarge, color = c.text2)
        Gap(14)
        ServerLine(b)
    }
}

@Composable
private fun TaskCard(t: Task, first: Boolean, numbered: Boolean, index: Int, b: Board, actions: BoardActions) {
    val c = LocalPalette.current
    val tint = toneColor(t.tone)
    val icon = when (t.kind) {
        TaskKind.STOPPED_APP -> R.drawable.ic_warning
        TaskKind.SERVER -> R.drawable.ic_cloud_off
        TaskKind.NOTIFICATIONS -> R.drawable.ic_notifications
        TaskKind.USAGE -> R.drawable.ic_query_stats
        TaskKind.BATTERY -> R.drawable.ic_battery_alert
    }
    val onClick: () -> Unit = when (t.kind) {
        TaskKind.STOPPED_APP -> { { b.watched.firstOrNull { it.slug == t.slug }?.let(actions.openApp) } }
        TaskKind.SERVER -> if (b.server == ServerState.NEEDS_HTTPS) actions.openSettings else actions.checkServer
        TaskKind.NOTIFICATIONS -> actions.allowNotifications
        TaskKind.USAGE -> actions.allowUsage
        TaskKind.BATTERY -> actions.openBattery
    }
    Panel(Modifier.padding(bottom = 10.dp), border = if (first) tint else c.line) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Ic(icon, tint = tint, size = 18) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (numbered) stringResource(R.string.task_numbered, index, t.title) else t.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Gap(2)
                Text(t.body, style = MaterialTheme.typography.bodyMedium, color = c.text2)
                Gap(10)
                SmallAction(t.action, onClick, color = if (first) c.accent else c.text, filled = first)
            }
        }
    }
}

@Composable
private fun BoardRow(a: AppRow, now: Long, actions: BoardActions) {
    val c = LocalPalette.current
    val s = strings()
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.details), role = Role.Button) { actions.showApp(a.slug) }
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark(a.name)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(toneColor(a.state.tone()), 7)
                Spacer(Modifier.width(6.dp))
                Text(
                    a.rowLine(s, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (a.state == AppState.STOPPED) c.bad else c.text2,
                )
            }
        }
        if (a.state == AppState.STOPPED) {
            val openApp = stringResource(R.string.action_open_app, a.name)
            SmallAction(stringResource(R.string.action_open), { actions.openApp(a) }, filled = true, modifier = Modifier.semantics { contentDescription = openApp })
        } else {
            Ic(R.drawable.ic_chevron_right, tint = c.muted, size = 18)
        }
    }
}

@Composable
private fun EarningsStrip(b: Board, actions: BoardActions) {
    val c = LocalPalette.current
    val e = b.earningsLine(strings())
    val tap = if (!b.paired) Modifier.clickable(role = Role.Button, onClick = actions.openSettings) else Modifier
    Panel(Modifier.clip(RoundedCornerShape(16.dp)).then(tap).semantics(mergeDescendants = true) {}, pad = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.label, style = MaterialTheme.typography.bodySmall, color = c.text2)
                when {
                    e.isFigure -> Text(e.value, style = MaterialTheme.typography.titleLarge.merge(Tnum), fontWeight = FontWeight.Bold)
                    !b.paired -> Text(e.value, fontWeight = FontWeight.SemiBold, color = c.accent)
                    else -> Text(e.value, fontWeight = FontWeight.SemiBold)
                }
            }
            if (e.staleNote != null) Pill(e.staleNote, Tone.WARN)
        }
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    val c = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.accent, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Ic(R.drawable.ic_chevron_right, tint = c.accent, size = 18)
    }
}

/** Tapping a row: everything known about one app, and the one thing to do. */
@Composable
fun AppDetailScreen(a: AppRow, b: Board, actions: BoardActions, onBack: () -> Unit) {
    val c = LocalPalette.current
    val s = strings()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackBar(null, onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Gap(8)
            AppMark(a.name, 64)
            Gap(16)
            Heading(a.name)
            Gap(6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(toneColor(a.state.tone()))
                Spacer(Modifier.width(8.dp))
                Text(
                    when (a.state) {
                        AppState.STOPPED -> stringResource(R.string.detail_stopped, b.ago(s, a.lastActive))
                        AppState.UNKNOWN -> stringResource(R.string.detail_unknown)
                        else -> a.stateWord(s, b.now)
                    },
                    color = if (a.state == AppState.STOPPED) c.bad else c.text2,
                )
            }
            Gap(20)
            if (a.state != AppState.NOT_INSTALLED) {
                Primary(stringResource(R.string.action_open_app, a.name), { actions.openApp(a) }, Modifier.fillMaxWidth())
                Gap(8)
                Text(
                    stringResource(R.string.detail_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
            Gap(24)
            Panel(pad = PaddingValues(0.dp)) {
                Fact(pluralStringResource(R.plurals.detail_server_reports, b.windowDays, b.windowDays), b.appEarnings(s, a))
                Divider()
                val (moved, opened) = b.measuredUse(s, a)
                Fact(stringResource(R.string.detail_data_moved), moved)
                Divider()
                Fact(stringResource(R.string.detail_last_opened), opened)
                Divider()
                // The whole row is the switch: one 56 dp target, one TalkBack stop.
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(a.state != AppState.DISABLED, role = Role.Switch) { actions.toggleApp(a.slug) }
                        .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.detail_watched), color = c.text2, modifier = Modifier.weight(1f))
                    Switch(checked = a.state != AppState.DISABLED, onCheckedChange = null)
                }
            }
            if (b.paired && a.shared && a.usd != null) {
                Gap(10)
                Text(stringResource(R.string.shared_footnote), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Gap(24)
        }
    }
}

@Composable
private fun Fact(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(k, color = LocalPalette.current.text2, modifier = Modifier.weight(1f))
        Text(v, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge.merge(Tnum))
    }
}

/**
 * The one state where the home screen can say nothing true: no access at all,
 * so every app reads unknown. The fix is the only thing on screen, and it is
 * calm, because nothing is known to be wrong.
 */
@Composable
fun BlindHome(b: Board, actions: BoardActions) {
    val c = LocalPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Gap(48)
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.raised), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_visibility_off, tint = c.text2, size = 28)
        }
        Gap(20)
        Heading(stringResource(R.string.blind_title), style = MaterialTheme.typography.headlineSmall)
        Gap(10)
        Text(
            pluralStringResource(R.plurals.blind_body, b.watched.size, b.watched.size),
            style = MaterialTheme.typography.bodyLarge,
            color = c.text2,
        )
        Gap(24)
        Primary(stringResource(R.string.allow_notification_access), actions.allowNotifications, Modifier.fillMaxWidth())
        Gap(10)
        Secondary(stringResource(R.string.allow_usage_access), actions.allowUsage, Modifier.fillMaxWidth())
        Gap(12)
        if (b.paired) {
            Gap(20)
            EarningsStrip(b, actions)
        }
        Gap(12)
        LinkRow(stringResource(R.string.settings_title), actions.openSettings)
        Gap(12)
        ServerLine(b)
    }
}

/**
 * Nothing supported is installed. Leads with what the app does, not with
 * install buttons: the supported list is one link down, and says which links
 * carry the referral code.
 */
@Composable
fun NoAppsHome(b: Board, actions: BoardActions) {
    val c = LocalPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = actions.openSettings, modifier = Modifier.size(48.dp)) {
                Ic(R.drawable.ic_settings, size = 24, description = stringResource(R.string.settings_title))
            }
        }
        Gap(8)
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.raised), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_query_stats, tint = c.text2, size = 28)
        }
        Gap(20)
        Heading(stringResource(R.string.no_apps_title), style = MaterialTheme.typography.headlineSmall)
        Gap(10)
        Text(
            stringResource(R.string.no_apps_body),
            style = MaterialTheme.typography.bodyLarge,
            color = c.text2,
        )
        Gap(24)
        Panel(Modifier.semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.no_apps_standalone_title), fontWeight = FontWeight.SemiBold)
            Gap(6)
            Text(
                stringResource(R.string.no_apps_standalone_body),
                style = MaterialTheme.typography.bodyMedium,
                color = c.text2,
            )
        }
        Gap(20)
        Secondary(stringResource(R.string.demo_start), actions.startDemo, Modifier.fillMaxWidth())
        Gap(8)
        LinkRow(pluralStringResource(R.plurals.no_apps_see_supported, b.apps.size, b.apps.size), actions.showSupported)
        Gap(24)
        ServerLine(b)
    }
}

/**
 * The supported apps, with the referral disclosure at the top. From the home
 * screen, the ones this phone does not have; from Settings ([all]), every one,
 * the installed ones with Open instead of Get.
 */
@Composable
fun SupportedAppsScreen(b: Board, actions: BoardActions, all: Boolean = false, onBack: () -> Unit) {
    val c = LocalPalette.current
    val rows = if (all) b.apps else b.notInstalled.ifEmpty { b.apps }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackBar(stringResource(R.string.supported_title), onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(if (all) R.string.supported_intro_all else R.string.supported_intro_missing),
                color = c.text2,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Panel(fill = c.raised, pad = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    stringResource(R.string.supported_referral_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.text2,
                )
            }
            Gap(12)
            Panel(pad = PaddingValues(vertical = 4.dp)) {
                rows.forEachIndexed { i, a ->
                    val have = a.state != AppState.NOT_INSTALLED
                    if (i > 0) Divider(Modifier.padding(start = 70.dp))
                    Row(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppMark(a.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(
                                    when {
                                        have -> R.string.supported_installed
                                        a.referralUrl != null -> R.string.supported_referral
                                        else -> R.string.supported_store
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.muted,
                            )
                        }
                        val named = stringResource(if (have) R.string.action_open_app else R.string.action_get_app, a.name)
                        SmallAction(
                            stringResource(if (have) R.string.action_open else R.string.action_get),
                            { if (have) actions.openApp(a) else actions.install(a) },
                            color = c.text,
                            modifier = Modifier.semantics { contentDescription = named },
                        )
                    }
                }
            }
            Gap(24)
        }
    }
}

/** Demo mode: the real screens with sample data, labelled on every screen. */
@Composable
fun DemoFrame(onExit: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalPalette.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(c.accentSoft).padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.demo_banner),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            SmallAction(stringResource(R.string.action_exit), onExit, color = c.accent)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

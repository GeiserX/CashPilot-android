package com.cashpilot.android.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.util.FormatUtils

/**
 * Direction C: fix what's broken.
 *
 * The first tab is a to-do list built from every problem the app can detect:
 * a stopped app, a server that stopped answering, a permission or battery
 * setting that will cause trouble. Each item has exactly one button that fixes
 * it. When the list is empty the screen says so and gets out of the way.
 */

private data class Task(val icon: Int, val tint: Color, val title: String, val body: String, val action: String)

@Composable
private fun tasksOf(h: Home): List<Task> {
    val c = LocalCp.current
    val out = mutableListOf<Task>()
    h.stopped.sortedBy { it.lastActive }.forEach {
        out += Task(R.drawable.ic_warning, c.bad, "${it.name} stopped ${ago(it.lastActive)}", "It isn't earning. Opening it usually starts it again.", "Open ${it.name}")
    }
    if (h.server == ServerState.FAILING) {
        out += Task(R.drawable.ic_cloud_off, c.warn, "Your server isn't answering", "Nothing has reached ${h.host} ${forHowLong(h.lastReport)}. Earnings below are from ${ago(h.earningsAsOf)}.", "Check connection")
    }
    if (!h.battery) {
        out += Task(R.drawable.ic_battery_alert, c.warn, "Android may pause CashPilot", "Background running is restricted, so reports can stop when the screen is off.", "Allow background")
    }
    return out
}

@Composable
private fun TaskCard(t: Task, first: Boolean) {
    val c = LocalCp.current
    Panel(Modifier.padding(bottom = 10.dp), border = if (first) t.tint else c.line) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(t.tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Ic(t.icon, tint = t.tint, size = 18)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Gap(2)
                Text(t.body, style = MaterialTheme.typography.bodyMedium, color = c.text2)
                Gap(12)
                SmallAction(t.action, color = if (first) c.accent else c.text, filled = first)
            }
        }
    }
}

@Composable
private fun CNav(selected: Int) {
    val c = LocalCp.current
    NavigationBar(containerColor = c.card, tonalElevation = 0.dp) {
        listOf("Today" to R.drawable.ic_check_circle, "Apps" to R.drawable.ic_query_stats, "Settings" to R.drawable.ic_settings)
            .forEachIndexed { i, (label, icon) ->
                NavigationBarItem(
                    selected = i == selected, onClick = {},
                    icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = c.accent, selectedTextColor = c.text, indicatorColor = c.accentSoft,
                        unselectedIconColor = c.muted, unselectedTextColor = c.muted,
                    ),
                )
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunningChips(rows: List<AppRow>) {
    val c = LocalCp.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { a ->
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(c.card).padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppMark(a.name, 22)
                Spacer(Modifier.width(8.dp))
                Text(a.name, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun CHome(h: Home) {
    if (h.blind) return BlindHome(h)
    val c = LocalCp.current
    val tasks = tasksOf(h)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Gap(36)
            if (tasks.isEmpty()) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(c.okSoft), contentAlignment = Alignment.Center) {
                    Ic(R.drawable.ic_check_circle, tint = c.ok, size = 30)
                }
                Gap(16)
                Text("Nothing to fix", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Gap(6)
                Text("${h.running.size} apps are earning on this phone.", style = MaterialTheme.typography.bodyLarge, color = c.text2)
            } else {
                Text(
                    if (tasks.size == 1) "1 thing needs you" else "${tasks.size} things need you",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                )
                Gap(6)
                Text("Most important first.", style = MaterialTheme.typography.bodyLarge, color = c.text2)
            }
            Gap(20)
            tasks.forEachIndexed { i, t -> TaskCard(t, i == 0) }
            if (tasks.isNotEmpty()) Gap(8)
            SectionLabel(if (tasks.isEmpty()) "Earning now" else "Fine (${h.running.size})")
            RunningChips(h.running)
            Gap(20)
            Panel(pad = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Earned in ${h.windowDays} days", style = MaterialTheme.typography.bodySmall, color = c.text2)
                        Text(
                            if (h.server == ServerState.NONE) "Needs a server" else money(h.total),
                            style = MaterialTheme.typography.titleMedium.merge(Tnum), fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Ic(R.drawable.ic_chevron_right, tint = c.muted, size = 18)
                }
            }
            Gap(12)
            ServerLine(h, Modifier.padding(start = 4.dp))
        }
        CNav(0)
    }
}

/** Second tab: every supported app, filterable, nothing hidden. */
@Composable
fun CApps(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            TopBar("Apps")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Watched ${h.watched.size}" to true, "Stopped ${h.stopped.size}" to false, "Not installed ${h.notInstalled.size}" to false).forEach { (t, sel) ->
                    FilterChip(
                        selected = sel, onClick = {}, label = { Text(t) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.accentSoft, selectedLabelColor = c.text, labelColor = c.text2),
                    )
                }
            }
            Gap(8)
            Panel(Modifier.padding(horizontal = 16.dp), pad = PaddingValues(vertical = 4.dp)) {
                h.watched.forEachIndexed { i, a ->
                    if (i > 0) Box(Modifier.padding(start = 70.dp)) { Divider() }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppMark(a.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (a.state == AppState.RUNNING) "Running · ${FormatUtils.formatBytes(a.bytes24h)} today" else stateWord(a),
                                style = MaterialTheme.typography.bodySmall, color = if (a.state == AppState.STOPPED) c.bad else c.text2,
                            )
                        }
                        Text(money(a.usd), style = MaterialTheme.typography.bodyLarge.merge(Tnum), color = if (a.usd == null) c.muted else c.text)
                    }
                }
            }
        }
        CNav(1)
    }
}

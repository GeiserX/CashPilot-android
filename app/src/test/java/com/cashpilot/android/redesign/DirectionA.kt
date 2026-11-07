package com.cashpilot.android.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.util.FormatUtils

/**
 * Direction A: the status board.
 *
 * One sentence answers the question the app is opened for ("is everything
 * earning?"), then one quiet row per app. Colour appears only where something
 * is wrong, so a healthy phone is a calm screen and a broken one is not.
 */

@Composable
private fun Verdict(h: Home) {
    val c = LocalCp.current
    val bad = h.stopped.size
    val (ring, title, sub) = when {
        bad > 0 -> Triple(c.bad, "$bad of ${h.watched.size} apps stopped", "Opening a stopped app usually starts it again.")
        h.server == ServerState.FAILING -> Triple(c.warn, "All ${h.watched.size} apps running", "But your server hasn't heard from this phone.")
        else -> Triple(c.ok, "All ${h.watched.size} apps running", "Nothing needs you right now.")
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Box(Modifier.size(64.dp).clip(CircleShape).border(5.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (bad > 0) {
                Text("$bad", style = MaterialTheme.typography.headlineSmall.merge(Tnum), fontWeight = FontWeight.Bold, color = ring)
            } else {
                Ic(R.drawable.ic_check_circle, tint = ring, size = 32)
            }
        }
        Gap(16)
        Text(title, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        Gap(6)
        Text(sub, style = MaterialTheme.typography.bodyLarge, color = c.text2)
        Gap(14)
        ServerLine(h)
    }
}

@Composable
private fun BoardRow(a: AppRow) {
    val c = LocalCp.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppMark(a.name)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(stateColor(a.state), 7)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (a.state == AppState.RUNNING) "Running · ${FormatUtils.formatBytes(a.bytes24h)} today" else stateWord(a),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (a.state == AppState.STOPPED) c.bad else c.text2,
                )
            }
        }
        if (a.state == AppState.STOPPED) SmallAction("Open", filled = true) else Ic(R.drawable.ic_chevron_right, tint = c.muted, size = 18)
    }
}

@Composable
private fun EarningsStrip(h: Home) {
    val c = LocalCp.current
    Panel(pad = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your server reports, ${h.windowDays} days", style = MaterialTheme.typography.bodySmall, color = c.text2)
                when {
                    h.server == ServerState.NONE -> Text("Add a server to see figures", fontWeight = FontWeight.SemiBold, color = c.accent)
                    h.total == null -> Text("Nothing read yet", fontWeight = FontWeight.SemiBold)
                    else -> Text(money(h.total), style = MaterialTheme.typography.titleLarge.merge(Tnum), fontWeight = FontWeight.Bold)
                }
            }
            if (h.stale) Pill("From ${ago(h.earningsAsOf)}", c.warn, c.warnSoft) else Ic(R.drawable.ic_chevron_right, tint = c.muted, size = 18)
        }
    }
}

@Composable
fun AHome(h: Home) {
    if (h.blind) return BlindHome(h)
    if (h.watched.isEmpty()) return NoAppsHome(h)
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        TopBar("CashPilot") { Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_settings) } }
        Gap(12)
        Verdict(h)
        Gap(24)
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (h.server == ServerState.FAILING) {
                Panel(border = c.warn, fill = c.warnSoft, pad = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reports are queued until the server answers.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        SmallAction("Check", c.warn)
                    }
                }
                Gap(12)
            }
            Panel(pad = PaddingValues(vertical = 4.dp)) {
                h.watched.forEachIndexed { i, a ->
                    if (i > 0) Box(Modifier.padding(start = 70.dp)) { Divider() }
                    BoardRow(a)
                }
            }
            Gap(12)
            EarningsStrip(h)
            Gap(12)
            Text(
                "${h.notInstalled.size} other supported apps  ›",
                style = MaterialTheme.typography.bodyMedium, color = c.accent, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** Tapping a row: everything known about one app, and the one thing to do. */
@Composable
fun AAppDetail(a: AppRow, h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_arrow_back, tint = c.text) }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Gap(8)
            AppMark(a.name, 64)
            Gap(16)
            Text(a.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Gap(6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(stateColor(a.state))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (a.state == AppState.STOPPED) "Stopped. Last seen working ${ago(a.lastActive)}." else stateWord(a),
                    color = if (a.state == AppState.STOPPED) c.bad else c.text2,
                )
            }
            Gap(20)
            Primary("Open ${a.name}", Modifier.fillMaxWidth())
            Gap(8)
            Text(
                "These apps often stop when Android clears them from memory. Opening the app usually starts it again.",
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
            Gap(24)
            Panel(pad = PaddingValues(0.dp)) {
                Fact2("Server reports, ${h.windowDays} days", money(a.usd) + if (a.shared) " · whole account" else "")
                Divider()
                Fact2("Data moved, last 24 h", FormatUtils.formatBytes(a.bytes24h))
                Divider()
                Fact2("Last seen working", ago(a.lastActive))
                Divider()
                Fact2("Watched by CashPilot", "Yes · stop watching")
            }
        }
    }
}

@Composable
private fun Fact2(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(k, color = LocalCp.current.text2, modifier = Modifier.weight(1f))
        Text(v, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge.merge(Tnum))
    }
}

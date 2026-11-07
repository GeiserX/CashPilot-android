package com.cashpilot.android.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

/**
 * Direction B: the ledger.
 *
 * Money first, because money is why these apps are installed. The headline is
 * the 30-day figure the server already sends; every row is an app with what it
 * earned and, beside it, whether it is still earning. A stopped app reads as a
 * line in the ledger that has gone red.
 */

private val series = listOf(0xFFF472B6, 0xFF22D3EE, 0xFF8B5CF6, 0xFF46B37F, 0xFFE0A53C, 0xFF3987E5).map { Color(it) }

@Composable
private fun ShareBar(rows: List<AppRow>, total: Double) {
    val c = LocalCp.current
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(c.raised)) {
        rows.forEachIndexed { i, r ->
            val w = ((r.usd ?: 0.0) / total).toFloat()
            if (w > 0f) Box(Modifier.weight(w).fillMaxHeight().background(series[i % series.size]))
        }
    }
}

@Composable
private fun Hero(h: Home) {
    val c = LocalCp.current
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Earned in the last ${h.windowDays} days", color = c.text2)
        Gap(4)
        if (h.server == ServerState.NONE) {
            Text("—", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = c.muted)
            Gap(6)
            Text("Earnings come from your CashPilot server, which reads each provider's account.", color = c.text2, style = MaterialTheme.typography.bodyMedium)
            Gap(14)
            Primary("Connect a server")
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(money(h.total), fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold, style = Tnum)
                Spacer(Modifier.width(10.dp))
                if (h.stale) Box(Modifier.padding(bottom = 12.dp)) { Pill("as of ${ago(h.earningsAsOf)}", c.warn, c.warnSoft) }
            }
            Gap(6)
            Text(
                "By the apps on this phone, read from your providers by ${h.host}.",
                style = MaterialTheme.typography.bodySmall, color = c.muted,
            )
            Gap(16)
            val ranked = h.watched.filter { it.usd != null }.sortedByDescending { it.usd }
            ShareBar(ranked, h.total ?: 1.0)
        }
    }
}

@Composable
private fun LedgerRow(a: AppRow, i: Int, paired: Boolean) {
    val c = LocalCp.current
    val stopped = a.state == AppState.STOPPED
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, 36.dp).clip(RoundedCornerShape(2.dp)).background(if (a.usd != null) series[i % series.size] else c.line))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name + if (a.shared) " *" else "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(stateColor(a.state), 7)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (stopped) "${stateWord(a)} · not earning" else stateWord(a),
                    style = MaterialTheme.typography.bodySmall, color = if (stopped) c.bad else c.text2,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (paired) Text(
                if (a.usd == null) "no reading" else money(a.usd),
                style = (if (a.usd == null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium).merge(Tnum),
                fontWeight = if (a.usd == null) FontWeight.Normal else FontWeight.SemiBold,
                color = if (a.usd == null) c.muted else c.text,
            )
            if (stopped) { Gap(4); SmallAction("Open", filled = true) }
        }
    }
}

@Composable
fun BHome(h: Home) {
    if (h.blind) return BlindHome(h)
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        TopBar("CashPilot") { Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_settings) } }
        Gap(8)
        Hero(h)
        Gap(20)
        Column(Modifier.padding(horizontal = 16.dp)) {
            ServerLine(h, Modifier.padding(start = 4.dp, bottom = 12.dp))
            // Highest earner first; apps with no reading sink to the bottom,
            // because "no reading" is not a small number.
            val rows = h.watched.sortedWith(compareBy<AppRow>({ it.usd == null }, { -(it.usd ?: 0.0) }))
            Panel(pad = PaddingValues(vertical = 4.dp)) {
                rows.forEachIndexed { i, a ->
                    if (i > 0) Box(Modifier.padding(start = 32.dp)) { Divider() }
                    LedgerRow(a, i, paired = h.server != ServerState.NONE)
                }
            }
            Gap(10)
            if (h.watched.any { it.shared }) {
                Text(
                    "* Runs on more than one of your devices. The provider reports one balance for the account, so this is not this phone alone.",
                    style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (h.withoutReading.isNotEmpty() && h.server != ServerState.NONE) {
                Gap(6)
                Text(
                    "No reading means your server has no login for that provider yet. It is not \$0.",
                    style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** The apps this phone could also run, framed as what they are: more ways to earn. */
@Composable
fun BAddApps(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_arrow_back, tint = c.text) }
            Text("Add an earning app", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "CashPilot supports ${h.notInstalled.size} more apps. Each one shares a little of your connection and pays for it; running several adds up.",
                color = c.text2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Gap(8)
            Panel(pad = PaddingValues(vertical = 4.dp)) {
                h.notInstalled.forEachIndexed { i, a ->
                    if (i > 0) Box(Modifier.padding(start = 70.dp)) { Divider() }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppMark(a.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (referralOf(a.slug) != null) "Sign up with our invite, then install" else "Install from your app store",
                                style = MaterialTheme.typography.bodySmall, color = c.text2,
                            )
                        }
                        SmallAction("Get")
                    }
                }
            }
            Gap(10)
            Text(
                "Invite links support CashPilot's development at no cost to you.",
                style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

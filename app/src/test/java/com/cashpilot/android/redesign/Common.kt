package com.cashpilot.android.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppState

// ---------------------------------------------------------------------------
// Onboarding: three short steps instead of one long page. Required things
// (the two permissions) come first; the optional server comes last, so skipping
// it is the natural end of the flow, not a button hunt.
// ---------------------------------------------------------------------------

@Composable
private fun StepDots(at: Int) {
    val c = LocalCp.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier.size(width = if (i == at) 22.dp else 8.dp, height = 8.dp).clip(RoundedCornerShape(50))
                    .background(if (i <= at) c.accent else c.line),
            )
        }
    }
}

@Composable
private fun OnboardFrame(step: Int, title: String, lead: String, bottom: @Composable () -> Unit, body: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 40.dp, bottom = 24.dp)) {
        StepDots(step)
        Gap(28)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Gap(10)
        Text(lead, style = MaterialTheme.typography.bodyLarge, color = LocalCp.current.text2)
        Gap(28)
        Column(Modifier.weight(1f)) { body() }
        bottom()
    }
}

@Composable
private fun Fact(icon: Int, title: String, body: String) {
    val c = LocalCp.current
    Row(Modifier.padding(bottom = 20.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Ic(icon, tint = c.accent)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Gap(2)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.text2)
        }
    }
}

@Composable
fun OnboardWelcome() = OnboardFrame(
    0, "Know when an earning app stops",
    "CashPilot checks the passive-income apps on this phone and shows you which ones stopped, and how to start them again.",
    bottom = { Primary("Get started", Modifier.fillMaxWidth()) },
) {
    Fact(R.drawable.ic_check_circle, "Which apps are running", "EarnApp, Pawns, Grass, MystNodes and 7 more, checked whenever you open CashPilot.")
    Fact(R.drawable.ic_warning, "What to do when one stops", "One tap opens the app so you can start it again.")
    Fact(R.drawable.ic_cloud, "Figures from your server, if you have one", "A CashPilot server reports what each app's account shows. Optional; reports go every 15 minutes.")
    Fact(R.drawable.ic_visibility_off, "Nothing leaves this phone", "Except to your own server, if you add one. No ads, no tracking.")
}

@Composable
private fun PermissionCard(icon: Int, title: String, why: String, notRead: String, granted: Boolean, required: Boolean) {
    val c = LocalCp.current
    Panel(Modifier.padding(bottom = 12.dp), border = if (granted) c.line else if (required) c.lineStrong else c.line) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ic(icon, tint = if (granted) c.ok else c.text)
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            when {
                granted -> Pill("Allowed", c.ok, c.okSoft)
                required -> SmallAction("Allow", filled = true)
                else -> SmallAction("Allow")
            }
        }
        Gap(8)
        Text(why, style = MaterialTheme.typography.bodyMedium, color = c.text2)
        Gap(4)
        Text(notRead, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
fun OnboardPermissions(notif: Boolean, usage: Boolean, battery: Boolean) = OnboardFrame(
    1, "Let CashPilot see your apps",
    "Android hides other apps' state. Two switches in system settings fix that; you come straight back here.",
    bottom = {
        val c = LocalCp.current
        if (!(notif && usage)) {
            Text(
                "Allow both to continue. Without them CashPilot can't tell running from stopped.",
                style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Primary("Continue", Modifier.fillMaxWidth(), enabled = notif && usage)
    },
) {
    PermissionCard(
        R.drawable.ic_notifications, "Notification access",
        "These apps keep a notification up while they run. CashPilot checks whether it is there, for the 11 supported apps only.",
        "Never reads notification text. You'll see exactly what it reads before Android asks.", notif, required = true,
    )
    PermissionCard(
        R.drawable.ic_query_stats, "Usage access",
        "Shows when each supported app last ran and how much data it moved today, which shows it is working.",
        "Never looks at other apps or websites. You'll see exactly what it reads first.", usage, required = true,
    )
    PermissionCard(
        R.drawable.ic_battery_alert, "Background running",
        "Opens Android's battery list, where you can set CashPilot to Don't optimize.",
        "Optional. Keeps the 15-minute reports on time with the screen off.", battery, required = false,
    )
}

enum class PairResult { NONE, OK, KEY_REFUSED }

@Composable
fun OnboardServer(result: PairResult) = OnboardFrame(
    2, "Connect your server",
    "Optional. Without one you still see which apps run and which stopped. A server adds the figures its provider accounts show, and lists this phone in your fleet.",
    bottom = {
        Primary(if (result == PairResult.OK) "Finish" else "Test and finish", Modifier.fillMaxWidth(), enabled = result != PairResult.KEY_REFUSED)
        Gap(8)
        Secondary("Use without a server", Modifier.fillMaxWidth())
    },
) {
    val c = LocalCp.current
    OutlinedTextField(
        value = if (result == PairResult.NONE) "" else "https://cashpilot.home.example", onValueChange = {},
        label = { Text("Server address") }, placeholder = { Text("https://cashpilot.example.com") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Gap(12)
    OutlinedTextField(
        value = if (result == PairResult.NONE) "" else "cp_live_8f2c1d9a7b", onValueChange = {},
        label = { Text("API key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        supportingText = { Text("On your server: Fleet → Reveal API Key. Stored only on this phone.") },
    )
    Gap(4)
    Text(
        "Use https://. Plain http:// works only for an address on your own network, such as 192.168.x.x or a .local or Tailscale name.",
        style = MaterialTheme.typography.bodySmall, color = c.muted,
    )
    Gap(16)
    when (result) {
        PairResult.NONE -> Unit
        PairResult.OK -> Panel(border = c.ok, fill = c.okSoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ic(R.drawable.ic_check_circle, tint = c.ok)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Connected", fontWeight = FontWeight.SemiBold)
                    Text("Your server answered and accepted the key. This phone will appear in Fleet as Pixel 8.", style = MaterialTheme.typography.bodySmall, color = c.text2)
                }
            }
        }
        PairResult.KEY_REFUSED -> Panel(border = c.bad, fill = c.badSoft) {
            Row(verticalAlignment = Alignment.Top) {
                Ic(R.drawable.ic_warning, tint = c.bad)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("The server refused this API key", fontWeight = FontWeight.SemiBold)
                    Text("The address is right: the server answered. Copy the key again from Fleet → Reveal API Key.", style = MaterialTheme.typography.bodySmall, color = c.text2)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// The one state where the home screen can say nothing true. Every direction
// shows this instead of its home, with the fix as the only thing on screen.
// ---------------------------------------------------------------------------

@Composable
fun BlindHome(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.warnSoft), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_visibility_off, tint = c.warn, size = 28)
        }
        Gap(20)
        Text("CashPilot can't see your apps yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Gap(10)
        Text(
            "${h.watched.size} supported apps are installed, but Android won't show CashPilot whether they run until you allow two things. Each one explains what it reads first.",
            style = MaterialTheme.typography.bodyLarge, color = c.text2,
        )
        Gap(24)
        Primary("Allow notification access", Modifier.fillMaxWidth())
        Gap(10)
        Secondary("Allow usage access", Modifier.fillMaxWidth())
        Gap(24)
        ServerLine(h)
    }
}

// ---------------------------------------------------------------------------
// Settings: grouped by what the user is checking, every row says its state.
// ---------------------------------------------------------------------------

@Composable
private fun SettingRow(title: String, value: String, valueColor: Color = LocalCp.current.text2, trailing: @Composable () -> Unit = { Ic(R.drawable.ic_chevron_right, size = 18) }) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
        }
        trailing()
    }
}

@Composable
fun SettingsCommon(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 24.dp, end = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_arrow_back, tint = c.text) }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(8)
            SectionLabel("Server")
            Panel(pad = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                if (h.server == ServerState.FAILING) {
                    SettingRow(h.host, "Not answering · last report ${ago(h.lastReport)}", c.warn)
                } else {
                    SettingRow(h.host, "Reports every 15 min · last report ${ago(h.lastReport)}", c.ok)
                }
                Divider()
                SettingRow("API key", "•••• •••• 7b9a · stored on this phone only")
                Divider()
                SettingRow("Test connection", "Sends a report now and shows what the server said")
            }
            Gap(20)
            SectionLabel("This phone")
            Panel(pad = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                SettingRow("Notification access", if (h.notif) "Allowed" else "Off · can't see running apps", if (h.notif) c.ok else c.bad)
                Divider()
                SettingRow("Usage access", if (h.usage) "Allowed" else "Off · no last-seen times", if (h.usage) c.ok else c.bad)
                Divider()
                SettingRow("Battery optimisation", if (h.battery) "Off for CashPilot" else "On · reports may arrive late", if (h.battery) c.ok else c.warn)
            }
            Gap(20)
            SectionLabel("Apps to watch")
            Panel(pad = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                h.watched.take(3).forEachIndexed { i, a ->
                    if (i > 0) Divider()
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppMark(a.name, 32)
                        Spacer(Modifier.width(12.dp))
                        Text(a.name, Modifier.weight(1f))
                        Switch(checked = a.state != AppState.DISABLED, onCheckedChange = {})
                    }
                }
                Divider()
                SettingRow("All ${h.apps.size} supported apps", "${h.watched.size} installed and watched · ${h.notInstalled.size} not installed")
            }
            Gap(20)
            SectionLabel("About")
            Panel(pad = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                SettingRow("CashPilot for Android 0.4.0", "Source code, licence and the server project")
                Divider()
                SettingRow("Demo mode", "Shows the screens with sample data")
            }
        }
    }
}

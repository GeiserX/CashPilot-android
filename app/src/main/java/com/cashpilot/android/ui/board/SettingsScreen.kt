package com.cashpilot.android.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.ui.theme.LocalPalette

/** What Settings can ask for, on top of the board actions. */
data class SettingsActions(
    val onUrl: (String) -> Unit = {},
    val onKey: (String) -> Unit = {},
    val testConnection: () -> Unit = {},
    val openUrl: (String) -> Unit = {},
)

/** The server fields as typed, and what the last test said. */
data class ServerForm(val url: String, val key: String, val savedKey: String, val result: PairResult)

const val SOURCE_URL = "https://github.com/GeiserX/CashPilot-android"
const val SERVER_PROJECT_URL = "https://github.com/GeiserX/CashPilot"
const val SPONSOR_URL = "https://github.com/sponsors/GeiserX"

@Composable
private fun SettingRow(
    title: String,
    value: String,
    valueColor: Color = LocalPalette.current.text2,
    onClick: (() -> Unit)? = null,
) {
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics(mergeDescendants = true) {}
    Row(
        Modifier.fillMaxWidth().then(tap).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
        }
        if (onClick != null) Ic(R.drawable.ic_chevron_right, size = 18)
    }
}

/** Grouped by what the user is checking; every row says its current state. */
@Composable
fun SettingsScreen(
    b: Board,
    form: ServerForm,
    version: String,
    deviceName: String,
    actions: BoardActions,
    settings: SettingsActions,
    onBack: () -> Unit,
) {
    val c = LocalPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackBar("Settings", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(8)
            SectionLabel("Server")
            Panel(pad = PaddingValues(0.dp)) {
                val s = b.serverSetting()
                SettingRow(if (b.paired) b.host else "No server", s.text, toneColor(s.tone))
                Divider()
                SettingRow("API key", Words.maskKey(form.savedKey) + if (form.savedKey.isNotBlank()) " · stored on this phone only" else "")
                Divider()
                Column(Modifier.padding(16.dp)) {
                    ServerFields(form.url, form.key, settings.onUrl, settings.onKey)
                    Gap(12)
                    Secondary(
                        if (form.result == PairResult.TESTING) "Testing…" else "Test connection",
                        settings.testConnection,
                        Modifier.fillMaxWidth(),
                    )
                    Gap(4)
                    Text("Sends a report now and shows what the server said.", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    if (form.result != PairResult.NONE) {
                        Gap(12)
                        PairNotice(form.result, Board.hostOf(form.url), deviceName)
                    }
                }
            }
            Gap(20)
            SectionLabel("This phone")
            Panel(pad = PaddingValues(0.dp)) {
                SettingRow(
                    "Notification access",
                    if (b.notif) "Allowed" else "Off · can't see running apps",
                    if (b.notif) c.ok else c.bad,
                    actions.allowNotifications,
                )
                Divider()
                SettingRow(
                    "Usage access",
                    if (b.usage) "Allowed" else "Off · no last-seen times",
                    if (b.usage) c.ok else c.bad,
                    actions.allowUsage,
                )
                Divider()
                SettingRow(
                    "Battery optimisation",
                    if (b.battery) "Off for CashPilot" else "On · reports may arrive late",
                    if (b.battery) c.ok else c.warn,
                    actions.openBattery,
                )
            }
            Gap(20)
            SectionLabel("Apps to watch")
            Panel(pad = PaddingValues(0.dp)) {
                b.installed.forEachIndexed { i, a ->
                    if (i > 0) Divider()
                    // The whole row is the switch: one 56 dp target, one TalkBack stop.
                    Row(
                        Modifier.fillMaxWidth()
                            .toggleable(a.state != AppState.DISABLED, role = Role.Switch) { actions.toggleApp(a.slug) }
                            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppMark(a.name, 32)
                        Spacer(Modifier.width(12.dp))
                        Text(a.name, Modifier.weight(1f))
                        Switch(checked = a.state != AppState.DISABLED, onCheckedChange = null)
                    }
                }
                if (b.installed.isNotEmpty()) Divider()
                SettingRow(
                    "All ${b.apps.size} supported apps",
                    "${b.installed.size} installed · ${b.notInstalled.size} not installed",
                    onClick = actions.showAllSupported,
                )
            }
            Gap(20)
            SectionLabel("About")
            Panel(pad = PaddingValues(0.dp)) {
                SettingRow("CashPilot for Android $version", "Source code and licence (GPL-3.0)", onClick = { settings.openUrl(SOURCE_URL) })
                Divider()
                SettingRow("CashPilot server", "The self-hosted dashboard this app reports to", onClick = { settings.openUrl(SERVER_PROJECT_URL) })
                Divider()
                SettingRow("Sponsor the project", "GitHub Sponsors", onClick = { settings.openUrl(SPONSOR_URL) })
                Divider()
                SettingRow("Demo mode", "Shows the screens with sample data", onClick = actions.startDemo)
            }
            Gap(24)
        }
    }
}

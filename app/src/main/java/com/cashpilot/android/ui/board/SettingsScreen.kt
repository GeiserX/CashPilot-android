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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
    val openLanguage: () -> Unit = {},
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
    /** The language picked in the app, or "" when it follows the system. */
    language: String,
    actions: BoardActions,
    settings: SettingsActions,
    onBack: () -> Unit,
) {
    val c = LocalPalette.current
    val s = strings()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackBar(stringResource(R.string.settings_title), onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(8)
            SectionLabel(stringResource(R.string.settings_section_server))
            Panel(pad = PaddingValues(0.dp)) {
                val line = b.serverSetting(s)
                SettingRow(if (b.paired) b.host else stringResource(R.string.settings_no_server), line.text, toneColor(line.tone))
                Divider()
                // Left-to-right marks around the key, so in Arabic the dots stay
                // before its last four characters instead of moving after them.
                val masked = "\u200E" + Words.maskKey(s, form.savedKey) + "\u200E"
                SettingRow(stringResource(R.string.api_key), if (form.savedKey.isNotBlank()) stringResource(R.string.settings_key_stored, masked) else masked)
                Divider()
                Column(Modifier.padding(16.dp)) {
                    ServerFields(form.url, form.key, settings.onUrl, settings.onKey)
                    Gap(12)
                    Secondary(
                        stringResource(if (form.result == PairResult.TESTING) R.string.testing else R.string.action_test_connection),
                        settings.testConnection,
                        Modifier.fillMaxWidth(),
                    )
                    Gap(4)
                    Text(stringResource(R.string.settings_test_hint), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    if (form.result != PairResult.NONE) {
                        Gap(12)
                        PairNotice(form.result, Board.hostOf(form.url), deviceName)
                    }
                }
            }
            Gap(20)
            SectionLabel(stringResource(R.string.settings_section_phone))
            Panel(pad = PaddingValues(0.dp)) {
                SettingRow(
                    stringResource(R.string.notification_access),
                    stringResource(if (b.notif) R.string.allowed else R.string.settings_notif_off),
                    if (b.notif) c.ok else c.bad,
                    actions.allowNotifications,
                )
                Divider()
                SettingRow(
                    stringResource(R.string.usage_access),
                    stringResource(if (b.usage) R.string.allowed else R.string.settings_usage_off),
                    if (b.usage) c.ok else c.bad,
                    actions.allowUsage,
                )
                Divider()
                SettingRow(
                    stringResource(R.string.battery_optimisation),
                    stringResource(if (b.battery) R.string.settings_battery_off else R.string.settings_battery_on),
                    if (b.battery) c.ok else c.warn,
                    actions.openBattery,
                )
                Divider()
                SettingRow(stringResource(R.string.language), languageLabel(language), onClick = settings.openLanguage)
            }
            Gap(20)
            SectionLabel(stringResource(R.string.settings_section_apps))
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
                    pluralStringResource(R.plurals.settings_all_supported, b.apps.size, b.apps.size),
                    stringResource(R.string.settings_installed_count, b.installed.size, b.notInstalled.size),
                    onClick = actions.showAllSupported,
                )
            }
            Gap(20)
            SectionLabel(stringResource(R.string.settings_section_about))
            Panel(pad = PaddingValues(0.dp)) {
                SettingRow(stringResource(R.string.settings_about_app, version), stringResource(R.string.settings_about_source), onClick = { settings.openUrl(SOURCE_URL) })
                Divider()
                SettingRow(stringResource(R.string.settings_about_server), stringResource(R.string.settings_about_server_body), onClick = { settings.openUrl(SERVER_PROJECT_URL) })
                Divider()
                SettingRow(stringResource(R.string.settings_about_sponsor), stringResource(R.string.settings_about_sponsor_body), onClick = { settings.openUrl(SPONSOR_URL) })
                Divider()
                SettingRow(stringResource(R.string.settings_demo), stringResource(R.string.settings_demo_body), onClick = actions.startDemo)
            }
            Gap(24)
        }
    }
}

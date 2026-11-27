package com.cashpilot.android.ui.board

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.ui.theme.LocalPalette
import com.cashpilot.android.util.ServerUrlPolicy

// ---------------------------------------------------------------------------
// Onboarding: three short steps. The two permissions come first; the optional
// server comes last, so skipping it is the natural end of the flow.
// ---------------------------------------------------------------------------

@Composable
private fun StepDots(at: Int) {
    val c = LocalPalette.current
    val step = stringResource(R.string.onboard_step, at + 1, 3)
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = step },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { i ->
            Box(
                Modifier.size(width = if (i == at) 22.dp else 8.dp, height = 8.dp).clip(RoundedCornerShape(50))
                    .background(if (i <= at) c.accent else c.line),
            )
        }
    }
}

/** Title and lead on top, the body scrolls, the buttons stay at the bottom at any font size. */
@Composable
private fun OnboardFrame(step: Int, title: String, lead: String, bottom: @Composable ColumnScope.() -> Unit, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (step >= 0) StepDots(step)
            Gap(24)
            Heading(title)
            Gap(10)
            Text(lead, style = MaterialTheme.typography.bodyLarge, color = LocalPalette.current.text2)
            Gap(24)
            body()
        }
        Gap(12)
        bottom()
    }
}

@Composable
private fun Fact(@DrawableRes icon: Int, title: String, body: String) {
    val c = LocalPalette.current
    Row(Modifier.padding(bottom = 20.dp).semantics(mergeDescendants = true) {}) {
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
fun OnboardWelcome(onNext: () -> Unit) = OnboardFrame(
    0,
    stringResource(R.string.welcome_title),
    stringResource(R.string.welcome_lead),
    bottom = { Primary(stringResource(R.string.welcome_start), onNext, Modifier.fillMaxWidth()) },
) {
    Fact(R.drawable.ic_check_circle, stringResource(R.string.welcome_fact_running_title), stringResource(R.string.welcome_fact_running_body, Words.supportedSample(strings())))
    Fact(R.drawable.ic_warning, stringResource(R.string.welcome_fact_stops_title), stringResource(R.string.welcome_fact_stops_body))
    Fact(R.drawable.ic_cloud, stringResource(R.string.welcome_fact_server_title), stringResource(R.string.welcome_fact_server_body))
    Fact(R.drawable.ic_visibility_off, stringResource(R.string.welcome_fact_private_title), stringResource(R.string.welcome_fact_private_body))
}

@Composable
private fun PermissionCard(
    @DrawableRes icon: Int,
    title: String,
    /** What TalkBack says for the Allow button: "Allow notification access". */
    allowDescription: String,
    why: String,
    notRead: String,
    granted: Boolean,
    required: Boolean,
    onAllow: () -> Unit,
) {
    val c = LocalPalette.current
    Panel(Modifier.padding(bottom = 12.dp), border = if (!granted && required) c.lineStrong else c.line) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ic(icon, tint = if (granted) c.ok else c.text)
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            when {
                granted -> Pill(stringResource(if (required) R.string.allowed else R.string.done), Tone.OK)
                required -> SmallAction(stringResource(R.string.action_allow), onAllow, filled = true, modifier = Modifier.semantics { contentDescription = allowDescription })
                // Optional, and not a permission: it only opens Android's
                // battery list. No exemption dialog, so it never says "Allow".
                else -> SmallAction(stringResource(R.string.action_open_settings), onAllow, color = c.text, modifier = Modifier.semantics { contentDescription = allowDescription })
            }
        }
        Gap(8)
        Text(why, style = MaterialTheme.typography.bodyMedium, color = c.text2)
        Gap(4)
        Text(notRead, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
fun OnboardPermissions(
    notif: Boolean,
    usage: Boolean,
    battery: Boolean,
    onAllowNotifications: () -> Unit,
    onAllowUsage: () -> Unit,
    onOpenBattery: () -> Unit,
    onNext: () -> Unit,
) = OnboardFrame(
    1,
    stringResource(R.string.permissions_title),
    stringResource(R.string.permissions_lead),
    bottom = {
        val c = LocalPalette.current
        if (!(notif && usage)) {
            Text(
                stringResource(R.string.permissions_without),
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Primary(stringResource(if (notif && usage) R.string.permissions_continue else R.string.permissions_continue_without), onNext, Modifier.fillMaxWidth())
    },
) {
    val n = KnownApps.all.size
    PermissionCard(
        R.drawable.ic_notifications, stringResource(R.string.notification_access), stringResource(R.string.allow_notification_access),
        stringResource(R.string.permissions_notif_why, n),
        stringResource(R.string.permissions_notif_not_read),
        notif, required = true, onAllow = onAllowNotifications,
    )
    PermissionCard(
        R.drawable.ic_query_stats, stringResource(R.string.usage_access), stringResource(R.string.allow_usage_access),
        stringResource(R.string.permissions_usage_why),
        stringResource(R.string.permissions_usage_not_read),
        usage, required = true, onAllow = onAllowUsage,
    )
    PermissionCard(
        R.drawable.ic_battery_alert, stringResource(R.string.battery_optimisation), stringResource(R.string.action_open_battery),
        stringResource(R.string.permissions_battery_why),
        stringResource(R.string.permissions_battery_not_read),
        battery, required = false, onAllow = onOpenBattery,
    )
}

/** The address and key fields, shared by onboarding and Settings so both enforce the same rules. */
@Composable
fun ServerFields(url: String, key: String, onUrl: (String) -> Unit, onKey: (String) -> Unit) {
    val c = LocalPalette.current
    val refused = url.isNotBlank() && "://" in url && !ServerUrlPolicy.allowsToken(url)
    val cleartext = !refused && url.trim().startsWith("http://", ignoreCase = true)
    OutlinedTextField(
        value = url,
        onValueChange = onUrl,
        label = { Text(stringResource(R.string.server_address)) },
        placeholder = { Text("https://cashpilot.example.com") },
        singleLine = true,
        isError = refused,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        supportingText = when {
            refused -> { { Text(stringResource(R.string.server_address_refused)) } }
            cleartext -> { { Text(stringResource(R.string.server_address_cleartext)) } }
            else -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Gap(12)
    OutlinedTextField(
        value = key,
        onValueChange = onKey,
        label = { Text(stringResource(R.string.api_key)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = { Text(stringResource(R.string.api_key_where)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Gap(4)
    Text(
        stringResource(R.string.server_address_hint),
        style = MaterialTheme.typography.bodySmall,
        color = c.muted,
    )
}

@Composable
fun PairNotice(result: PairResult, host: String, deviceName: String) {
    val m = pairMessage(strings(), result, host, deviceName) ?: return
    Notice(
        m.tone, m.title, m.body,
        when (m.tone) {
            Tone.OK -> R.drawable.ic_check_circle
            Tone.MUTED -> R.drawable.ic_cloud
            else -> R.drawable.ic_warning
        },
    )
}

@Composable
fun OnboardServer(
    url: String,
    key: String,
    onUrl: (String) -> Unit,
    onKey: (String) -> Unit,
    result: PairResult,
    deviceName: String,
    onTest: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) = OnboardFrame(
    2,
    stringResource(R.string.server_title),
    stringResource(R.string.server_lead),
    bottom = {
        if (result == PairResult.OK) {
            Primary(stringResource(R.string.server_finish), onFinish, Modifier.fillMaxWidth())
        } else {
            Primary(
                stringResource(if (result == PairResult.TESTING) R.string.testing else R.string.action_test_connection),
                onTest,
                Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && key.isNotBlank() && result != PairResult.TESTING,
            )
        }
        Gap(8)
        Secondary(stringResource(R.string.server_skip), onSkip, Modifier.fillMaxWidth())
    },
) {
    ServerFields(url, key, onUrl, onKey)
    Gap(16)
    PairNotice(result, Board.hostOf(url), deviceName)
}

enum class Access { NOTIFICATIONS, USAGE }

/**
 * Shown when the user taps Allow, BEFORE Android's own settings screen opens.
 * Google Play requires a prominent in-app disclosure for both special accesses,
 * stating exactly what is read.
 */
@Composable
fun DisclosureScreen(kind: Access, onContinue: () -> Unit, onNotNow: () -> Unit) {
    val c = LocalPalette.current
    val n = KnownApps.all.size
    val icon: Int
    val title: String
    val reads: String
    val never: String
    when (kind) {
        Access.NOTIFICATIONS -> {
            icon = R.drawable.ic_notifications
            title = stringResource(R.string.disclose_title_notifications)
            reads = stringResource(R.string.disclose_notif_reads, n)
            never = stringResource(R.string.disclose_notif_never)
        }
        Access.USAGE -> {
            icon = R.drawable.ic_query_stats
            title = stringResource(R.string.disclose_title_usage)
            reads = stringResource(R.string.disclose_usage_reads, n)
            never = stringResource(R.string.disclose_usage_never)
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    OnboardFrame(
        -1,
        title,
        stringResource(R.string.disclose_lead),
        bottom = {
            Primary(stringResource(R.string.disclose_continue), onContinue, Modifier.fillMaxWidth())
            Gap(8)
            Secondary(stringResource(R.string.disclose_not_now), onNotNow, Modifier.fillMaxWidth())
        },
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) { Ic(icon, tint = c.accent, size = 26) }
        Gap(16)
        Panel {
            Text(stringResource(R.string.disclose_reads).uppercase(locale), style = MaterialTheme.typography.labelMedium, color = c.ok, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(reads, style = MaterialTheme.typography.bodyMedium)
            Gap(14)
            Text(stringResource(R.string.disclose_never).uppercase(locale), style = MaterialTheme.typography.labelMedium, color = c.bad, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(never, style = MaterialTheme.typography.bodyMedium)
        }
        Gap(14)
        Text(Words.supportedNames(strings()), style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

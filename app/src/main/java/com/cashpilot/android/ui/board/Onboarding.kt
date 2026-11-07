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
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "Step ${at + 1} of 3" },
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
    "Know when an earning app stops",
    "CashPilot checks the passive-income apps on this phone and shows you which ones stopped, and how to start them again.",
    bottom = { Primary("Get started", onNext, Modifier.fillMaxWidth()) },
) {
    Fact(R.drawable.ic_check_circle, "Which apps are running", "${Words.supportedSample()}, checked whenever you open CashPilot.")
    Fact(R.drawable.ic_warning, "What to do when one stops", "One tap opens the app so you can start it again.")
    Fact(R.drawable.ic_cloud, "Figures from your server, if you have one", "A CashPilot server reports what each app's account shows. Optional; reports go every 15 minutes.")
    Fact(R.drawable.ic_visibility_off, "Nothing leaves this phone", "Except to your own server, if you add one. No ads, no tracking.")
}

@Composable
private fun PermissionCard(
    @DrawableRes icon: Int,
    title: String,
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
                granted -> Pill(if (required) "Allowed" else "Done", Tone.OK)
                required -> SmallAction("Allow", onAllow, filled = true, modifier = Modifier.semantics { contentDescription = "Allow ${title.lowercase()}" })
                // Optional, and not a permission: it only opens Android's
                // battery list. No exemption dialog, so it never says "Allow".
                else -> SmallAction("Open settings", onAllow, color = c.text, modifier = Modifier.semantics { contentDescription = "Open battery settings" })
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
    "Let CashPilot see your apps",
    "Android hides other apps' state. Two switches in system settings fix that; you come straight back here.",
    bottom = {
        val c = LocalPalette.current
        if (!(notif && usage)) {
            Text(
                "Without both, CashPilot can't tell running from stopped. You can allow them later from Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Primary(if (notif && usage) "Continue" else "Continue without them", onNext, Modifier.fillMaxWidth())
    },
) {
    val n = KnownApps.all.size
    PermissionCard(
        R.drawable.ic_notifications, "Notification access",
        "These apps keep a notification up while they run. CashPilot checks whether it is there, for the $n supported apps only.",
        "Never reads notification text. You'll see exactly what it reads before Android asks.",
        notif, required = true, onAllow = onAllowNotifications,
    )
    PermissionCard(
        R.drawable.ic_query_stats, "Usage access",
        "Shows when each supported app last ran and how much data it moved in the last 24 hours, which shows it is working.",
        "Never looks at other apps or websites. You'll see exactly what it reads first.",
        usage, required = true, onAllow = onAllowUsage,
    )
    PermissionCard(
        R.drawable.ic_battery_alert, "Battery optimisation",
        "CashPilot reports every 15 minutes. With battery optimisation on, Android can hold those reports back while the screen is off.",
        "Optional. Opens Android's battery list: choose All apps, then CashPilot, then Don't optimize.",
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
        label = { Text("Server address") },
        placeholder = { Text("https://cashpilot.example.com") },
        singleLine = true,
        isError = refused,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        supportingText = when {
            refused -> { { Text("This address is on the public internet, so it needs https://. CashPilot never sends the key over plain http:// there.") } }
            cleartext -> { { Text("Plain http:// on your own network: the key and app list travel unencrypted on it.") } }
            else -> null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Gap(12)
    OutlinedTextField(
        value = key,
        onValueChange = onKey,
        label = { Text("API key") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = { Text("On your server: Fleet → Reveal API Key. Stored only on this phone.") },
        modifier = Modifier.fillMaxWidth(),
    )
    Gap(4)
    Text(
        "Use https://. Plain http:// works only for an address on your own network, such as 192.168.x.x or a .local or Tailscale name.",
        style = MaterialTheme.typography.bodySmall,
        color = c.muted,
    )
}

@Composable
fun PairNotice(result: PairResult, host: String, deviceName: String) {
    val m = pairMessage(result, host, deviceName) ?: return
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
    "Connect your server",
    "Optional. Without one you still see which apps run and which stopped. A server adds the figures its provider accounts show, and lists this phone in your fleet.",
    bottom = {
        if (result == PairResult.OK) {
            Primary("Finish", onFinish, Modifier.fillMaxWidth())
        } else {
            Primary(
                if (result == PairResult.TESTING) "Testing…" else "Test connection",
                onTest,
                Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && key.isNotBlank() && result != PairResult.TESTING,
            )
        }
        Gap(8)
        Secondary("Use without a server", onSkip, Modifier.fillMaxWidth())
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
            title = "notification access"
            reads = "Only whether one of the $n apps below is showing an ongoing notification right now. That notification is how those apps show they are running."
            never = "The text of any notification, and anything from any other app. Only two things go out, and only to your own server if you add one: whether each of these apps has that notification up, and whether it is running."
        }
        Access.USAGE -> {
            icon = R.drawable.ic_query_stats
            title = "usage access"
            reads = "For the $n apps below only: when each one was last in use, and how much data it sent and received in the last 24 hours."
            never = "Which other apps you use, which websites you visit, or what any app does. The results stay on this phone, unless you add your own server."
        }
    }
    OnboardFrame(
        -1,
        "Before you turn on $title",
        "Android will open its settings next. This is what CashPilot does with it.",
        bottom = {
            Primary("Continue to Android settings", onContinue, Modifier.fillMaxWidth())
            Gap(8)
            Secondary("Not now", onNotNow, Modifier.fillMaxWidth())
        },
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft).clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) { Ic(icon, tint = c.accent, size = 26) }
        Gap(16)
        Panel {
            Text("CASHPILOT READS", style = MaterialTheme.typography.labelMedium, color = c.ok, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(reads, style = MaterialTheme.typography.bodyMedium)
            Gap(14)
            Text("IT NEVER READS", style = MaterialTheme.typography.labelMedium, color = c.bad, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(never, style = MaterialTheme.typography.bodyMedium)
        }
        Gap(14)
        Text(supportedNames(), style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

package com.cashpilot.android.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashpilot.android.R
import com.cashpilot.android.model.KnownApps

// ---------------------------------------------------------------------------
// Store-policy screens, shared by all three directions.
// ---------------------------------------------------------------------------

enum class Access { NOTIFICATIONS, USAGE }

/**
 * Shown when the user taps Allow, BEFORE Android's own settings screen opens.
 * Google Play requires a prominent in-app disclosure for both special accesses,
 * stating exactly what is read.
 */
@Composable
fun Disclosure(kind: Access) {
    val c = LocalCp.current
    val (icon, title, reads, never) = when (kind) {
        Access.NOTIFICATIONS -> listOf(
            R.drawable.ic_notifications, "Notification access",
            "Only whether one of the 11 apps below is showing an ongoing notification right now. That notification is how those apps show they are running.",
            "The text of any notification, and anything from any other app. Nothing about notifications is stored or sent; only the running or stopped result, and only to your own server if you add one.",
        )
        Access.USAGE -> listOf(
            R.drawable.ic_query_stats, "Usage access",
            "For the 11 apps below only: when each one was last in use, and how much data it sent and received in the last 24 hours.",
            "Which other apps you use, which websites you visit, or what any app does. The results stay on this phone, unless you add your own server.",
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 40.dp, bottom = 24.dp)) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Ic(icon as Int, tint = c.accent, size = 26)
        }
        Gap(20)
        Text("Before you turn on ${(title as String).lowercase()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Gap(8)
        Text("Android will open its settings next. This is what CashPilot does with it.", color = c.text2)
        Gap(20)
        Panel {
            Text("CASHPILOT READS", style = MaterialTheme.typography.labelMedium, color = c.ok, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(reads as String, style = MaterialTheme.typography.bodyMedium)
            Gap(14)
            Text("IT NEVER READS", style = MaterialTheme.typography.labelMedium, color = c.bad, fontWeight = FontWeight.SemiBold)
            Gap(4)
            Text(never as String, style = MaterialTheme.typography.bodyMedium)
        }
        Gap(14)
        Text(
            KnownApps.all.joinToString(", ") { it.displayName } + ".",
            style = MaterialTheme.typography.bodySmall, color = c.muted,
        )
        Spacer(Modifier.weight(1f))
        Primary("Continue to Android settings", Modifier.fillMaxWidth())
        Gap(8)
        Secondary("Not now", Modifier.fillMaxWidth())
    }
}

/**
 * Nothing supported is installed. Leads with what the app does for the user,
 * not with install buttons: the supported list is one link down, and says that
 * some of its links carry our referral code.
 */
@Composable
fun NoAppsHome(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Gap(40)
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.raised), contentAlignment = Alignment.Center) {
            Ic(R.drawable.ic_query_stats, tint = c.text2, size = 28)
        }
        Gap(20)
        Text("No supported app on this phone yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Gap(10)
        Text(
            "CashPilot watches passive-income apps you already use, such as EarnApp or Grass, and tells you when one stops running. Once one is installed it appears here by itself.",
            style = MaterialTheme.typography.bodyLarge, color = c.text2,
        )
        Gap(24)
        Panel {
            Text("Works without a server", fontWeight = FontWeight.SemiBold)
            Gap(6)
            Text(
                "On its own, CashPilot shows which apps run, when one stopped, and opens it for you. A CashPilot server adds earnings figures and lists this phone with your other machines.",
                style = MaterialTheme.typography.bodyMedium, color = c.text2,
            )
        }
        Gap(20)
        Secondary("Show a demo with sample data", Modifier.fillMaxWidth())
        Gap(14)
        Text("See the ${h.apps.size} supported apps  ›", color = c.accent, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.weight(1f))
        ServerLine(h)
    }
}

/** The supported apps a phone does not have, as a plain list with the referral disclosure at the top. */
@Composable
fun SupportedApps(h: Home) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ic(R.drawable.ic_arrow_back, tint = c.text) }
            Text("Supported apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "CashPilot can watch these apps if you install them. What any of them pays depends on the provider, your connection and your region.",
                color = c.text2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Panel(fill = c.raised, pad = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    "Links marked Referral include CashPilot's referral code, which supports CashPilot's development. The others go to the app store.",
                    style = MaterialTheme.typography.bodySmall, color = c.text2,
                )
            }
            Gap(12)
            Panel(pad = PaddingValues(vertical = 4.dp)) {
                h.notInstalled.forEachIndexed { i, a ->
                    if (i > 0) Box(Modifier.padding(start = 70.dp)) { Divider() }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppMark(a.name)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                if (referralOf(a.slug) != null) "Referral · provider's sign-up page" else "App store page",
                                style = MaterialTheme.typography.bodySmall, color = c.muted,
                            )
                        }
                        SmallAction("Open", color = c.text)
                    }
                }
            }
        }
    }
}

/** Demo mode: the real home screen with sample data, clearly labelled, for reviewers and the curious. */
@Composable
fun DemoFrame(content: @Composable () -> Unit) {
    val c = LocalCp.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp).background(c.accentSoft).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Demo · sample data, nothing here is real", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Exit", color = c.accent, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

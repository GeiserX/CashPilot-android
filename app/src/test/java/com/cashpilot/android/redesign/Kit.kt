package com.cashpilot.android.redesign

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.ui.AppState
import com.cashpilot.android.util.FormatUtils
import java.net.URI

// ---------------------------------------------------------------------------
// Palette: the web dashboard's "Classic night" theme (app/static/css/style.css,
// data-theme="classic") and its "Light" theme, so the phone and the server look
// like one product. Dynamic colour is off on purpose: Material You repaints the
// app in the wallpaper's colours and the brand disappears.
// ---------------------------------------------------------------------------

data class Cp(
    val bg: Color, val card: Color, val raised: Color, val line: Color, val lineStrong: Color,
    val text: Color, val text2: Color, val muted: Color,
    val accent: Color, val onAccent: Color, val accent2: Color,
    val ok: Color, val warn: Color, val bad: Color,
    val okSoft: Color, val warnSoft: Color, val badSoft: Color, val accentSoft: Color,
    val dark: Boolean,
)

val Night = Cp(
    bg = Color(0xFF0B0B14), card = Color(0xFF11111D), raised = Color(0xFF171726),
    line = Color(0xFF25233A), lineStrong = Color(0xFF4B4670),
    text = Color(0xFFE8E6F0), text2 = Color(0xFFBDB6CF), muted = Color(0xFFA59CB5),
    accent = Color(0xFFF472B6), onAccent = Color(0xFF0B0B14), accent2 = Color(0xFF22D3EE),
    ok = Color(0xFF46B37F), warn = Color(0xFFE0A53C), bad = Color(0xFFF06A5F),
    okSoft = Color(0x1F46B37F), warnSoft = Color(0x1FE0A53C), badSoft = Color(0x1FF06A5F),
    accentSoft = Color(0x1AF472B6), dark = true,
)

val Day = Cp(
    bg = Color(0xFFF6F7F8), card = Color(0xFFFFFFFF), raised = Color(0xFFEEF0F2),
    line = Color(0xFFDDE1E5), lineStrong = Color(0xFF9AA2AB),
    text = Color(0xFF16191C), text2 = Color(0xFF464D55), muted = Color(0xFF5F6770),
    // The web's light theme uses near-black actions; the phone keeps the brand
    // pink, darkened to 6:1 on white so it passes AA for text.
    accent = Color(0xFFBE185D), onAccent = Color.White, accent2 = Color(0xFF0E7490),
    ok = Color(0xFF1D7A4C), warn = Color(0xFF8F5A00), bad = Color(0xFFB42318),
    okSoft = Color(0x1A1D7A4C), warnSoft = Color(0x1F8F5A00), badSoft = Color(0x17B42318),
    accentSoft = Color(0x14BE185D), dark = false,
)

val LocalCp = staticCompositionLocalOf { Night }

@Composable
fun CpTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) Night else Day
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent2,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.lineStrong,
            outlineVariant = c.line, error = c.bad, surfaceContainer = c.card,
            surfaceContainerHigh = c.raised, surfaceContainerLow = c.card,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent2,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.lineStrong,
            outlineVariant = c.line, error = c.bad, surfaceContainer = c.card,
            surfaceContainerHigh = c.raised, surfaceContainerLow = c.card,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
        )
    }
    CompositionLocalProvider(LocalCp provides c) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = c.bg, contentColor = c.text) { content() }
        }
    }
}

/** Figures line up in columns: tabular digits. */
val Tnum = TextStyle(fontFeatureSettings = "tnum")

// ---------------------------------------------------------------------------
// What every direction draws from. Derived from the same Fixtures the "before"
// screenshots use, so the facts on screen are identical across all images.
// ---------------------------------------------------------------------------

enum class ServerState { NONE, OK, FAILING }

data class AppRow(
    val slug: String,
    val name: String,
    val state: AppState,
    val lastActive: Long?,
    val bytes24h: Long,
    val usd: Double?,
    val shared: Boolean,
)

data class Home(
    val apps: List<AppRow>,
    val server: ServerState,
    val host: String,
    val lastReport: Long,
    val total: Double?,
    val windowDays: Int,
    val earningsAsOf: Long,
    val stale: Boolean,
    val notif: Boolean,
    val usage: Boolean,
    val battery: Boolean,
) {
    val watched get() = apps.filter { it.state != AppState.NOT_INSTALLED && it.state != AppState.DISABLED }
    val running get() = watched.filter { it.state == AppState.RUNNING }
    val stopped get() = watched.filter { it.state == AppState.STOPPED }
    val notInstalled get() = apps.filter { it.state == AppState.NOT_INSTALLED }
    val blind get() = !notif && !usage
    val withoutReading get() = watched.filter { it.usd == null }
}

fun Fixtures.Scenario.toHome(): Home = Home(
    apps = apps.map { info ->
        val p = earnings?.platforms?.firstOrNull { it.slug == info.app.slug }
        AppRow(
            slug = info.app.slug,
            name = info.app.displayName,
            state = info.state,
            lastActive = info.status?.lastActive?.let(FormatUtils::parseIso),
            bytes24h = (info.status?.netTx24h ?: 0) + (info.status?.netRx24h ?: 0),
            usd = p?.usd,
            shared = p?.sharedWithOtherWorkers == true,
        )
    },
    server = when {
        !serverConfigured -> ServerState.NONE
        heartbeatFailed -> ServerState.FAILING
        else -> ServerState.OK
    },
    host = if (serverConfigured) URI(serverUrl).host else "",
    lastReport = lastHeartbeat,
    total = earnings?.totalUsd,
    windowDays = earnings?.windowDays ?: 30,
    earningsAsOf = earningsAsOf,
    stale = earnings?.totalUsd != null && FormatUtils.earningsAreStale(earningsAsOf, System.currentTimeMillis()),
    notif = notif, usage = usage, battery = battery,
)

fun money(usd: Double?): String = if (usd == null) "—" else "$" + String.format("%.2f", usd)

fun ago(millis: Long?): String {
    if (millis == null || millis == 0L) return "never"
    val d = (System.currentTimeMillis() - millis) / 1000
    return when {
        d < 60 -> "just now"
        d < 3600 -> "${d / 60} min ago"
        d < 86_400 -> "${d / 3600} h ago"
        d < 2 * 86_400 -> "yesterday"
        else -> "${d / 86_400} days ago"
    }
}

/** "for 25 min", "for 3 h": how long something has been wrong. */
fun forHowLong(millis: Long?): String = ago(millis).removeSuffix(" ago").let {
    when (it) { "yesterday" -> "since yesterday"; "just now" -> "for a moment"; else -> "for $it" }
}

fun referralOf(slug: String) = KnownApps.bySlug[slug]?.referralUrl

// ---------------------------------------------------------------------------
// Small parts shared by every direction.
// ---------------------------------------------------------------------------

/**
 * A two-letter tile per app. The shipped app would draw the real launcher icon
 * from PackageManager for installed apps; the JVM renderer has no such icons, so
 * the proposals show where it goes.
 */
@Composable
fun AppMark(name: String, size: Int = 40, dim: Boolean = false) {
    val c = LocalCp.current
    val hues = listOf(0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFF46B37F, 0xFFE0A53C, 0xFF3987E5, 0xFFD95926)
    val base = Color(hues[(name.hashCode() and 0x7fffffff) % hues.size])
    val letters = name.split(" ").let { if (it.size > 1) "${it[0][0]}${it[1][0]}" else name.take(2) }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp))
            .background(if (dim) c.raised else base.copy(alpha = if (c.dark) 0.22f else 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letters, fontWeight = FontWeight.Bold, fontSize = (size * 0.36).sp,
            color = if (dim) c.muted else if (c.dark) base else base.copy(red = base.red * 0.7f, green = base.green * 0.7f, blue = base.blue * 0.7f),
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun stateColor(state: AppState): Color {
    val c = LocalCp.current
    return when (state) {
        AppState.RUNNING -> c.ok
        AppState.STOPPED -> c.bad
        AppState.UNKNOWN -> c.warn
        else -> c.muted
    }
}

fun stateWord(row: AppRow): String = when (row.state) {
    AppState.RUNNING -> "Running"
    AppState.STOPPED -> "Stopped ${ago(row.lastActive)}"
    AppState.UNKNOWN -> "Can't see it"
    AppState.DISABLED -> "Not watched"
    AppState.NOT_INSTALLED -> "Not installed"
}

@Composable
fun Ic(@DrawableRes id: Int, tint: Color = LocalCp.current.text2, size: Int = 20) =
    Icon(painterResource(id), contentDescription = null, tint = tint, modifier = Modifier.size(size.dp))

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(), modifier = modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp),
    style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp,
    color = LocalCp.current.muted, fontWeight = FontWeight.SemiBold,
)

/** A flat card with a hairline, the web dashboard's card. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: Color = LocalCp.current.line,
    fill: Color = LocalCp.current.card,
    pad: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(fill)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp)).padding(pad),
        content = content,
    )
}

@Composable
fun Primary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true) = Button(
    onClick = {}, enabled = enabled, modifier = modifier.height(48.dp),
    shape = RoundedCornerShape(12.dp),
) { Text(text, fontWeight = FontWeight.SemiBold) }

@Composable
fun Secondary(text: String, modifier: Modifier = Modifier) = OutlinedButton(
    onClick = {}, modifier = modifier.height(48.dp), shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, LocalCp.current.lineStrong),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalCp.current.text),
) { Text(text, fontWeight = FontWeight.SemiBold) }

@Composable
fun SmallAction(text: String, color: Color = LocalCp.current.accent, filled: Boolean = false) {
    val c = LocalCp.current
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (filled) color else Color.Transparent)
            .border(1.dp, if (filled) color else c.lineStrong, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (filled) c.onAccent else color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Pill(text: String, color: Color, soft: Color) = Text(
    text,
    Modifier.clip(RoundedCornerShape(50)).background(soft).padding(horizontal = 10.dp, vertical = 4.dp),
    color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
)

/** One line about the server, the same words in every direction. */
@Composable
fun ServerLine(h: Home, modifier: Modifier = Modifier) {
    val c = LocalCp.current
    val (color, text) = when (h.server) {
        ServerState.NONE -> c.muted to "No server · everything stays on this phone"
        ServerState.OK -> if (h.lastReport == 0L) {
            c.muted to "Paired with ${h.host} · waiting for the first report"
        } else {
            c.ok to "Last report ${ago(h.lastReport)} · sent every 15 min"
        }
        ServerState.FAILING -> c.warn to "Can't reach ${h.host} ${forHowLong(h.lastReport)}"
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TopBar(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun Divider() = HorizontalDivider(color = LocalCp.current.line)

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))

@Composable
fun Screen(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.Top, content = content)

package com.cashpilot.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * The two palettes of the web dashboard: "Classic night" (data-theme="classic"
 * in the server's style.css) and "Light", so the phone and the server read as
 * one product.
 *
 * Dynamic colour is off on purpose. Material You repaints the app in the
 * wallpaper's colours, and then the status colours (green running, red
 * stopped) are whatever the wallpaper made of them.
 *
 * Every text colour here clears WCAG AA (4.5:1) on [bg], [card] and [raised],
 * and each status colour does on every tinted fill too. PaletteContrastTest
 * checks each pair, so a tweak that drops one below fails the build instead of
 * shipping unreadable text. (Light's green and amber were darkened one step
 * for that reason: on the tinted fills the first values fell to 4.2:1.)
 */
data class Palette(
    val bg: Color,
    val card: Color,
    val raised: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val ok: Color,
    val warn: Color,
    val bad: Color,
    val okSoft: Color,
    val warnSoft: Color,
    val badSoft: Color,
    val accentSoft: Color,
    val dark: Boolean,
)

val ClassicNight = Palette(
    bg = Color(0xFF0B0B14), card = Color(0xFF11111D), raised = Color(0xFF171726),
    line = Color(0xFF25233A), lineStrong = Color(0xFF4B4670),
    text = Color(0xFFE8E6F0), text2 = Color(0xFFBDB6CF), muted = Color(0xFFA59CB5),
    accent = Color(0xFFF472B6), onAccent = Color(0xFF0B0B14),
    ok = Color(0xFF46B37F), warn = Color(0xFFE0A53C), bad = Color(0xFFF06A5F),
    okSoft = Color(0x1F46B37F), warnSoft = Color(0x1FE0A53C), badSoft = Color(0x1FF06A5F),
    accentSoft = Color(0x1AF472B6), dark = true,
)

val Light = Palette(
    bg = Color(0xFFF6F7F8), card = Color(0xFFFFFFFF), raised = Color(0xFFEEF0F2),
    line = Color(0xFFDDE1E5), lineStrong = Color(0xFF9AA2AB),
    text = Color(0xFF16191C), text2 = Color(0xFF464D55), muted = Color(0xFF5F6770),
    // The web's light theme uses near-black actions; the phone keeps the brand
    // pink, darkened until it passes AA as text on white.
    accent = Color(0xFFBE185D), onAccent = Color.White,
    ok = Color(0xFF1A7046), warn = Color(0xFF875500), bad = Color(0xFFB42318),
    okSoft = Color(0x1A1A7046), warnSoft = Color(0x1F875500), badSoft = Color(0x17B42318),
    accentSoft = Color(0x14BE185D), dark = false,
)

val LocalPalette = staticCompositionLocalOf { ClassicNight }

/** Figures line up in columns: tabular digits. */
val Tnum = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun CashPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) ClassicNight else Light
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.lineStrong,
            outlineVariant = c.line, error = c.bad, surfaceContainer = c.card,
            surfaceContainerHigh = c.raised, surfaceContainerLow = c.card,
            surfaceContainerHighest = c.raised,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.raised, onSurfaceVariant = c.text2, outline = c.lineStrong,
            outlineVariant = c.line, error = c.bad, surfaceContainer = c.card,
            surfaceContainerHigh = c.raised, surfaceContainerLow = c.card,
            surfaceContainerHighest = c.raised,
            primaryContainer = c.accentSoft, onPrimaryContainer = c.text,
        )
    }
    CompositionLocalProvider(LocalPalette provides c) {
        MaterialTheme(colorScheme = scheme) {
            Surface(Modifier.fillMaxSize(), color = c.bg, contentColor = c.text) { content() }
        }
    }
}

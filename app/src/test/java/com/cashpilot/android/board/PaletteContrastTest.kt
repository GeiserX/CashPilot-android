package com.cashpilot.android.board

import androidx.compose.ui.graphics.Color
import com.cashpilot.android.ui.theme.ClassicNight
import com.cashpilot.android.ui.theme.Light
import com.cashpilot.android.ui.theme.Palette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** WCAG AA for every text colour on every surface it is drawn on, in both palettes. */
class PaletteContrastTest {

    private fun channel(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** A translucent fill as it actually appears over [base]. */
    private fun over(fill: Color, base: Color) = Color(
        red = fill.red * fill.alpha + base.red * (1 - fill.alpha),
        green = fill.green * fill.alpha + base.green * (1 - fill.alpha),
        blue = fill.blue * fill.alpha + base.blue * (1 - fill.alpha),
    )

    private fun failures(p: Palette): List<String> {
        val texts = mapOf("text" to p.text, "text2" to p.text2, "muted" to p.muted, "accent" to p.accent, "ok" to p.ok, "warn" to p.warn, "bad" to p.bad)
        val surfaces = mutableMapOf("bg" to p.bg, "card" to p.card, "raised" to p.raised)
        listOf("okSoft" to p.okSoft, "warnSoft" to p.warnSoft, "badSoft" to p.badSoft, "accentSoft" to p.accentSoft).forEach { (n, s) ->
            surfaces["$n/bg"] = over(s, p.bg)
            surfaces["$n/card"] = over(s, p.card)
        }
        val out = mutableListOf<String>()
        for ((tn, t) in texts) for ((sn, s) in surfaces) {
            // Muted text is never drawn on a tinted fill.
            if (tn == "muted" && "/" in sn) continue
            val r = ratio(t, s)
            if (r < 4.5) out += "$tn on $sn = ${"%.2f".format(r)}"
        }
        val onAccent = ratio(p.onAccent, p.accent)
        if (onAccent < 4.5) out += "onAccent on accent = ${"%.2f".format(onAccent)}"
        return out
    }

    @Test
    fun `classic night text clears AA everywhere`() {
        val f = failures(ClassicNight)
        assertTrue(f.isEmpty(), "below 4.5:1 in Classic night: $f")
    }

    @Test
    fun `light text clears AA everywhere`() {
        val f = failures(Light)
        assertTrue(f.isEmpty(), "below 4.5:1 in Light: $f")
    }

    @Test
    fun `the contrast check can fail`() {
        // The first Light green (1D7A4C) measured 4.2:1 on the tinted fills.
        assertTrue(failures(Light.copy(ok = Color(0xFF1D7A4C))).isNotEmpty())
    }
}

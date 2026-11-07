package com.cashpilot.android.ui.board

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashpilot.android.R
import com.cashpilot.android.ui.theme.LocalPalette

@Composable
fun toneColor(tone: Tone): Color {
    val c = LocalPalette.current
    return when (tone) {
        Tone.OK -> c.ok
        Tone.WARN -> c.warn
        Tone.BAD -> c.bad
        Tone.MUTED -> c.muted
    }
}

@Composable
fun toneSoft(tone: Tone): Color {
    val c = LocalPalette.current
    return when (tone) {
        Tone.OK -> c.okSoft
        Tone.WARN -> c.warnSoft
        Tone.BAD -> c.badSoft
        Tone.MUTED -> c.raised
    }
}

/**
 * A two-letter tile per app. Decorative: the app's name is always written next
 * to it, so TalkBack skips it.
 */
@Composable
fun AppMark(name: String, size: Int = 40, dim: Boolean = false) {
    val c = LocalPalette.current
    val hues = listOf(0xFF8B5CF6, 0xFF22D3EE, 0xFFF472B6, 0xFF46B37F, 0xFFE0A53C, 0xFF3987E5, 0xFFD95926)
    val base = Color(hues[(name.hashCode() and 0x7fffffff) % hues.size])
    val words = name.split(" ").filter { it.isNotEmpty() }
    val letters = if (words.size > 1) "${words[0][0]}${words[1][0]}" else name.take(2)
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp))
            .background(if (dim) c.raised else base.copy(alpha = if (c.dark) 0.22f else 0.16f))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letters,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.36).sp,
            color = when {
                dim -> c.muted
                c.dark -> base
                else -> base.copy(red = base.red * 0.6f, green = base.green * 0.6f, blue = base.blue * 0.6f)
            },
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) = Box(Modifier.size(size.dp).clip(CircleShape).background(color))

@Composable
fun Ic(@DrawableRes id: Int, tint: Color = LocalPalette.current.text2, size: Int = 20, description: String? = null) =
    Icon(painterResource(id), contentDescription = description, tint = tint, modifier = Modifier.size(size.dp))

@Composable
fun Heading(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium) =
    Text(text, modifier.semantics { heading() }, style = style, fontWeight = FontWeight.Bold)

@Composable
fun SectionLabel(text: String) = Text(
    text.uppercase(),
    Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp).semantics { heading() },
    style = MaterialTheme.typography.labelMedium,
    letterSpacing = 1.2.sp,
    color = LocalPalette.current.muted,
    fontWeight = FontWeight.SemiBold,
)

/** A flat card with a hairline, the web dashboard's card. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: Color = LocalPalette.current.line,
    fill: Color = LocalPalette.current.card,
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
fun Primary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.heightIn(min = 48.dp),
    shape = RoundedCornerShape(12.dp),
) { Text(text, fontWeight = FontWeight.SemiBold) }

@Composable
fun Secondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) = OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = 48.dp),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, LocalPalette.current.lineStrong),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalPalette.current.text),
) { Text(text, fontWeight = FontWeight.SemiBold) }

/** A compact action inside a row or card. Still a 48 dp touch target. */
@Composable
fun SmallAction(
    text: String,
    onClick: () -> Unit,
    color: Color = LocalPalette.current.accent,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = LocalPalette.current
    val pad = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = pad,
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = c.onAccent),
        ) { Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = pad,
            border = BorderStroke(1.dp, c.lineStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun Pill(text: String, tone: Tone) = Text(
    text,
    Modifier.clip(RoundedCornerShape(50)).background(toneSoft(tone)).padding(horizontal = 10.dp, vertical = 4.dp),
    color = toneColor(tone),
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.SemiBold,
)

/** One line about the server, the same words on every screen. */
@Composable
fun ServerLine(b: Board, modifier: Modifier = Modifier) {
    val line = b.serverLine()
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Dot(toneColor(line.tone))
        Spacer(Modifier.width(8.dp))
        Text(line.text, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.text2)
    }
}

/** The title row of a top-level screen, with room for icon actions. */
@Composable
fun TopBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Heading(title, Modifier.weight(1f), MaterialTheme.typography.titleLarge)
        actions()
    }
}

/** The title row of a screen one level down: back arrow, then the title. */
@Composable
fun BackBar(title: String?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, end = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Ic(R.drawable.ic_arrow_back, tint = LocalPalette.current.text, size = 24, description = "Back")
        }
        if (title != null) Heading(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = LocalPalette.current.line)

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))

/** A note in its tone's colours: what a task, a test result or a warning looks like. */
@Composable
fun Notice(tone: Tone, title: String, body: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Panel(modifier.semantics(mergeDescendants = true) {}, border = toneColor(tone), fill = toneSoft(tone)) {
        Row(verticalAlignment = Alignment.Top) {
            Ic(icon, tint = toneColor(tone))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = LocalPalette.current.text2)
            }
        }
    }
}

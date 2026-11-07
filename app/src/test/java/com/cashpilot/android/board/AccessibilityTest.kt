package com.cashpilot.android.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.cashpilot.android.ui.board.BoardActions
import com.cashpilot.android.ui.board.HomeScreen
import com.cashpilot.android.ui.theme.CashPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The accessibility pass, run on every screen in [Screens.all]:
 *
 *  - every tappable thing is at least 48 x 48 dp,
 *  - every tappable thing has a name TalkBack can read,
 *  - every screen has a heading to jump to,
 *  - no text is out of reach (off the right edge, or below the screen with
 *    nothing to scroll it), at the default size and at font scale 1.3,
 *  - TalkBack reads the home screen in the order it is meant to be read.
 *
 * Contrast is PaletteContrastTest. Each rule here is shown to fail once on a
 * deliberately broken screen, because a check nobody has seen fail proves nothing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private fun SemanticsNode.walk(): List<SemanticsNode> = listOf(this) + children.flatMap { it.walk() }

    private fun nodes(merged: Boolean = true) = rule.onRoot(useUnmergedTree = !merged).fetchSemanticsNode().walk()

    private fun label(n: SemanticsNode): String =
        (n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            listOfNotNull(n.config.getOrNull(SemanticsProperties.EditableText)?.text)).joinToString(" ").trim()

    private fun SemanticsNode.hasScrollingAncestor(): Boolean {
        var p = parent
        while (p != null) {
            if (p.config.contains(SemanticsActions.ScrollBy)) return true
            p = p.parent
        }
        return false
    }

    /** Problems on whatever is on screen now, one line per offender. */
    private fun problems(): List<String> {
        val out = mutableListOf<String>()
        // The density the layout was drawn at. Both rule.density and the
        // application's display metrics read 1.0 under Robolectric while Compose
        // lays out at 2.0 (xhdpi), which let a 24 dp target pass as 48 dp.
        val density = rule.onRoot().fetchSemanticsNode().layoutInfo.density.density
        for (n in nodes().filter { it.config.contains(SemanticsActions.OnClick) }) {
            // The node's own laid-out size, the size Accessibility Scanner checks.
            // Not touchBoundsInRoot: Compose widens that to 48 dp for every
            // clickable, so a check on it can never fail. And not the bounds on
            // screen, which a scroll container clips to 0 x 0 below the fold.
            val w = n.size.width / density
            val h = n.size.height / density
            val name = label(n)
            if (w < 47.5f || h < 47.5f) out += "touch target ${w.toInt()}x${h.toInt()} dp: '$name'"
            if (name.isBlank()) out += "tappable with no name at ${n.boundsInRoot}"
        }
        if (nodes(merged = false).none { it.config.contains(SemanticsProperties.Heading) }) out += "no heading"
        // Text the user can never read: past the right edge (it did not wrap),
        // or below the bottom edge with nothing to scroll it into view. Measured
        // from layout positions, which a scroll container does not clip.
        val root = rule.onRoot().fetchSemanticsNode()
        for (n in nodes(merged = false)) {
            val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text } ?: continue
            val right = n.positionInRoot.x + n.size.width
            val bottom = n.positionInRoot.y + n.size.height
            if (right > root.size.width + 1) out += "text runs off the right edge: '$text'"
            if (bottom > root.size.height + 1 && !n.hasScrollingAncestor()) out += "text below the screen, no scrolling: '$text'"
            // A Column that cannot scroll gives what does not fit a height of 0.
            if (text.isNotEmpty() && n.size.height == 0) out += "text squashed to nothing: '$text'"
        }
        return out
    }

    @Test
    fun everyScreenPassesTheAccessibilityPass() {
        var current by mutableStateOf(Screens.all.first())
        rule.setContent { key(current.name) { Themed(current) } }
        val failures = mutableListOf<String>()
        for (shot in Screens.all) {
            current = shot
            rule.waitForIdle()
            problems().forEach { failures += "${shot.name}: $it" }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun talkBackReadsTheHomeScreenInOrder() {
        // Title, settings, the verdict, the server line, the task list, then
        // the apps: the answer before the details.
        rule.setContent { CashPilotTheme(darkTheme = true) { HomeScreen(Situations.problems, BoardActions()) } }
        val spoken = nodes().map(::label).filter { it.isNotBlank() }
        fun at(prefix: String) = spoken.indexOfFirst { it.startsWith(prefix) }.also { assertTrue("'$prefix' is not read at all: $spoken", it >= 0) }
        val order = listOf("CashPilot", "Settings", "2 of 6 apps stopped", "Can't reach", "4 things need you", "1. Traffmonetizer stopped", "Grass Stopped", "Traffmonetizer Stopped", "Your server reports")
        val indexes = order.map(::at)
        assertEquals("read in the wrong order: $spoken", indexes.sorted(), indexes)
    }

    // ---- Each rule can fail ---------------------------------------------------

    @Composable
    private fun Broken() {
        // 24 dp, no name, no heading. Inside a full-size Box, because the
        // theme's Surface passes its minimum size down to a direct child.
        Box(Modifier.fillMaxSize()) { Box(Modifier.size(24.dp).clickable { }) }
    }

    @Composable
    private fun TooLong() {
        // Taller than the screen, in a Column that cannot scroll.
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
            androidx.compose.material3.Text("Title", Modifier.semantics { heading() })
            repeat(80) { androidx.compose.material3.Text("Line $it") }
        }
    }

    @Test
    fun unreachableTextIsCaught() {
        rule.setContent { CashPilotTheme(darkTheme = true) { TooLong() } }
        val p = problems()
        assertTrue("squashed text not caught: ${p.take(5)}", p.any { it.startsWith("text squashed to nothing") })
    }

    @Test
    fun theAccessibilityPassCanFail() {
        rule.setContent { CashPilotTheme(darkTheme = true) { Broken() } }
        val p = problems()
        assertTrue("small target not caught: $p", p.any { it.startsWith("touch target 24x24") })
        assertTrue("missing name not caught: $p", p.any { it.startsWith("tappable with no name") })
        assertTrue("missing heading not caught: $p", p.contains("no heading"))
        assertEquals(2f, rule.onRoot().fetchSemanticsNode().layoutInfo.density.density)
    }
}

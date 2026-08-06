package com.cashpilot.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * A golden image per icon, so migrating off the frozen icon library is provable.
 *
 * WHY THIS EXISTS (CashPilot-android-vxb)
 * --------------------------------------
 * `androidx.compose.material:material-icons-extended` is frozen at 1.7.8 —
 * Google stopped publishing it, and material3 no longer depends on
 * material-icons-core transitively. It still resolves today because the Compose
 * BOM continues to pin it, but a frozen dependency is a slow leak: one day a BOM
 * simply stops pinning it and the build breaks with a missing version rather
 * than a deprecation warning.
 *
 * The recommended replacement is per-icon vector drawables. That is sixteen
 * substitutions whose ONLY acceptable outcome is "nothing changed visually", and
 * eyeballing sixteen icons is not evidence. These goldens are.
 *
 * WHY ICONS ALONE, AND NOT WHOLE SCREENS
 * --------------------------------------
 * Deliberate, and it is the difference between a gate that works and one that
 * gets disabled within a month. Whole-screen goldens are dominated by TEXT, and
 * text rendering depends on the fonts installed on the machine that rendered it
 * — so a golden recorded in the Docker build container fails on the CI runner
 * for reasons that have nothing to do with the change under test. A flaky gate
 * gets muted, and then it protects nothing.
 *
 * Icons are pure vector geometry with no text at all, so what is captured here
 * is the thing the migration actually risks and almost nothing else.
 *
 * Screen-level goldens are worth having, but they belong with the UI-renewal
 * work, where the fonts can be pinned deliberately rather than inherited.
 *
 * HOW TO USE IT
 * -------------
 *   ./scripts/remote-gradle.sh recordRoborazziDebug   # write/refresh the goldens
 *   ./scripts/remote-gradle.sh verifyRoborazziDebug   # fail on any visual change
 *
 * A diff during the icon migration means that icon is NOT the same glyph. A diff
 * at any other time means something rendered differently and you should find out
 * why before re-recording.
 */
@RunWith(RobolectricTestRunner::class)
// NATIVE graphics is what makes this work off-device: Robolectric renders through
// its bundled Skia rather than returning blank bitmaps from a stubbed Canvas. In
// LEGACY mode every golden here would be an identical empty image and every
// assertion would pass — the most dangerous possible failure for a visual gate.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Pinned rather than left to default. The SDK level selects the platform's
// rendering stack, so letting it float would re-render every golden on an
// unrelated dependency bump.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class IconGoldenTest {

    /** Drawables that are not part of the migrated icon set and need no golden. */
    private val NOT_AN_ICON = setOf("ic_github", "ic_notification", "ic_launcher_background")


    /**
     * Every icon this app draws, by the name it is referenced with.
     *
     * Enumerated from the source rather than typed from memory:
     *   grep -rn 'Icons\.' app/src/main/java --include='*.kt'
     *
     * [iconsInSourceAreAllCovered] fails if the app starts using one that is not
     * in this list, because an icon nobody photographed is an icon the migration
     * can silently break.
     */
    private val icons: List<Pair<String, Int>> = listOf(
        "arrow_back" to R.drawable.ic_arrow_back,
        "arrow_downward" to R.drawable.ic_arrow_downward,
        "arrow_upward" to R.drawable.ic_arrow_upward,
        "battery_alert" to R.drawable.ic_battery_alert,
        "check_circle" to R.drawable.ic_check_circle,
        "chevron_right" to R.drawable.ic_chevron_right,
        "circle" to R.drawable.ic_circle,
        "close" to R.drawable.ic_close,
        "cloud" to R.drawable.ic_cloud,
        "cloud_off" to R.drawable.ic_cloud_off,
        "language" to R.drawable.ic_language,
        "notifications" to R.drawable.ic_notifications,
        "query_stats" to R.drawable.ic_query_stats,
        "settings" to R.drawable.ic_settings,
        "visibility_off" to R.drawable.ic_visibility_off,
        "warning" to R.drawable.ic_warning,
    )

    @Composable
    private fun Subject(@DrawableRes resId: Int) {
        // A fixed box, an opaque background and an explicit tint: three things
        // that would otherwise be inherited from a theme and would re-render
        // every golden the first time the theme changed.
        Box(Modifier.size(96.dp).background(Color.White)) {
            Icon(
                painter = painterResource(resId),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(48.dp),
            )
        }
    }

    @Test
    fun everyIconHasAGolden() {
        // One file per icon, so a diff names the icon that changed instead of
        // saying "the sheet is different".
        icons.forEach { (name, resId) ->
            captureRoboImage("src/test/screenshots/icon_$name.png") { Subject(resId) }
        }
    }

    @Test
    fun everyIconDrawnByTheAppHasAGolden() {
        // The list above is hand-maintained, and a hand-maintained list drifts --
        // that is the whole reason this bead exists. So it is checked against the
        // source on every run.
        //
        // It now scans for R.drawable.ic_* rather than Icons.*, because the app
        // no longer draws a single ImageVector: the frozen library is gone.
        val referenced = Regex("""R\.drawable\.(ic_[a-z0-9_]+)""")
            .findAll(sourceText())
            .map { it.groupValues[1] }
            .toSet() - NOT_AN_ICON
        val covered = icons.map { (name, _) -> "ic_$name" }.toSet()

        val uncovered = referenced - covered
        assert(uncovered.isEmpty()) { "drawables the app draws with no golden image: $uncovered" }
    }

    @Test
    fun theCoverListHasNoIconTheAppStoppedUsing() {
        // The mirror of the test above. Without it the list only ever grows, and
        // a golden for an icon nobody draws is a file that fails a migration for
        // no reason anyone can act on.
        val referenced = Regex("""R\.drawable\.(ic_[a-z0-9_]+)""")
            .findAll(sourceText())
            .map { it.groupValues[1] }
            .toSet()
        val covered = icons.map { (name, _) -> "ic_$name" }.toSet()

        val stale = covered - referenced
        assert(stale.isEmpty()) { "goldens for icons the app no longer draws: $stale" }
    }

    @Test
    fun theFrozenIconLibraryIsGone() {
        // The point of the whole migration.
        //
        // Belt and braces, and worth saying which is which: with the dependency
        // removed, a stray `Icons.` reference does not compile at all
        // ("Unresolved reference 'Icons'" -- verified by putting one back). This
        // test catches the case that DOES compile: someone re-adds the
        // dependency and the usage together, which would restore the frozen
        // library without failing anything else.
        val offenders = Regex("""Icons\.(?:AutoMirrored\.)?(?:Filled|Outlined|Default)\.""")
            .findAll(sourceText())
            .map { it.value }
            .toSet()
        assert(offenders.isEmpty()) { "the app still draws Material Icons: $offenders" }
    }

    @Test
    fun theGoldenDirectoryHoldsExactlyTheExpectedFiles() {
        // The third direction of drift, and the only one the two tests above
        // cannot see: they compare the LIST against the SOURCE and never look at
        // the directory. An icon removed from both leaves its PNG committed
        // forever, and a golden that goes missing is not reported as missing --
        // it is simply not compared. (CodeRabbit, PR #51.)
        val dir = File("src/test/screenshots")
        check(dir.isDirectory) { "expected the goldens at ${dir.absolutePath}" }

        val onDisk = dir.listFiles { f -> f.isFile && f.extension == "png" }
            .orEmpty()
            .map { it.name }
            .toSet()
        val expected = icons.map { (name, _) -> "icon_$name.png" }.toSet()

        val orphaned = onDisk - expected
        val missing = expected - onDisk
        assert(orphaned.isEmpty() && missing.isEmpty()) {
            "golden files with no icon: $orphaned; icons with no golden file: $missing"
        }
    }

    /**
     * Every Kotlin source file under `app/src/main`, concatenated.
     *
     * Read from disk rather than from a compiled artifact because the point is to
     * catch a NEW `Icons.` reference, which by definition is not in this test's
     * own classpath knowledge.
     */
    private fun sourceText(): String {
        val root = File("src/main/java")
        check(root.isDirectory) { "expected app sources at ${root.absolutePath}" }
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        // A silent empty read would make both drift tests vacuously pass.
        check(files.size >= 10) { "only found ${files.size} Kotlin sources; the path is wrong" }
        return files.joinToString("\n") { it.readText() }
    }
}

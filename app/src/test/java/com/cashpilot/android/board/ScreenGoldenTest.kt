package com.cashpilot.android.board

import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A golden image of every screen, so a visual change is a reviewed change.
 *
 * Text is most of what these images hold, which is why the project kept to
 * icon goldens until now. What makes screens stable enough to gate on:
 *
 *  - Robolectric's NATIVE graphics draw text with the fonts bundled in its
 *    android-all jar, not the build machine's, so the build container and the
 *    CI runner render the same glyphs;
 *  - the SDK level is pinned, so a dependency bump does not swap the renderer;
 *  - every age on screen comes from [Situations.NOW], never the clock, and the
 *    version string is fixed, so nothing changes from one day or release to the next.
 *
 *   ./scripts/remote-gradle.sh recordRoborazziDebug   # after an intended change
 *   ./scripts/remote-gradle.sh verifyRoborazziDebug   # what CI runs
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class ScreenGoldenTest {

    @Test
    fun everyScreenHasAGolden() {
        Screens.all.forEach { shot -> captureRoboImage(shot.file) { Themed(shot) } }
    }

    @Test
    fun theScreenGoldenDirectoryHoldsExactlyTheExpectedFiles() {
        // A screen removed from the list would otherwise leave its PNG
        // committed forever, and a golden that goes missing is not compared.
        val dir = File("src/test/screenshots/screens")
        val onDisk = dir.listFiles { f -> f.isFile && f.extension == "png" }.orEmpty().map { it.name }.toSet()
        val expected = Screens.all.map { File(it.file).name }.toSet()
        assertTrue("golden files with no screen: ${onDisk - expected}", (onDisk - expected).isEmpty())
        assertTrue("screens with no golden file: ${expected - onDisk}", (expected - onDisk).isEmpty())
    }

    @Test
    fun screenNamesAreUnique() {
        val names = Screens.all.map { it.name }
        assertTrue("duplicate screen names: ${names.groupBy { it }.filterValues { it.size > 1 }.keys}", names.size == names.toSet().size)
    }
}

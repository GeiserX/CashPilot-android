package com.cashpilot.android

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * CashPilot-android-ni6: the app demanded a server before it would do anything.
 *
 * `SetupScreen` gated its finish button on `serverDone && hasNotif && hasUsage`,
 * so a user who installed the app without a CashPilot server could not complete
 * onboarding at all. That is defensible while the only people holding the APK
 * already run a server; it stops being defensible the moment it is listed on
 * F-Droid or Play, where it would earn one-star reviews from people who did
 * exactly what the listing invited them to do.
 *
 * The permissions are a different matter and still gate: without notification
 * and usage access the detector is blind and there is nothing to show.
 *
 * These assert on the source because the gating is a Compose expression with no
 * seam to call. They are deliberately narrow — the behavioural proof is the
 * build plus the screenshot tests tracked separately.
 */
class StandaloneSetupTest {

    private fun source(path: String): String {
        val file = File("src/main/java/com/cashpilot/android/$path")
        assertTrue(file.exists(), "expected $path to exist; the test is looking in the wrong place")
        return file.readText()
    }

    @Test
    fun `finishing setup no longer requires a server`() {
        val text = source("ui/screen/SetupScreen.kt")
        assertFalse(
            text.contains("enabled = serverDone && hasNotif && hasUsage"),
            "the finish button still demands a server URL and API key",
        )
    }

    @Test
    fun `but it still requires the permissions the detector cannot work without`() {
        val text = source("ui/screen/SetupScreen.kt")
        assertTrue(
            text.contains("enabled = hasNotif && hasUsage"),
            "notification and usage access must still gate setup — without them the app is blind",
        )
    }

    @Test
    fun `the server step is presented as optional`() {
        val text = source("ui/screen/SetupScreen.kt")
        assertTrue(text.contains("optional = true"), "the server card does not tell the user it can be skipped")
    }

    @Test
    fun `the button says what continuing without a server means`() {
        val text = source("ui/screen/SetupScreen.kt")
        assertTrue(
            text.contains("R.string.setup_continue_standalone"),
            "the button label does not change when no server is configured",
        )
    }

    @Test
    fun `standalone is not rendered as an error`() {
        val text = source("ui/screen/DashboardScreen.kt")
        assertFalse(
            text.contains("R.string.not_connected"),
            "the dashboard still calls standalone 'not connected', which reads as a failure",
        )
        assertTrue(text.contains("R.string.not_paired"), "expected the neutral not_paired wording")
    }

    @Test
    fun `the strings explain what pairing actually adds`() {
        val strings = File("src/main/res/values/strings.xml").readText()
        assertTrue(strings.contains("not_paired"), "not_paired string missing")
        assertTrue(
            strings.contains("setup_server_what_it_adds"),
            "nothing tells the user what they give up by skipping the server",
        )
        assertFalse(strings.contains("\"not_connected\""), "the old error-framed string is still defined")
    }
}

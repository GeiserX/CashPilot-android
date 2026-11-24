package com.cashpilot.android

import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.MonitoredApp
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class KnownAppsTest {

    @Test
    fun `all apps have unique slugs`() {
        val slugs = KnownApps.all.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size, "Duplicate slugs found")
    }

    @Test
    fun `all apps have unique package names`() {
        val packages = KnownApps.all.map { it.packageName }
        assertEquals(packages.size, packages.toSet().size, "Duplicate package names found")
    }

    @Test
    fun `byPackage map contains all entries`() {
        assertEquals(KnownApps.all.size, KnownApps.byPackage.size)
    }

    @Test
    fun `bySlug map contains all entries`() {
        assertEquals(KnownApps.all.size, KnownApps.bySlug.size)
    }

    @Test
    fun `byPackage lookup returns correct app`() {
        val app = KnownApps.byPackage["com.brd.earnapp.play"]
        assertNotNull(app)
        assertEquals("earnapp", app!!.slug)
        assertEquals("EarnApp", app.displayName)
    }

    @Test
    fun `bySlug lookup returns correct app`() {
        val app = KnownApps.bySlug["honeygain"]
        assertNull(app, "honeygain is not in the current known apps list")
    }

    @Test
    fun `all apps have non-empty slug and packageName`() {
        KnownApps.all.forEach { app ->
            assertTrue(app.slug.isNotBlank(), "App has blank slug: $app")
            assertTrue(app.packageName.isNotBlank(), "App has blank packageName: $app")
            assertTrue(app.displayName.isNotBlank(), "App has blank displayName: $app")
        }
    }

    @Test
    fun `iproyal has referral url`() {
        val app = KnownApps.bySlug["iproyal"]
        assertNotNull(app)
        assertNotNull(app!!.referralUrl)
        assertTrue(app.referralUrl!!.startsWith("https://"))
    }

    @Test
    fun `mysterium has referral url`() {
        val app = KnownApps.bySlug["mysterium"]
        assertNotNull(app)
        assertNotNull(app!!.referralUrl)
        assertTrue(app.referralUrl!!.startsWith("https://"))
    }

    @Test
    fun `nodle has no referral url`() {
        val app = KnownApps.bySlug["nodle"]
        assertNotNull(app)
        assertNull(app!!.referralUrl)
    }

    @Test
    fun `wipter has referral url`() {
        assertEquals("https://wipter.com/register?via=FB1373FBDA", KnownApps.bySlug["wipter"]!!.referralUrl)
    }

    /**
     * The referral links in KnownApps are copied by hand from the CashPilot web
     * catalog, and a copy drifts: Wipter's code reached the catalog after the
     * last sync, and 1.0.1 shipped without it. This reads each app's catalog
     * entry from the web repository's main branch and fails when the phone's
     * link differs.
     *
     * The rule follows the catalog's own meaning of `referral.code`. An entry
     * with a code has a signup_url that carries it, so the phone must open
     * exactly that URL. An entry without a code has no referral, so the phone
     * opens the store page (referralUrl null).
     *
     * It needs the network, as Robolectric's first jar download does. A failed
     * fetch fails the test and says it is a fetch failure, not drift: a drift
     * check that passes when it cannot read the catalog checks nothing.
     *
     * It runs only with CATALOG_DRIFT=1, which .github/workflows/catalog-drift.yml
     * sets on a schedule and on each push to main, never on a pull request. A
     * referral change on the web side must not turn an unrelated PR here red,
     * as Desktop's catalog-drift.yml never runs on PRs either.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "CATALOG_DRIFT", matches = "1")
    fun `every referral url matches the web catalog`() {
        val http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val drift = KnownApps.all.mapNotNull { app ->
            val category = catalogCategory[app.slug]
                ?: return@mapNotNull "${app.slug}: add its catalog folder (services/<folder>/${app.slug}.yml) to catalogCategory"
            val url = "$CATALOG/$category/${app.slug}.yml"
            val response = fetch(http, url)
            assertEquals(200, response.statusCode(), "Could not read $url. A fetch failure or a moved file, not drift.")
            val block = response.body().lines()
                .dropWhile { !it.startsWith("referral:") }
                .drop(1)
                .takeWhile { it.isBlank() || it.startsWith(" ") }
            fun field(key: String) = block.firstNotNullOfOrNull {
                Regex("""^  $key:\s*"?([^"\s]+)""").find(it)?.groupValues?.get(1)
            }
            val signupUrl = field("signup_url")
            assertNotNull(signupUrl, "$url has no referral.signup_url. The catalog layout changed; fix this test.")
            val expected = if (field("code") != null) signupUrl else null
            if (app.referralUrl == expected) null else "${app.slug}: the phone opens ${app.referralUrl}, the catalog says $expected"
        }
        assertTrue(drift.isEmpty(), "Referral links differ from the web catalog. Update MonitoredApp.kt:\n" + drift.joinToString("\n"))
    }

    /**
     * Reads [url] and tries again, up to [FETCH_TRIES] times, after a dropped
     * connection, a rate limit (429) or a server error (5xx), so one network
     * blip does not turn a PR red. Any other answer, a 404 included, comes back
     * at once. The last failure still fails the test.
     */
    private fun fetch(http: HttpClient, url: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(30)).build()
        for (attempt in 1..FETCH_TRIES) {
            try {
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                val transient = response.statusCode() == 429 || response.statusCode() >= 500
                if (!transient || attempt == FETCH_TRIES) return response
            } catch (e: IOException) {
                if (attempt == FETCH_TRIES) {
                    throw AssertionError("Could not read $url after $FETCH_TRIES tries. This is a fetch failure, not drift.", e)
                }
            }
            Thread.sleep(attempt * 3_000L)
        }
        error("unreachable")
    }

    @Test
    fun `monitored app data class equality`() {
        val a = MonitoredApp("test", "com.test", "Test App")
        val b = MonitoredApp("test", "com.test", "Test App")
        assertEquals(a, b)
    }

    @Test
    fun `monitored app with different referral urls are not equal`() {
        val a = MonitoredApp("test", "com.test", "Test", referralUrl = "https://a.com")
        val b = MonitoredApp("test", "com.test", "Test", referralUrl = "https://b.com")
        assertNotEquals(a, b)
    }

    private companion object {
        const val CATALOG = "https://raw.githubusercontent.com/GeiserX/CashPilot/main/services"
        const val FETCH_TRIES = 3

        /** The folder of each app's file in the web catalog's services/. */
        val catalogCategory = mapOf(
            "earnapp" to "bandwidth",
            "iproyal" to "bandwidth",
            "mysterium" to "bandwidth",
            "traffmonetizer" to "bandwidth",
            "bytelixir" to "bandwidth",
            "bytebenefit" to "bandwidth",
            "grass" to "depin",
            "titan" to "depin",
            "nodle" to "depin",
            "wipter" to "depin",
        )
    }
}

package com.cashpilot.android.board

import com.cashpilot.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * [English] reads strings.xml itself so BoardTest needs no Android. This holds
 * it to what Android actually returns for every string and plural, so a quote,
 * an escape or a plural rule it gets wrong fails here instead of hiding.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnglishMatchesAndroidTest {

    private val res = RuntimeEnvironment.getApplication().resources

    @Test
    fun everyStringReadsTheSameAsOnAndroid() {
        val ids = R.string::class.java.fields.map { it.name to it.getInt(null) }
        assertTrue("no strings found", ids.size > 50)
        for ((name, id) in ids) assertEquals(name, res.getString(id), English.get(id))
    }

    @Test
    fun everyPluralReadsTheSameAsOnAndroid() {
        val ids = R.plurals::class.java.fields.map { it.name to it.getInt(null) }
        assertTrue("no plurals found", ids.size > 10)
        for ((name, id) in ids) {
            for (n in listOf(0, 1, 2, 5, 21)) assertEquals("$name($n)", res.getQuantityString(id, n, n, n), English.plural(id, n, n, n))
        }
    }

    @Test
    fun formattedStringsMatchToo() {
        assertEquals(res.getString(R.string.server_line_failing, "h", "for 3 h"), English.get(R.string.server_line_failing, "h", "for 3 h"))
    }

    @Test
    fun theReaderDecodesWhatAapt2Decodes() {
        // The cases strings.xml uses: escaped apostrophes, a quoted span whose
        // spaces must survive, and white space that must collapse.
        assertEquals("Can't", English.android("Can\\'t"))
        assertEquals(", ", English.android("\", \""))
        assertEquals("a b", English.android("  a \n   b  "))
        // A reader that dropped the quoted space would fail here.
        assertTrue(English.android("\", \"") != ",")
    }
}

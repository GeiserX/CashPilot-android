package com.cashpilot.android.board

import com.cashpilot.android.ui.board.Languages
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** The in-app picker and Android's own language setting offer the same 20 languages. */
class LanguagesTest {

    private fun localeConfig(): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/locales_config.xml")).documentElement
        val list = doc.getElementsByTagName("locale")
        return (0 until list.length).map { (list.item(it) as org.w3c.dom.Element).getAttribute("android:name") }
    }

    @Test
    fun `the picker and locales_config list the same languages in the same order`() {
        assertEquals(20, Languages.all.size)
        assertEquals(Languages.all.map { it.tag }, localeConfig())
    }

    @Test
    fun `a stored tag finds its language, legacy codes and regions included`() {
        assertEquals("id", Languages.find("in")?.tag)
        assertEquals("id", Languages.find("id-ID")?.tag)
        assertEquals("fil", Languages.find("tl")?.tag)
        assertEquals("fil", Languages.find("fil-PH")?.tag)
        assertEquals("pt-BR", Languages.find("pt-BR")?.tag)
        assertEquals("zh-Hans", Languages.find("zh-Hans")?.tag)
        assertEquals("es", Languages.find("es-MX,en")?.tag)
        assertNull(Languages.find(""))
        assertNull(Languages.find("sw"))
    }

    @Test
    fun `capitals follow the language on screen, English when the app lacks the phone's`() {
        val turkish = Locale.forLanguageTag("tr-TR")
        val azeri = Locale.forLanguageTag("az")
        assertEquals(turkish, Languages.spoken(listOf(turkish)))
        assertEquals("İ", "i".uppercase(Languages.spoken(listOf(turkish))))
        // Azerbaijani also dots its capital I, but the app shows English there.
        assertEquals(Locale.ENGLISH, Languages.spoken(listOf(azeri)))
        assertEquals("THIS PHONE", "This phone".uppercase(Languages.spoken(listOf(azeri))))
        // Android takes the first language the app has, so the capitals do too.
        assertEquals(turkish, Languages.spoken(listOf(Locale.forLanguageTag("fi"), turkish)))
    }
}

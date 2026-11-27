package com.cashpilot.android.board

import com.cashpilot.android.R
import com.cashpilot.android.ui.board.Strings
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * The English words, read from `values/strings.xml` the way Android reads them,
 * so BoardTest stays a plain JVM test and still asserts the sentences a user
 * reads. `EnglishMatchesAndroidTest` checks this against Android's own
 * resources, string by string, so the two cannot drift apart.
 */
object English : Strings {
    override val locale: Locale = Locale.US

    private val file = File("src/main/res/values/strings.xml")

    private val doc by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun elements(tag: String): List<Element> {
        val list = doc.getElementsByTagName(tag)
        return (0 until list.length).map { list.item(it) as Element }
    }

    /** Name -> text of every `<string>`. */
    val strings: Map<String, String> by lazy { elements("string").associate { it.getAttribute("name") to android(it.textContent) } }

    /** Name -> quantity -> text of every `<plurals>`. */
    val plurals: Map<String, Map<String, String>> by lazy {
        elements("plurals").associate { p ->
            val items = p.getElementsByTagName("item")
            p.getAttribute("name") to (0 until items.length).associate {
                val item = items.item(it) as Element
                item.getAttribute("quantity") to android(item.textContent)
            }
        }
    }

    private fun nameOf(cls: Class<*>, id: Int): String =
        cls.fields.first { it.getInt(null) == id }.name

    override fun get(id: Int, vararg args: Any): String {
        val text = strings.getValue(nameOf(R.string::class.java, id))
        return if (args.isEmpty()) text else String.format(locale, text, *args)
    }

    override fun plural(id: Int, count: Int, vararg args: Any): String {
        val forms = plurals.getValue(nameOf(R.plurals::class.java, id))
        val text = forms[if (count == 1) "one" else "other"] ?: forms.getValue("other")
        return if (args.isEmpty()) text else String.format(locale, text, *args)
    }

    /**
     * What aapt2 makes of a resource's text: a quoted span keeps its spaces,
     * elsewhere runs of white space become one space, and backslash escapes
     * are decoded.
     */
    fun android(raw: String): String {
        val out = StringBuilder()
        // Which characters of out are unquoted white space, the only kind aapt2 trims.
        val loose = mutableListOf<Boolean>()
        fun add(c: Char, isLoose: Boolean = false) { out.append(c); loose += isLoose }
        var quoted = false
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            when {
                ch == '\\' && i + 1 < raw.length -> {
                    i++
                    when (val e = raw[i]) {
                        'n' -> add('\n')
                        't' -> add('\t')
                        'u' -> { add(raw.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }
                        else -> add(e)
                    }
                }
                ch == '"' -> quoted = !quoted
                ch.isWhitespace() && !quoted -> if (loose.lastOrNull() != true) add(' ', isLoose = true)
                else -> add(ch)
            }
            i++
        }
        var start = 0
        var end = out.length
        while (start < end && loose[start]) start++
        while (end > start && loose[end - 1]) end--
        return out.substring(start, end)
    }
}

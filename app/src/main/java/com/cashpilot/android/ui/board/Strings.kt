package com.cashpilot.android.ui.board

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * The app's words, read from the `strings.xml` files under `res/`.
 *
 * [Board] and [Words] take this as a parameter instead of reading Android's
 * resources themselves, so they stay plain functions: the app passes
 * [AndroidStrings], and BoardTest passes the English file read straight from
 * disk, which keeps every sentence a unit test.
 */
interface Strings {
    /** The language the words are in. Numbers and money are written its way. */
    val locale: Locale

    fun get(@StringRes id: Int, vararg args: Any): String

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

class AndroidStrings(private val res: Resources) : Strings {
    override val locale: Locale = res.configuration.locales[0]

    override fun get(id: Int, vararg args: Any): String =
        if (args.isEmpty()) res.getString(id) else res.getString(id, *args)

    override fun plural(id: Int, count: Int, vararg args: Any): String =
        if (args.isEmpty()) res.getQuantityString(id, count) else res.getQuantityString(id, count, *args)
}

/** The words in the language on screen now. Changes with the configuration. */
@Composable
fun strings(): Strings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { AndroidStrings(context.resources) }
}

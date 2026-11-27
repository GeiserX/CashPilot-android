package com.cashpilot.android.ui.board

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.cashpilot.android.R
import com.cashpilot.android.ui.theme.LocalPalette

/** One language the app is translated into, named in that language. */
data class Language(val tag: String, val name: String)

/**
 * The 20 languages, in the order the picker lists them. `res/xml/locales_config.xml`
 * lists the same tags for Android's own per-app language setting.
 *
 * The names are each language's own name, so a user who cannot read the
 * current language still finds theirs. They are not translated.
 */
object Languages {
    val all = listOf(
        Language("en", "English"),
        Language("es", "Español"),
        Language("pt-BR", "Português (Brasil)"),
        Language("fr", "Français"),
        Language("de", "Deutsch"),
        Language("it", "Italiano"),
        Language("nl", "Nederlands"),
        Language("pl", "Polski"),
        Language("ru", "Русский"),
        Language("uk", "Українська"),
        Language("tr", "Türkçe"),
        Language("ar", "العربية"),
        Language("hi", "हिन्दी"),
        Language("id", "Bahasa Indonesia"),
        Language("vi", "Tiếng Việt"),
        Language("th", "ไทย"),
        Language("fil", "Filipino"),
        Language("zh-Hans", "简体中文"),
        Language("ja", "日本語"),
        Language("ko", "한국어"),
    )

    /**
     * The picker's entry for a stored language tag, or null for "follow the
     * system". Android can hand back the legacy code ("in" for Indonesian,
     * "tl" for Filipino) or add a region, so this matches by language too.
     */
    fun find(tag: String): Language? {
        if (tag.isBlank()) return null
        val first = tag.split(',').first().trim()
        all.firstOrNull { it.tag.equals(first, ignoreCase = true) }?.let { return it }
        val lang = when (val l = first.substringBefore('-').lowercase()) {
            "in" -> "id"
            "tl" -> "fil"
            else -> l
        }
        return all.firstOrNull { it.tag.substringBefore('-') == lang }
    }

    /** The language picked in the app, or "" when it follows the system. */
    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    /**
     * Switch the app to [tag], or back to the system language for "". AppCompat
     * stores the choice and recreates the screen; Android 13 and later store it
     * in the system's per-app language setting.
     */
    fun pick(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }
}

/** What the Language row in Settings says: the picked language, or the system default. */
@Composable
fun languageLabel(tag: String): String = Languages.find(tag)?.name ?: stringResource(R.string.language_system)

/** A language's name, marked as that language so TalkBack reads it and Han text uses the right glyphs. */
private fun inItsLanguage(l: Language) = AnnotatedString(l.name, SpanStyle(localeList = LocaleList(l.tag)))

@Composable
fun LanguageScreen(current: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    val c = LocalPalette.current
    val picked = Languages.find(current)?.tag ?: ""
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackBar(stringResource(R.string.language), onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Gap(8)
            Panel(Modifier.selectableGroup(), pad = PaddingValues(vertical = 4.dp)) {
                LanguageRow(picked == "", { onPick("") }) {
                    Text(stringResource(R.string.language_system), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.language_system_body), style = MaterialTheme.typography.bodySmall, color = c.text2)
                }
                Languages.all.forEach { l ->
                    Divider()
                    LanguageRow(picked == l.tag, { onPick(l.tag) }) {
                        Text(inItsLanguage(l), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Gap(24)
        }
    }
}

@Composable
private fun LanguageRow(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val c = LocalPalette.current
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) { label() }
        Spacer(Modifier.width(12.dp))
        if (selected) Ic(R.drawable.ic_check_circle, tint = c.accent, size = 22) else Spacer(Modifier.size(22.dp))
    }
}

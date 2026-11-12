package com.cashpilot.android

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.cashpilot.android.service.Heartbeat
import com.cashpilot.android.util.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * The settings DataStore holds the fleet key and this phone's worker key, and
 * must not leave the device.
 *
 * allowBackup="false" stops cloud backup, but Android 12 and later ignore it for
 * device-to-device transfer. Only the dataExtractionRules file can keep the
 * DataStore off a new phone, so this checks the manifest points at it and that
 * the DataStore file and the heartbeat state are excluded in both sections.
 */
// Same SDK as IconGoldenTest: Robolectric 4.17 cannot run this module's targetSdk.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataBackupRulesTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    /** domain/path excludes per section, read from the merged resource. */
    private fun excludes(): Map<String, List<Pair<String, String>>> {
        val result = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val parser = context.resources.getXml(R.xml.data_extraction_rules)
        var section: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "cloud-backup", "device-transfer" -> {
                    section = parser.name
                    result.getOrPut(parser.name) { mutableListOf() }
                }
                "exclude" -> result.getValue(section!!).add(
                    parser.getAttributeValue(null, "domain") to
                        parser.getAttributeValue(null, "path"),
                )
            }
        }
        return result
    }

    @Test
    fun `the manifest points at the data extraction rules`() {
        // Robolectric does not fill ApplicationInfo.dataExtractionRulesRes, so
        // read the merged manifest that the Android Gradle plugin hands it.
        val props = java.util.Properties()
        javaClass.classLoader!!.getResourceAsStream("com/android/tools/test_config.properties")!!
            .use { props.load(it) }
        val manifest = java.io.File(props.getProperty("android_merged_manifest"))
        val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }.newDocumentBuilder().parse(manifest)
        val app = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
        val android = "http://schemas.android.com/apk/res/android"
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(android, "dataExtractionRules"))
    }

    /**
     * The path of the settings DataStore, relative to filesDir, from the same
     * function the preferencesDataStore delegate uses. Nothing is written: the
     * delegate is one store per process, bound to whichever test touched it
     * first, so a file written from here can land in another test's filesDir.
     */
    private fun settingsFile(): String =
        context.preferencesDataStoreFile(SettingsStore.FILE_NAME).relativeTo(context.filesDir).path

    private fun assertExcluded(domain: String, file: String) {
        val rules = excludes()
        for (section in listOf("cloud-backup", "device-transfer")) {
            val paths = rules[section].orEmpty()
                .filter { it.first == domain }.map { it.second.trimEnd('/') }
            assertTrue(
                "$section does not exclude $domain:$file",
                paths.any { it == "." || file == it || file.startsWith("$it/") },
            )
        }
    }

    @Test
    fun `the settings DataStore is kept out of cloud backup and device transfer`() {
        assertEquals("datastore/cashpilot_settings.preferences_pb", settingsFile())
        assertExcluded("file", settingsFile())
    }

    @Test
    fun `the heartbeat state is kept out of cloud backup and device transfer`() {
        // Last server URL and earnings snapshot: no key, but a phone set up
        // again starts clean, as allowBackup="false" intends.
        assertExcluded("sharedpref", "${Heartbeat.PREFS}.xml")
    }
}

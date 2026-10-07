package com.cashpilot.android

import android.content.Context
import android.content.pm.ApplicationInfo
import com.cashpilot.android.util.ServerUrlPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The network stack must let through every http host the app's policy allows,
 * and the policy must be the part that refuses public hosts.
 *
 * res/xml/network_security_config.xml keeps cleartext on at the base: Android
 * can allow plain http per name or domain suffix, never per address range, and
 * the common home setup is a server at http://192.168.x.x. So the config alone
 * would also let the fleet key go to a public host over http. ServerUrlPolicy
 * is the gate that refuses that, in the UI and before every heartbeat.
 *
 * The config is read through Android's own parser and matcher (the hidden
 * android.security.net.config classes in Robolectric's framework jar), so this
 * checks how the platform reads the file, not a copy of its rules.
 */
// Same SDK as IconGoldenTest: Robolectric 4.17 cannot run this module's targetSdk.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkSecurityConfigTest {

    private val privateHosts = listOf(
        "localhost",
        "127.0.0.1",
        "::1",
        "192.168.1.10",
        "10.0.0.1",
        "172.16.0.1",
        "100.64.0.1",
        "fd12:3456::1",
        "cashpilot.local",
        "cashpilot.lan",
        "nas.home.lan",
        "cashpilot.home.arpa",
        "cashpilot.internal",
        "cashpilot.example-tailnet.ts.net",
        "cashpilot",
    )

    private val publicHosts = listOf(
        "cashpilot.example.com",
        "8.8.8.8",
        "192.169.0.1",
        "100.128.0.1",
        "2001:db8::1",
        "local.example.com",
        "cashpilot.lan.example.com",
    )

    private fun configPermitsCleartext(host: String): Boolean {
        val context: Context = RuntimeEnvironment.getApplication()
        val source = Class.forName("android.security.net.config.XmlConfigSource")
            .getConstructor(Context::class.java, Int::class.javaPrimitiveType, ApplicationInfo::class.java)
            .newInstance(context, R.xml.network_security_config, context.applicationInfo)
        val configSourceType = Class.forName("android.security.net.config.ConfigSource")
        val config = Class.forName("android.security.net.config.ApplicationConfig")
            .getConstructor(configSourceType)
            .newInstance(source)
        return config.javaClass.getMethod("isCleartextTrafficPermitted", String::class.java)
            .invoke(config, host) as Boolean
    }

    @Test
    fun `the config lets through every http host the policy allows`() {
        privateHosts.forEach {
            assertEquals("policy, $it", true, ServerUrlPolicy.allowsCleartext(it))
            assertEquals("config, $it", true, configPermitsCleartext(it))
        }
    }

    @Test
    fun `the policy, not the config, is what refuses a public host`() {
        publicHosts.forEach {
            assertEquals("config alone would allow $it", true, configPermitsCleartext(it))
            assertEquals("policy refuses $it", false, ServerUrlPolicy.allowsCleartext(it))
        }
    }
}

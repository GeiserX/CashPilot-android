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
 * The app's http rule and the network stack's http rule are the same list.
 *
 * ServerUrlPolicy decides in the UI and before every heartbeat whether http is
 * allowed; res/xml/network_security_config.xml is what Android actually
 * enforces when OkHttp opens the connection. If the policy allowed a host the
 * config refuses, the app would promise a connection that then fails with a
 * cleartext error; if the config allowed a host the policy refuses, the config
 * would be wider than it needs to be.
 *
 * The config is read through Android's own parser and matcher (the hidden
 * android.security.net.config classes in Robolectric's framework jar), so this
 * checks how the platform reads the file, not a copy of its rules.
 */
// Same SDK as IconGoldenTest: Robolectric 4.17 cannot run this module's targetSdk.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkSecurityConfigTest {

    private val allowed = listOf(
        "localhost",
        "127.0.0.1",
        "::1",
        "cashpilot.local",
        "cashpilot.lan",
        "nas.home.lan",
        "cashpilot.home.arpa",
        "cashpilot.internal",
        "cashpilot.example-tailnet.ts.net",
    )

    private val refused = listOf(
        "cashpilot.example.com",
        "8.8.8.8",
        "192.168.1.10",
        "10.0.0.1",
        "172.16.0.1",
        "100.64.0.1",
        "127.0.0.2",
        "fd12:3456::1",
        "cashpilot",
        "notlocal",
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
    fun `the config allows http only to loopback and private names`() {
        allowed.forEach { assertEquals("config, $it", true, configPermitsCleartext(it)) }
        refused.forEach { assertEquals("config, $it", false, configPermitsCleartext(it)) }
    }

    @Test
    fun `the policy allows http to exactly the hosts the config allows`() {
        (allowed + refused).forEach {
            assertEquals("policy vs config, $it", configPermitsCleartext(it), ServerUrlPolicy.allowsCleartext(it))
        }
    }
}

package com.cashpilot.android

import com.cashpilot.android.util.ServerUrlPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** The fleet key goes out over https, or over http only to a host that cannot be public. */
class ServerUrlPolicyTest {

    @ParameterizedTest(name = "allowed: {0}")
    @ValueSource(
        strings = [
            "https://cashpilot.example.com",
            "https://8.8.8.8:8443/",
            "http://localhost:8080",
            "http://127.0.0.1",
            "HTTP://localhost",
            "http://[::1]:8080",
            "http://cashpilot.local",
            "http://CashPilot.Local:8000/",
            "http://cashpilot.lan",
            "http://cashpilot.home.arpa",
            "http://cashpilot.internal",
            "http://cashpilot.example-tailnet.ts.net:8000",
            // Private, link-local and Tailscale addresses: the common home setup.
            "http://10.1.2.3:8000",
            "http://172.16.0.1",
            "http://192.168.1.10:8000/",
            "http://169.254.10.20",
            "http://100.64.0.1",
            "http://127.0.0.2",
            "http://[fd12:3456::1]",
            "http://[fe80::1]",
            // A dotless name resolves through the LAN's search domain, never publicly.
            "http://cashpilot:8000",
        ],
    )
    fun `sends the key`(url: String) {
        assertTrue(ServerUrlPolicy.allowsToken(url))
    }

    @ParameterizedTest(name = "refused: {0}")
    @ValueSource(
        strings = [
            "http://8.8.8.8",
            "http://cashpilot.example.com",
            "http://evil.lan.example.com",
            "http://100.63.255.255",
            "http://100.128.0.1",
            "http://172.15.255.255",
            "http://172.32.0.1",
            "http://192.169.0.1",
            "http://[2001:db8::1]",
            "http://[fec0::1]",
            "http://134744072",
            "http://010.0.0.1",
            "http://192.168.1.256",
            "http://192.168.1.1.example.com",
            "http://10.0.0.1@cashpilot.example.com",
            // Non-canonical hosts that OkHttp would turn into 8.8.8.8 or a public name.
            "http://8%2e8%2e8%2e8",
            "http://cashpilot%2eexample%2ecom",
            "http://\uFF18\uFF0E\uFF18\uFF0E\uFF18\uFF0E\uFF18",
            "ftp://192.168.1.10",
            "cashpilot.example.com",
            "",
        ],
    )
    fun `refuses the key`(url: String) {
        assertFalse(ServerUrlPolicy.allowsToken(url))
    }
}

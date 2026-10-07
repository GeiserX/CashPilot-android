package com.cashpilot.android.util

import io.ktor.http.Url

/**
 * Whether the fleet key may be sent to a server URL.
 *
 * https always. http only to a host that cannot be on the public internet,
 * because people run the server at home without a certificate: loopback,
 * private and link-local ranges, Tailscale (the tunnel encrypts), and names
 * that cannot be public. Decided from the URL's literal host and never from
 * DNS, so the answer does not depend on the network the phone is on.
 *
 * This is the gate. res/xml/network_security_config.xml keeps cleartext on
 * at the base, because Android can allow plain http per name or domain
 * suffix but not per address range, and the common home setup is a server at
 * http://192.168.x.x. NetworkSecurityConfigTest checks that the config does
 * not refuse what this policy allows, and that this policy refuses public
 * hosts the config alone would let through.
 */
object ServerUrlPolicy {

    private val LOOPBACK = setOf("localhost", "127.0.0.1", "::1")
    private val PRIVATE_SUFFIXES = listOf(".local", ".lan", ".home.arpa", ".internal", ".ts.net")
    private val IPV4 = Regex("""(0|[1-9]\d{0,2})(\.(0|[1-9]\d{0,2})){3}""")
    private val IPV6_LOOPBACK = Regex("""[0:]*::?0{0,3}1""")
    private val HEXTET = Regex("""[0-9a-f]{1,4}""")

    // Only a host already in canonical form is judged. OkHttp decodes %2e and
    // normalises Unicode dots and digits, so 8%2e8%2e8%2e8 would reach 8.8.8.8.
    private val CANONICAL = Regex("""[a-z0-9.:-]+""")

    // A dotless number is an IPv4 address to the system resolver (134744072 is 8.8.8.8).
    private val NUMERIC = Regex("""0x[0-9a-f]*|\d+""")

    fun allowsToken(url: String): Boolean {
        if ("://" !in url) return false
        val parsed = runCatching { Url(url.trim()) }.getOrNull() ?: return false
        return when (parsed.protocol.name) {
            "https" -> true
            "http" -> allowsCleartext(parsed.host)
            else -> false
        }
    }

    fun allowsCleartext(rawHost: String): Boolean {
        val host = rawHost.removeSurrounding("[", "]").trimEnd('.').lowercase()
        return when {
            !CANONICAL.matches(host) -> false
            host in LOOPBACK -> true
            IPV4.matches(host) -> isPrivateIpv4(host.split('.').map { it.toInt() })
            ':' in host -> isPrivateIpv6(host)
            '.' in host -> PRIVATE_SUFFIXES.any { host.endsWith(it) && host.length > it.length }
            else -> !NUMERIC.matches(host)
        }
    }

    private fun isPrivateIpv4(o: List<Int>): Boolean = o.all { it <= 255 } && when (o[0]) {
        10, 127 -> true
        172 -> o[1] in 16..31
        192 -> o[1] == 168
        169 -> o[1] == 254
        100 -> o[1] in 64..127
        else -> false
    }

    private fun isPrivateIpv6(host: String): Boolean {
        if (IPV6_LOOPBACK.matches(host)) return true
        val first = if (host.startsWith("::")) "0" else host.substringBefore(':')
        if (!HEXTET.matches(first)) return false
        val value = first.toInt(16)
        return (value and 0xfe00) == 0xfc00 || (value and 0xffc0) == 0xfe80
    }
}

package com.cashpilot.android.util

import io.ktor.http.Url

/**
 * Whether the fleet key may be sent to a server URL.
 *
 * https always. http only to loopback and to names that cannot be public,
 * because people run the server at home without a certificate. Decided from
 * the URL's literal host and never from DNS, so the answer does not depend on
 * the network the phone is on.
 *
 * The http list is exactly what res/xml/network_security_config.xml allows,
 * because that file is what the network stack enforces: it can name hosts and
 * domain suffixes but not IP ranges, so a LAN or Tailscale IP over http is
 * refused here rather than failing later with a cleartext error.
 * NetworkSecurityConfigTest fails if the two lists disagree.
 */
object ServerUrlPolicy {

    private val LOOPBACK = setOf("localhost", "127.0.0.1", "::1")
    private val PRIVATE_SUFFIXES = listOf(".local", ".lan", ".home.arpa", ".internal", ".ts.net")

    // Only a host already in canonical form is judged. OkHttp decodes %2e and
    // normalises Unicode dots and digits, so 8%2e8%2e8%2e8 would reach 8.8.8.8.
    private val CANONICAL = Regex("""[a-z0-9.:-]+""")

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
        return CANONICAL.matches(host) &&
            (host in LOOPBACK || PRIVATE_SUFFIXES.any { host.endsWith(it) && host.length > it.length })
    }
}

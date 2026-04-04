package com.cashpilot.android.model

import kotlinx.serialization.Serializable

/**
 * A passive income app that CashPilot monitors on this device.
 * The slug matches the CashPilot service catalog (e.g. "honeygain", "earnapp").
 */
@Serializable
data class MonitoredApp(
    val slug: String,
    val packageName: String,
    val displayName: String,
    /** Web referral URL (opens browser, tracks referral, redirects to Play Store). Null = direct Play Store link. */
    val referralUrl: String? = null,
)

/** Built-in list of known passive income Android apps and their package names. */
object KnownApps {
    val all = listOf(
        MonitoredApp("earnapp", "com.brd.earnapp.play", "EarnApp",
            referralUrl = "https://earnapp.com/i/TSMD9wSm"),
        MonitoredApp("iproyal", "com.iproyal.android", "IPRoyal Pawns",
            referralUrl = "https://pawns.app?r=19266874"),
        MonitoredApp("mysterium", "network.mysterium.provider", "MystNodes"),
        MonitoredApp("traffmonetizer", "com.traffmonetizer.client", "Traffmonetizer",
            referralUrl = "https://traffmonetizer.com/?aff=2111758"),
        MonitoredApp("bytelixir", "com.bytelixir.blapp", "Bytelixir",
            referralUrl = "https://bytelixir.com/?ref=OYEIRE0VSZBZ"),
        MonitoredApp("bytebenefit", "io.bytebenefit.app", "ByteBenefit"),
        MonitoredApp("grass", "io.getgrass.www", "Grass",
            referralUrl = "https://app.getgrass.io/register/?referralCode=kn8FNEPnUr2tMqE"),
        MonitoredApp("titan", "com.titan_network_vip.titan_app", "Titan Network",
            referralUrl = "https://edge.titannet.info/signup?inviteCode=2GKKJ495"),
        MonitoredApp("nodle", "io.nodle.cash", "Nodle Cash"),
        MonitoredApp("uprock", "com.uprock.mining", "Uprock",
            referralUrl = "https://link.uprock.com/i/33e8492e"),
        MonitoredApp("wipter", "com.wipter.app", "Wipter"),
    )

    val byPackage = all.associateBy { it.packageName }
    val bySlug = all.associateBy { it.slug }
}

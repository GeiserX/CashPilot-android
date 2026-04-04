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
)

/** Built-in list of known passive income Android apps and their package names. */
object KnownApps {
    val all = listOf(
        MonitoredApp("earnapp", "com.brd.earnapp.play", "EarnApp"),
        MonitoredApp("iproyal", "com.iproyal.android", "IPRoyal Pawns"),
        MonitoredApp("mysterium", "network.mysterium.provider", "MystNodes"),
        MonitoredApp("traffmonetizer", "com.traffmonetizer.client", "Traffmonetizer"),
        MonitoredApp("bytelixir", "com.bytelixir.blapp", "Bytelixir"),
        MonitoredApp("bytebenefit", "io.bytebenefit.app", "ByteBenefit"),
        MonitoredApp("grass", "io.getgrass.www", "Grass"),
        MonitoredApp("titan", "com.titan_network_vip.titan_app", "Titan Network"),
        MonitoredApp("nodle", "io.nodle.cash", "Nodle Cash"),
        MonitoredApp("uprock", "com.uprock.mining", "Uprock"),
        MonitoredApp("wipter", "com.wipter.app", "Wipter"),
    )

    val byPackage = all.associateBy { it.packageName }
    val bySlug = all.associateBy { it.slug }
}

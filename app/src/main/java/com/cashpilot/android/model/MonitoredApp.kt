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
        MonitoredApp("honeygain", "com.honeygain.app", "Honeygain"),
        MonitoredApp("earnapp", "com.bright.earnapp", "EarnApp"),
        MonitoredApp("iproyal", "com.iproyal.pawns", "IPRoyal Pawns"),
        MonitoredApp("mysterium", "network.mysterium.vpn", "Mysterium"),
        MonitoredApp("packetstream", "com.packetstream.android", "PacketStream"),
        MonitoredApp("traffmonetizer", "com.traffmonetizer.client", "Traffmonetizer"),
        MonitoredApp("repocket", "com.repocket.android", "Repocket"),
        MonitoredApp("peer2profit", "com.peer2profit.app", "Peer2Profit"),
        MonitoredApp("bytelixir", "com.bytelixir.app", "Bytelixir"),
        MonitoredApp("bytebenefit", "com.bytebenefit.app", "ByteBenefit"),
        MonitoredApp("grass", "com.getgrass.android", "Grass"),
        MonitoredApp("gaganode", "com.gaga.gaganode", "GagaNode"),
        MonitoredApp("titan", "com.titan.network", "Titan Network"),
        MonitoredApp("nodle", "io.nodle.cash", "Nodle Cash"),
        MonitoredApp("passiveapp", "com.passiveincome.app", "PassiveApp"),
        MonitoredApp("uprock", "com.uprock.android", "Uprock"),
        MonitoredApp("wipter", "com.wipter.app", "Wipter"),
    )

    val byPackage = all.associateBy { it.packageName }
    val bySlug = all.associateBy { it.slug }
}

package moe.matsuri.nb4a.tproxy

import io.nekohasekai.sagernet.database.DataStore
import moe.matsuri.nb4a.tproxy.core.XrayTags
import moe.matsuri.nb4a.tproxy.network.NetworkLimits
import moe.matsuri.nb4a.tproxy.root.RootConfigBuildContext
import moe.matsuri.nb4a.tproxy.root.RootEbpfRuntimeConfig
import moe.matsuri.nb4a.tproxy.root.RootIptablesConfig
import moe.matsuri.nb4a.tproxy.root.RootModeStartConfig
import moe.matsuri.nb4a.tproxy.root.RootStartConfig

data class TproxyStartConfig(
    override val root: RootStartConfig,
    val tproxyPort: Int,
    val iptablesConfig: RootIptablesConfig,
    override val rootEbpfConfig: RootEbpfRuntimeConfig?,
) : RootModeStartConfig

val TproxyBaseIptablesConfig = RootIptablesConfig(
    mark = TproxyFwmark,
    ipv4Table = TproxyRouteTable,
    ipv6Table = TproxyRouteTable,
)

/**
 * Build the TproxyStartConfig. The sing-box config JSON (which already contains
 * the tproxy inbound emitted by ConfigBuilder in tproxy service mode) is passed
 * in; here we only resolve the tproxy port, iptables and eBPF configuration.
 */
fun RootConfigBuildContext.buildTproxyStartConfig(coreConfigJson: String): TproxyStartConfig {
    val tproxyPort = DataStore.tproxyPortValue()
    val iptablesConfig = buildRootIptablesConfig(
        base = TproxyBaseIptablesConfig,
        ignoredLocalInterfaceNames = setOf(TproxyDummyDevice),
    )
    return TproxyStartConfig(
        root = buildRootStartConfig(coreConfigJson),
        tproxyPort = tproxyPort,
        iptablesConfig = iptablesConfig,
        rootEbpfConfig = buildRootEbpfRuntimeConfig(iptablesConfig),
    )
}

/** Resolve the tproxy inbound port from DataStore, defaulting to the max port. */
fun DataStore.tproxyPortValue(): Int {
    return tproxyRootPort.takeIf { it in NetworkLimits.PORT_MIN..NetworkLimits.PORT_MAX } ?: DefaultTproxyPort
}
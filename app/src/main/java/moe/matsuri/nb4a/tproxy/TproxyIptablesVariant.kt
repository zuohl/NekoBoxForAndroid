package moe.matsuri.nb4a.tproxy

import moe.matsuri.nb4a.tproxy.root.RootIp6Command
import moe.matsuri.nb4a.tproxy.root.RootIp6tablesCommand
import moe.matsuri.nb4a.tproxy.root.RootIpCommand
import moe.matsuri.nb4a.tproxy.root.RootIptablesConfig
import moe.matsuri.nb4a.tproxy.root.RootIptablesCommand

fun RootIptablesConfig.ipv4IptablesVariant(): TproxyIptablesVariant {
    return TproxyIptablesVariant(
        command = RootIptablesCommand,
        ipCommand = RootIpCommand,
        routeTable = ipv4Table,
        routeDestination = "default",
        preroutingChain = TproxyPreroutingChain,
        outputChain = TproxyOutputChain,
        dnsOutputChain = TproxyDnsOutputChain,
        tproxyOnIp = "0.0.0.0",
        localInterfaceCidrs = localInterfaceIpv4Cidrs,
        proxyPrivateCidrs = proxyPrivateIpv4Cidrs,
        bypassPrivateCidrs = bypassPrivateIpv4Cidrs,
    )
}

fun RootIptablesConfig.ipv6IptablesVariant(useDummyInterface: Boolean): TproxyIptablesVariant {
    return TproxyIptablesVariant(
        command = RootIp6tablesCommand,
        ipCommand = RootIp6Command,
        routeTable = ipv6Table,
        routeDestination = "default",
        preroutingChain = TproxyPrerouting6Chain,
        outputChain = TproxyOutput6Chain,
        dnsOutputChain = TproxyDnsOutput6Chain,
        tproxyOnIp = "::",
        localInterfaceCidrs = localInterfaceIpv6Cidrs,
        proxyPrivateCidrs = proxyPrivateIpv6Cidrs,
        bypassPrivateCidrs = bypassPrivateIpv6Cidrs,
        dummyInterface = DummyInterfaceConfig.takeIf { useDummyInterface },
    )
}

fun buildGlobalIpv6AddressCheckCommand(): String {
    return "$RootIp6Command addr show scope global 2>/dev/null | grep -q 'inet6 '"
}

data class TproxyIptablesVariant(
    val command: String,
    val ipCommand: String,
    val routeTable: String,
    val routeDestination: String,
    val preroutingChain: String,
    val outputChain: String,
    val dnsOutputChain: String,
    val tproxyOnIp: String,
    val localInterfaceCidrs: List<String>,
    val proxyPrivateCidrs: List<String>,
    val bypassPrivateCidrs: List<String>,
    val dummyInterface: TproxyDummyInterfaceConfig? = null,
)

data class TproxyDummyInterfaceConfig(
    val device: String,
    val address: String,
    val mark: String,
    val routeTable: String,
    val outputChain: String,
    val preroutingChain: String,
)

private val DummyInterfaceConfig = TproxyDummyInterfaceConfig(
    device = TproxyDummyDevice,
    address = TproxyDummyAddress,
    mark = TproxyDummyFwmark,
    routeTable = TproxyDummyRouteTable,
    outputChain = "ASTERISK_TPROXY6_DUMMY",
    preroutingChain = "ASTERISK_TPROXY6_DUMMY_PRE",
)

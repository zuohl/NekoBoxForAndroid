package moe.matsuri.nb4a.tproxy.root

import android.content.Context
import android.os.Process
import io.nekohasekai.sagernet.database.DataStore
import moe.matsuri.nb4a.tproxy.appstate.modes.ProxyAppListModeGlobal
import moe.matsuri.nb4a.tproxy.utils.toTrimmedNonEmptyDistinctList

data class RootIptablesConfig(
    val mark: String,
    val ipv4Table: String,
    val ipv6Table: String,
    val enableEbpfRules: Boolean = false,
    val enableEbpfDirectCidrBypass: Boolean = false,
    val externalInterfacePrefixes: List<String> = emptyList(),
    val ignoredInterfaces: List<String> = emptyList(),
    val localInterfaceIpv4Cidrs: List<String> = emptyList(),
    val localInterfaceIpv6Cidrs: List<String> = emptyList(),
    val proxyPrivateIpv4Cidrs: List<String> = emptyList(),
    val proxyPrivateIpv6Cidrs: List<String> = emptyList(),
    val bypassPrivateIpv4Cidrs: List<String> = emptyList(),
    val bypassPrivateIpv6Cidrs: List<String> = emptyList(),
    val forcedBypassUids: List<Int> = emptyList(),
    val proxyAppListMode: Int = ProxyAppListModeGlobal,
    val proxyApplicationUids: List<Int> = emptyList(),
)

/**
 * Populate the iptables config from NekoBox's DataStore. Adapted from
 * AsteriskNG's withAppSettings: app list mode and selected apps come from
 * DataStore.proxyApps/bypass/individual; eBPF toggles come from DataStore.
 */
fun RootIptablesConfig.withAppSettings(
    context: Context,
    ignoredLocalInterfaceNames: Set<String>,
): RootIptablesConfig {
    val localInterfaceCidrs = collectRootLocalInterfaceCidrs(
        ignoredInterfaceNames = ignoredLocalInterfaceNames,
    ).toTrimmedNonEmptyDistinctList()
    val bypassPrivateCidrs = RootDefaultBypassPrivateCidrs.toTrimmedNonEmptyDistinctList()
    val selectedAppKeys = DataStore.individual.split('\n').filter { it.isNotBlank() }
    val appListMode = if (selectedAppKeys.isEmpty()) {
        ProxyAppListModeGlobal
    } else {
        DataStore.toProxyAppListMode()
    }

    return copy(
        externalInterfacePrefixes = emptyList(),
        ignoredInterfaces = emptyList(),
        localInterfaceIpv4Cidrs = localInterfaceCidrs.ipv4Cidrs(),
        localInterfaceIpv6Cidrs = localInterfaceCidrs.ipv6Cidrs(),
        proxyPrivateIpv4Cidrs = bypassPrivateCidrs.ipv4Cidrs(),
        proxyPrivateIpv6Cidrs = bypassPrivateCidrs.ipv6Cidrs(),
        bypassPrivateIpv4Cidrs = bypassPrivateCidrs.ipv4Cidrs(),
        bypassPrivateIpv6Cidrs = bypassPrivateCidrs.ipv6Cidrs(),
        forcedBypassUids = listOf(Process.myUid()),
        proxyAppListMode = appListMode,
        proxyApplicationUids = if (appListMode == ProxyAppListModeGlobal) {
            emptyList()
        } else {
            context.resolveRootProxyApplicationUids(selectedAppKeys)
        },
        enableEbpfRules = DataStore.tproxyRootEbpf,
        enableEbpfDirectCidrBypass = DataStore.tproxyRootEbpfDirectBypass,
    )
}

private fun List<String>.ipv4Cidrs(): List<String> {
    return filterNot { cidr -> ":" in cidr }
}

private fun List<String>.ipv6Cidrs(): List<String> {
    return filter { cidr -> ":" in cidr }
}
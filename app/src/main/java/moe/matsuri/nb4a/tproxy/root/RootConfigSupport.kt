package moe.matsuri.nb4a.tproxy.root

import android.content.Context
import io.nekohasekai.sagernet.IPv6Mode
import io.nekohasekai.sagernet.database.DataStore
import moe.matsuri.nb4a.tproxy.appstate.modes.ProxyAppListModeBlacklist
import moe.matsuri.nb4a.tproxy.appstate.modes.ProxyAppListModeGlobal
import moe.matsuri.nb4a.tproxy.appstate.modes.ProxyAppListModeWhitelist
import moe.matsuri.nb4a.tproxy.core.XrayCoreLogPaths
import moe.matsuri.nb4a.tproxy.core.prepareXrayCoreLogPaths
import moe.matsuri.nb4a.tproxy.resources.XrayResourceFilePaths
import moe.matsuri.nb4a.tproxy.resources.prepareXrayResourceFilePaths

/**
 * Builds the RootStartConfig for the tproxy daemon. Adapted from AsteriskNG's
 * RootConfigSupport: instead of going through AsteriskNG's AppState/xray
 * config factory, the sing-box config JSON is produced by NekoBox's own
 * ConfigBuilder (which emits the tproxy inbound in tproxy service mode) and
 * passed in here. The flags are read from NekoBox's DataStore.
 */
class RootConfigBuildContext(
    private val androidContext: Context,
    private val resourceFilePaths: XrayResourceFilePaths,
    private val coreLogPaths: XrayCoreLogPaths,
) {
    fun buildRootStartConfig(coreConfigJson: String): RootStartConfig {
        return RootStartConfig(
            xrayConfigJson = coreConfigJson,
            setuidgidPath = resourceFilePaths.setuidgidPath,
            runtimeLayout = resourceFilePaths.toRootRuntimeLayout(),
            enableIpv6 = DataStore.ipv6Mode != IPv6Mode.DISABLE,
            enableRootIpv6Disabler = DataStore.tproxyRootIpv6Disabler,
            enableLocalDns = true,
            enableFakeDns = DataStore.enableFakeDns,
            enableAccessLog = DataStore.tproxyRootAccessLog,
            coreLogPaths = coreLogPaths,
        )
    }

    fun buildRootIptablesConfig(
        base: RootIptablesConfig,
        ignoredLocalInterfaceNames: Set<String>,
    ): RootIptablesConfig {
        return base.withAppSettings(
            context = androidContext,
            ignoredLocalInterfaceNames = ignoredLocalInterfaceNames,
        )
    }

    fun buildRootEbpfRuntimeConfig(iptablesConfig: RootIptablesConfig): RootEbpfRuntimeConfig? {
        if (!iptablesConfig.enableEbpfRules) return null
        val runtimeLayout = resourceFilePaths.toRootRuntimeLayout()
        return RootEbpfRuntimeConfig(
            matcherPath = runtimeLayout.bpfMatcherPath,
            bpfPolicyPath = runtimeLayout.bpfPolicyPath,
            directCidrPathV4 = runtimeLayout.rootEbpfDirectCidrPathV4,
            directCidrPathV6 = runtimeLayout.rootEbpfDirectCidrPathV6,
            directCidrSourcePathsV4 = emptyList(),
            directCidrSourcePathsV6 = emptyList(),
            policy = iptablesConfig.toRootEbpfPolicy(
                enableIpv6 = DataStore.ipv6Mode != IPv6Mode.DISABLE,
                directCidrPathV4 = runtimeLayout.rootEbpfDirectCidrPathV4,
                directCidrPathV6 = runtimeLayout.rootEbpfDirectCidrPathV6,
                xtOutputV4ProgramPath = RootEbpfXtOutputV4ProgramPath,
                xtOutputV6ProgramPath = RootEbpfXtOutputV6ProgramPath,
                xtPreroutingV4ProgramPath = RootEbpfXtPreroutingV4ProgramPath,
                xtPreroutingV6ProgramPath = RootEbpfXtPreroutingV6ProgramPath,
            ),
        )
    }
}

fun Context.prepareRootConfigBuildContext(): RootConfigBuildContext {
    return RootConfigBuildContext(
        androidContext = applicationContext,
        resourceFilePaths = prepareXrayResourceFilePaths(),
        coreLogPaths = prepareXrayCoreLogPaths(),
    )
}

/**
 * Resolves the per-app proxy list mode from NekoBox's DataStore into the
 * AsteriskNG ProxyAppListMode constants used by the iptables/eBPF rules.
 */
fun DataStore.toProxyAppListMode(): Int {
    val proxyApps = proxyApps
    val individual = individual.split('\n').filter { it.isNotBlank() }
    if (!proxyApps || individual.isEmpty()) return ProxyAppListModeGlobal
    return if (bypass) ProxyAppListModeBlacklist else ProxyAppListModeWhitelist
}
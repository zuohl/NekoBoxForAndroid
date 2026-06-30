package moe.matsuri.nb4a.tproxy.root

import moe.matsuri.nb4a.tproxy.core.XrayCoreLogPaths
import moe.matsuri.nb4a.tproxy.core.logDirectoryPath
import java.io.File

/**
 * Configuration shared by all root daemon modes. `xrayConfigJson` is the
 * sing-box config JSON produced by NekoBox's ConfigBuilder (the name is
 * retained from the AsteriskNG port for source compatibility with the rest of
 * the root engine).
 */
data class RootStartConfig(
    val xrayConfigJson: String,
    val setuidgidPath: String,
    val runtimeLayout: RootRuntimeLayout,
    val enableIpv6: Boolean,
    val enableRootIpv6Disabler: Boolean,
    val enableLocalDns: Boolean,
    val enableFakeDns: Boolean,
    val enableAccessLog: Boolean,
    val coreLogPaths: XrayCoreLogPaths,
) {
    val configPath: String
        get() = runtimeLayout.configPath
}

interface RootModeStartConfig {
    val root: RootStartConfig
    val rootEbpfConfig: RootEbpfRuntimeConfig?
        get() = null
}

val RootStartConfig.startupScriptPath: String
    get() = runtimeLayout.startupScriptPath

val RootStartConfig.bootLogDirPath: String
    get() = coreLogPaths.logDirectoryPath()

val RootStartConfig.bootLogPath: String
    get() = File(bootLogDirPath, RootBootLogFileName).absolutePath

val RootRuntimeLayout.startupScriptPath: String
    get() = File(dataDir, RootStartupScriptFileName).absolutePath

val RootRuntimeLayout.ipv6DisablerPidPath: String
    get() = File(dataDir, RootIpv6DisablerPidFileName).absolutePath

val RootRuntimeLayout.bpfPolicyPath: String
    get() = File(dataDir, RootEbpfPolicyFileName).absolutePath

val RootRuntimeLayout.rootEbpfDirectCidrPathV4: String
    get() = File(dataDir, RootEbpfDirectCidrV4FileName).absolutePath

val RootRuntimeLayout.rootEbpfDirectCidrPathV6: String
    get() = File(dataDir, RootEbpfDirectCidrV6FileName).absolutePath

val RootStartConfig.ipv6DisablerLogPath: String
    get() = File(coreLogPaths.logDirectoryPath(), RootIpv6DisablerLogFileName).absolutePath
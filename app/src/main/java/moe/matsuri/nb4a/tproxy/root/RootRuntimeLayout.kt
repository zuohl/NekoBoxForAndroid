package moe.matsuri.nb4a.tproxy.root

import android.content.Context
import moe.matsuri.nb4a.tproxy.resources.XrayResourceFilePaths
import moe.matsuri.nb4a.tproxy.resources.prepareXrayResourceFilePaths
import java.io.File

data class RootRuntimeLayout(
    val configPath: String,
    val xrayCorePath: String,
    val ipv6DisablerPath: String,
    val bpfMatcherPath: String,
    val hevSocks5TunnelPath: String,
    val dataDir: String,
    val pidPath: String,
)

fun Context.prepareRootRuntimeLayout(): RootRuntimeLayout {
    val resourceFilePaths = prepareXrayResourceFilePaths()
    return resourceFilePaths.toRootRuntimeLayout()
}

fun XrayResourceFilePaths.toRootRuntimeLayout(): RootRuntimeLayout {
    val dir = File(dataDir)
    return RootRuntimeLayout(
        configPath = File(dir, RootConfigFileName).absolutePath,
        xrayCorePath = xrayCorePath,
        ipv6DisablerPath = ipv6DisablerPath,
        bpfMatcherPath = bpfMatcherPath,
        hevSocks5TunnelPath = hevSocks5TunnelPath,
        dataDir = dataDir,
        pidPath = File(dir, RootPidFileName).absolutePath,
    )
}

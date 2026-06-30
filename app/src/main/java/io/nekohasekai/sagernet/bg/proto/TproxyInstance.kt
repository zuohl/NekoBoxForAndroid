package io.nekohasekai.sagernet.bg.proto

import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.ServiceNotification
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.fmt.buildConfig
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.SagerNet
import kotlinx.coroutines.runBlocking
import moe.matsuri.nb4a.tproxy.TproxyRootRunner
import moe.matsuri.nb4a.tproxy.buildTproxyStartConfig
import moe.matsuri.nb4a.tproxy.root.RootModeEngine
import moe.matsuri.nb4a.tproxy.root.prepareRootConfigBuildContext
import moe.matsuri.nb4a.tproxy.system.AndroidRootShellGateway

/**
 * Drives the tproxy root daemon: a standalone sing-box process started through
 * root, with iptables TPROXY rules steering traffic into it. Unlike the VPN
 * ProxyInstance, the core is NOT loaded in-process (libcore); instead a root
 * shell launches the bundled sing-box binary, so the daemon can keep running
 * even when the app is not in the foreground.
 */
class TproxyInstance(profile: ProxyEntity, service: BaseService.Interface? = null) : ProxyInstance(profile, service) {

    private val rootAccess = AndroidRootShellGateway()
    private var engine: RootModeEngine<*>? = null

    /** No in-process core: the sing-box binary is started as a root daemon. */
    override suspend fun loadConfig() {
        // intentionally do not call Libcore.newSingBoxInstance; the daemon is
        // launched as a separate process by the root engine in launch().
    }

    override fun launch() {
        val cfg = config.config
        val buildContext = SagerNet.application.prepareRootConfigBuildContext()
        val runner = TproxyRootRunner(rootAccess)
        @Suppress("UNCHECKED_CAST")
        val modeEngine = RootModeEngine(
            context = SagerNet.application,
            rootAccess = rootAccess,
            runner = runner,
            modeName = "TPROXY",
            logTag = "NekoTproxy",
            buildConfig = { ctx -> ctx.buildTproxyStartConfig(cfg) },
        )
        engine = modeEngine
        runBlocking {
            modeEngine.start(buildContext)
        }
        runOnDefaultDispatcher {
            looper = service?.let { TrafficLooper(it.data, this) }
            looper?.start()
        }
    }

    override fun close() {
        runBlocking {
            engine?.stop()
            engine = null
            looper?.stop()
            looper = null
        }
    }
}
package moe.matsuri.nb4a.tproxy.root

import android.content.Context
import io.nekohasekai.sagernet.database.DataStore
import moe.matsuri.nb4a.tproxy.core.clearCoreLogs
import moe.matsuri.nb4a.tproxy.core.startCoreLogTailers
import moe.matsuri.nb4a.tproxy.logs.AndroidAppLogger
import moe.matsuri.nb4a.tproxy.core.CoreLogFileTailer
import moe.matsuri.nb4a.tproxy.system.AndroidRootShellGateway
import java.io.File

/**
 * Coordinates a root daemon mode (tproxy). Adapted from AsteriskNG's
 * RootModeEngine: the AndroidModeProxyEngine/proxy-engine abstractions are
 * dropped because NekoBox drives the proxy through its own service/instance
 * layer; this class now exposes plain start/stop/status/boot-script methods
 * invoked by TproxyInstance.
 */
class RootModeEngine<Config : RootModeStartConfig>(
    private val context: Context,
    private val rootAccess: AndroidRootShellGateway,
    private val runner: RootModeRunner<Config>,
    private val modeName: String,
    private val logTag: String,
    private val buildConfig: (RootConfigBuildContext) -> Config,
) {
    private var logFileTailers: List<CoreLogFileTailer> = emptyList()

    suspend fun start(buildContext: RootConfigBuildContext): Config {
        if (!rootAccess.hasRootAccess()) {
            error("Root access is required for $modeName mode")
        }
        stop()
        val config = buildConfig(buildContext)
        val runtimeLayout = config.root.runtimeLayout
        if (!File(runtimeLayout.xrayCorePath).canExecute()) {
            File(runtimeLayout.xrayCorePath).setExecutable(true, false)
        }
        runner.prepareCoreLogFiles(config.root.coreLogPaths)
        config.root.coreLogPaths.clearCoreLogs(logTag)
        logFileTailers = config.root.coreLogPaths.startCoreLogTailers(config.root.enableAccessLog)
        runCatching {
            runner.start(config)
            if (DataStore.tproxyRootBootScript) {
                runner.installBootScript(config)
            } else {
                runner.uninstallBootScript(config.root)
            }
        }.onFailure { error ->
            runCatching { runner.stop(config.root.runtimeLayout) }
                .onFailure { stopError -> AndroidAppLogger.warn(logTag, "Failed to clean up $modeName after startup failure", stopError) }
            logFileTailers.forEach { it.stop() }
            logFileTailers = emptyList()
            AndroidAppLogger.error(logTag, "Failed to start $modeName mode", error)
            throw IllegalStateException("Failed to start $modeName mode: ${error.message.orEmpty()}", error)
        }
        return config
    }

    suspend fun stop() {
        logFileTailers.forEach { it.stop() }
        logFileTailers = emptyList()
        runCatching {
            runner.stop(context.prepareRootRuntimeLayout())
        }.onFailure { error ->
            AndroidAppLogger.warn(logTag, "Failed to stop $modeName mode", error)
        }
    }

    suspend fun ownsRuntime(): Boolean {
        return runner.ownsRuntime(context.prepareRootRuntimeLayout())
    }

    suspend fun isRunning(): Boolean {
        return runner.isRunning(context.prepareRootRuntimeLayout())
    }
}
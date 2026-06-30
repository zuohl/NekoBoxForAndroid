package moe.matsuri.nb4a.tproxy.core

import android.content.Context
import moe.matsuri.nb4a.tproxy.logs.AndroidAppLogger
import java.io.File

/**
 * Log file paths for the root daemon. sing-box writes to these via the `log`
 * object in its config (error log) and the access log option. The daemon runs
 * as root so it can write here directly; the app reads them back for diagnostics.
 *
 * Retains the XrayCoreLogPaths name from the AsteriskNG port for
 * source-compatibility with the rest of the root engine.
 */
data class XrayCoreLogPaths(
    val accessLogPath: String,
    val errorLogPath: String,
)

private const val LOG_DIR_NAME = "tproxy-root"
private const val ACCESS_LOG_NAME = "boxd-access.log"
private const val ERROR_LOG_NAME = "boxd.log"

fun Context.prepareXrayCoreLogPaths(): XrayCoreLogPaths {
    val dir = File(filesDir, LOG_DIR_NAME).apply { mkdirs() }
    return XrayCoreLogPaths(
        accessLogPath = File(dir, ACCESS_LOG_NAME).absolutePath,
        errorLogPath = File(dir, ERROR_LOG_NAME).absolutePath,
    )
}

fun XrayCoreLogPaths.logDirectoryPath(): String {
    return File(errorLogPath).parentFile?.absolutePath
        ?: File(accessLogPath).parentFile?.absolutePath
        ?: error("Core log directory is unavailable")
}

fun XrayCoreLogPaths.logFilePaths(): List<String> {
    return listOf(accessLogPath, errorLogPath).filter(String::isNotBlank)
}

/** A no-op tailer: AsteriskNG streams core logs into a UI repository; NekoBox
 *  just leaves them on disk for `tail`-based diagnostics in the startup script. */
class CoreLogFileTailer {
    fun start() = Unit
    fun stop() = Unit
}

fun XrayCoreLogPaths.startCoreLogTailers(enableAccessLog: Boolean): List<CoreLogFileTailer> {
    return listOf(CoreLogFileTailer()).onEach { it.start() }
}

fun XrayCoreLogPaths.clearCoreLogs(logTag: String) {
    logFilePaths().filter(String::isNotBlank).forEach { logPath ->
        runCatching {
            File(logPath).apply {
                parentFile?.mkdirs()
                writeText("")
            }
        }.onFailure { error -> AndroidAppLogger.warn(logTag, "Failed to clear core log file: $logPath", error) }
    }
}
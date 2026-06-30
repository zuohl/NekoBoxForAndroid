package moe.matsuri.nb4a.tproxy.resources

import android.content.Context
import java.io.File
import java.io.OutputStream

/**
 * Paths to the native libraries shipped with the app and the shared data
 * directory used by the tproxy root daemon. The class name is retained from the
 * AsteriskNG port (XrayResourceFilePaths) so the rest of the root engine stays
 * source-compatible; here `xrayCorePath` actually points at the sing-box
 * `libboxd.so` binary.
 */
data class XrayResourceFilePaths(
    val dataDir: String,
    val xrayCorePath: String,
    val setuidgidPath: String,
    val ipv6DisablerPath: String,
    val bpfMatcherPath: String,
    val hevSocks5TunnelPath: String,
)

private const val CORE_LIBRARY_NAME = "libboxd.so"
private const val SETUIDGID_LIBRARY_NAME = "libsetuidgid.so"
private const val IPV6_DISABLER_LIBRARY_NAME = "libipv6disabler.so"
private const val BPF_MATCHER_LIBRARY_NAME = "libbpf-matcher.so"

/** Shared directory reachable by both the app process and the root daemon. */
fun Context.coreResourceFilesDir(): File {
    return File(filesDir, "tproxy-root").apply { mkdirs() }
}

fun Context.prepareXrayResourceFilePaths(): XrayResourceFilePaths {
    val nativeDir = applicationInfo.nativeLibraryDir
    return XrayResourceFilePaths(
        dataDir = coreResourceFilesDir().absolutePath,
        xrayCorePath = File(nativeDir, CORE_LIBRARY_NAME).absolutePath,
        setuidgidPath = File(nativeDir, SETUIDGID_LIBRARY_NAME).absolutePath,
        ipv6DisablerPath = File(nativeDir, IPV6_DISABLER_LIBRARY_NAME).absolutePath,
        bpfMatcherPath = File(nativeDir, BPF_MATCHER_LIBRARY_NAME).absolutePath,
        // hev-socks5-tunnel is only used by the tun2socks root mode, not tproxy.
        hevSocks5TunnelPath = "",
    )
}

/** Atomically write a file so the root daemon never observes a half-written file. */
fun writeAtomically(target: File, write: (OutputStream) -> Unit) {
    val parent = target.parentFile ?: error("Parent directory is unavailable for ${target.absolutePath}")
    parent.mkdirs()
    synchronized(writeLockFor(target)) {
        val tempPrefix = "${target.name}.".let { prefix -> if (prefix.length >= 3) prefix else prefix.padEnd(3, '_') }
        val tempFile = File.createTempFile(tempPrefix, ".tmp", parent)
        try {
            tempFile.outputStream().use(write)
            if (tempFile.length() <= 0) {
                tempFile.delete()
                error("${target.name} is empty")
            }
            if (target.exists() && !target.delete()) {
                tempFile.delete()
                error("Failed to replace ${target.name}")
            }
            if (!tempFile.renameTo(target)) {
                tempFile.delete()
                error("Failed to replace ${target.name}")
            }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
    }
}

private val WriteLocks = mutableMapOf<String, Any>()

private fun writeLockFor(target: File): Any {
    return synchronized(WriteLocks) {
        WriteLocks.getOrPut(target.absolutePath) { Any() }
    }
}
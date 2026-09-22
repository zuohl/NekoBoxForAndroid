package io.nekohasekai.sagernet.ktx

import io.nekohasekai.sagernet.database.DataStore
import libcore.Libcore
import java.io.InputStream
import java.io.OutputStream

object Logs {

    private fun mkTag(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace[4].className.substringAfterLast(".")
    }

    @Volatile
    private var cachedLevel: Int = -1

    fun updateLogLevel(newLevel: Int? = null) {
        cachedLevel = newLevel ?: try {
            DataStore.logLevel
        } catch (_: Throwable) {
            3
        }
    }

    private val level: Int
        get() {
            var l = cachedLevel
            if (l < 0) {
                l = try {
                    DataStore.logLevel
                } catch (_: Throwable) {
                    3
                }
                cachedLevel = l
            }
            return l
        }

    fun d(message: String) {
        if (level >= 3) {
            Libcore.nekoLogPrintln("[Debug] [${mkTag()}] $message")
        }
    }

    fun d(message: String, exception: Throwable) {
        if (level >= 3) {
            Libcore.nekoLogPrintln("[Debug] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
        }
    }

    fun i(message: String) {
        if (level >= 2) {
            Libcore.nekoLogPrintln("[Info] [${mkTag()}] $message")
        }
    }

    fun i(message: String, exception: Throwable) {
        if (level >= 2) {
            Libcore.nekoLogPrintln("[Info] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
        }
    }

    fun w(message: String) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Warning] [${mkTag()}] $message")
        }
    }

    fun w(message: String, exception: Throwable) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Warning] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
        }
    }

    fun w(exception: Throwable) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Warning] [${mkTag()}] " + exception.stackTraceToString())
        }
    }

    fun e(message: String) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Error] [${mkTag()}] $message")
        }
    }

    fun e(message: String, exception: Throwable) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Error] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
        }
    }

    fun e(exception: Throwable) {
        if (level >= 1) {
            Libcore.nekoLogPrintln("[Error] [${mkTag()}] " + exception.stackTraceToString())
        }
    }

}

fun InputStream.use(out: OutputStream) {
    use { input ->
        out.use { output ->
            input.copyTo(output)
        }
    }
}
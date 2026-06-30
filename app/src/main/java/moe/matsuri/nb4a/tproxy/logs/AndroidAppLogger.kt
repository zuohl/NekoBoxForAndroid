package moe.matsuri.nb4a.tproxy.logs

import io.nekohasekai.sagernet.ktx.Logs

object AndroidAppLogger {
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Logs.e("[$tag] $message", throwable)
        } else {
            Logs.e("[$tag] $message")
        }
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Logs.w("[$tag] $message", throwable)
        } else {
            Logs.w("[$tag] $message")
        }
    }

    fun info(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Logs.i("[$tag] $message", throwable)
        } else {
            Logs.i("[$tag] $message")
        }
    }

    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Logs.d("[$tag] $message", throwable)
        } else {
            Logs.d("[$tag] $message")
        }
    }

    fun platformWarn(tag: String, message: String, error: Throwable? = null) {
        if (error != null) {
            Logs.w("[$tag] $message", error)
        } else {
            Logs.w("[$tag] $message")
        }
    }
}

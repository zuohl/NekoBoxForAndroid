package io.nekohasekai.sagernet.bg

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.PowerManager
import io.nekohasekai.sagernet.SagerNet

/**
 * Foreground service for the tproxy root daemon mode. Unlike VpnService it does
 * not own a tun interface; traffic is steered into the standalone sing-box
 * daemon through root iptables TPROXY rules. The daemon runs independently of
 * the app process and can survive the app being killed (and, when the boot
 * script is installed, a reboot).
 */
class TproxyService : Service(), BaseService.Interface {
    override val data = BaseService.Data(this)
    override val tag: String get() = "SagerNetTproxyService"
    override fun createNotification(profileName: String): ServiceNotification =
        ServiceNotification(
            this,
            profileName,
            "service-tproxy",
            true,
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

    override var wakeLock: PowerManager.WakeLock? = null
    override var upstreamInterfaceName: String? = null

    @SuppressLint("WakelockTimeout")
    override fun acquireWakeLock() {
        wakeLock = SagerNet.power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sagernet:tproxy")
            .apply { acquire() }
    }

    override fun onBind(intent: Intent) = super.onBind(intent)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        super<BaseService.Interface>.onStartCommand(intent, flags, startId)
}
package ir.mmxkarimi.shockwave.ui.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Current audio output for the player's output chip. */
class OutputRoute(c: Context) {
    enum class Kind { SPEAKER, WIRED, USB, BLUETOOTH }

    data class Route(val kind: Kind, val name: String?)

    fun interface Listener {
        fun onRoute(r: Route)
    }

    private val context: Context = c.applicationContext
    private val am: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val main = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<AudioDeviceInfo>) {
            publish()
        }

        override fun onAudioDevicesRemoved(removed: Array<AudioDeviceInfo>) {
            publish()
        }
    }

    fun start(l: Listener) {
        listener = l
        if (am == null) {
            l.onRoute(Route(Kind.SPEAKER, null))
            return
        }
        am.registerAudioDeviceCallback(callback, main)
        publish()
    }

    fun stop() {
        listener = null
        am?.unregisterAudioDeviceCallback(callback)
    }

    private fun publish() {
        val l = listener ?: return
        l.onRoute(currentRoute())
    }

    fun currentRoute(): Route {
        if (am == null) return Route(Kind.SPEAKER, null)
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var best: AudioDeviceInfo? = null
        var bestRank = -1
        for (info in devices) {
            val r = rank(info.type)
            if (r > bestRank) {
                bestRank = r
                best = info
            }
        }
        if (best == null) return Route(Kind.SPEAKER, null)
        val k = kindOf(best.type)
        var name: String? = null
        if (k == Kind.BLUETOOTH) {
            val hasBtPerm = Build.VERSION.SDK_INT < 31 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (hasBtPerm) {
                val pn = best.productName
                if (pn != null && pn.isNotEmpty()) name = pn.toString()
            }
        }
        return Route(k, name)
    }

    private fun kindOf(type: Int): Kind {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> Kind.BLUETOOTH

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_AUX_LINE -> Kind.WIRED

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> Kind.USB

            else -> Kind.SPEAKER
        }
    }

    private fun rank(type: Int): Int {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> 40

            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET -> 30

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_AUX_LINE -> 20

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 10
            else -> 0
        }
    }
}

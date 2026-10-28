package ir.mmxkarimi.shockwave.ui.theme

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

/**
 * Decides whether decorative animation (wave travel, play-button morph, glow) may run.
 * Off when the system animator scale is 0, battery saver is on, or the app's own reduce-motion setting is on.
 * Widgets then draw a static, equally legible frame.
 */
object MotionPolicy {
    @Volatile
    private var appReduceMotion = false

    /** The user's battery-saver choice: TRUE = always on, FALSE = always off, null = follow the system. */
    @Volatile
    private var saverOverride: Boolean? = null

    /** App-level override (Settings screen). */
    @JvmStatic
    fun setAppReduceMotion(reduce: Boolean) {
        appReduceMotion = reduce
    }

    /** Settings screen's battery-saver choice; null follows the system. */
    @JvmStatic
    fun setBatterySaverOverride(forced: Boolean?) {
        saverOverride = forced
    }

    @JvmStatic
    fun decorativeMotionEnabled(context: Context): Boolean {
        if (appReduceMotion) return false
        val app = context.applicationContext
        val scale = Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) return false
        return !batterySaver(app)
    }

    /** Battery saver only (skip grain, glow and other purely decorative layers). */
    @JvmStatic
    fun batterySaver(context: Context): Boolean {
        val forced = saverOverride
        if (forced != null) return forced
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isPowerSaveMode == true
    }
}

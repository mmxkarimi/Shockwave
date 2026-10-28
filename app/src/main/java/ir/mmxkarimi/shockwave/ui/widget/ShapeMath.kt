package ir.mmxkarimi.shockwave.ui.widget

import kotlin.math.cos

/** Pure polar shape math for the play button morph: a circle (paused) to a 10-lobe cookie (playing). */
object ShapeMath {
    const val COOKIE_LOBES: Int = 10
    const val COOKIE_DEPTH: Float = 0.075f

    /** Unit-radius distance of the cookie edge at angle [t]; lobe tips at 1, valleys inside. */
    @JvmStatic
    fun cookie(t: Double): Float {
        return ((1.0 + COOKIE_DEPTH * cos(COOKIE_LOBES * t)) / (1.0 + COOKIE_DEPTH)).toFloat()
    }

    /** Morph between circle (m=0) and cookie (m=1); m may overshoot slightly for spring feel. */
    @JvmStatic
    fun radius(t: Double, m: Float): Float {
        return 1f + (cookie(t) - 1f) * m
    }
}

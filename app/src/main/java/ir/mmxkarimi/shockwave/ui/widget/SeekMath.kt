package ir.mmxkarimi.shockwave.ui.widget

import java.util.Locale
import kotlin.math.roundToLong

/** Pure seek-bar math (JVM-testable). The bar always runs left-to-right, also in RTL (tape direction, D-A). */
object SeekMath {
    /** Fraction 0..1 of [pos] in [duration]; 0 when duration is unknown. */
    @JvmStatic
    fun fraction(pos: Long, duration: Long): Float {
        if (duration <= 0) return 0f
        if (pos <= 0) return 0f
        if (pos >= duration) return 1f
        return (pos.toDouble() / duration).toFloat()
    }

    /** X coordinate of a position on a track spanning [left, right]. */
    @JvmStatic
    fun xFor(pos: Long, duration: Long, left: Float, right: Float): Float {
        return left + (right - left) * fraction(pos, duration)
    }

    /** Position for a touch at [x] on a track spanning [left, right], clamped to [0, duration]. */
    @JvmStatic
    fun positionFor(x: Float, left: Float, right: Float, duration: Long): Long {
        if (duration <= 0 || right <= left) return 0
        val f = (x - left) / (right - left)
        if (f <= 0f) return 0
        if (f >= 1f) return duration
        return (f.toDouble() * duration).roundToLong()
    }

    /** Clamp a keyboard/accessibility step. */
    @JvmStatic
    fun step(pos: Long, delta: Long, duration: Long): Long {
        val p = pos + delta
        if (p < 0) return 0
        return if (duration > 0 && p > duration) duration else p
    }

    /** "m:ss" or "h:mm:ss", digits in the given locale (Persian digits for fa). */
    @JvmStatic
    fun formatTime(ms: Long, locale: Locale): String {
        val safeMs = if (ms < 0) 0 else ms
        val total = safeMs / 1000
        val h = total / 3600
        val m = (total / 60) % 60
        val s = total % 60
        return if (h > 0) {
            String.format(locale, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(locale, "%d:%02d", m, s)
        }
    }
}

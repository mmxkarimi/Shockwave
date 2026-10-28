package ir.mmxkarimi.shockwave.ui.widget

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Pure math for the wavy line (seek bar, mini-player progress, nav underline). */
object WaveMath {
    const val TWO_PI: Float = (PI * 2).toFloat()

    /** Phase (radians, [0, 2pi)) after [elapsedMs] for a wave travelling one wavelength per [periodMs]. */
    @JvmStatic
    fun phaseAt(elapsedMs: Long, periodMs: Long): Float {
        if (periodMs <= 0) return 0f
        return TWO_PI * (elapsedMs % periodMs).toFloat() / periodMs
    }

    /** Vertical offset of the wave at [x]. */
    @JvmStatic
    fun y(x: Float, wavelength: Float, amplitude: Float, phase: Float): Float {
        return amplitude * sin(TWO_PI * x / wavelength - phase)
    }

    /** Moves [current] towards [target] at a rate that crosses [0,1] in [durationMs]. */
    @JvmStatic
    fun approach(current: Float, target: Float, dtMs: Long, durationMs: Long): Float {
        if (durationMs <= 0) return target
        val step = dtMs.toFloat() / durationMs
        return if (current < target) {
            min(target, current + step)
        } else {
            max(target, current - step)
        }
    }
}

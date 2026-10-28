package ir.mmxkarimi.shockwave.ui.player

import android.content.Context
import androidx.core.graphics.ColorUtils
import ir.mmxkarimi.shockwave.ui.theme.MotionPolicy

/**
 * Artwork-derived roles computed from a track's placeholder seed.
 */
class PlayerPalette private constructor(hue: Float) {
    val primary: Int = ColorUtils.HSLToColor(floatArrayOf(hue, 0.78f, 0.80f))
    val onPrimary: Int = ColorUtils.HSLToColor(floatArrayOf(hue, 0.90f, 0.14f))
    val container: Int = ColorUtils.HSLToColor(floatArrayOf(hue, 0.50f, 0.25f))
    val onContainer: Int = ColorUtils.HSLToColor(floatArrayOf(hue, 0.90f, 0.90f))
    val glow: Int = ColorUtils.HSLToColor(floatArrayOf(hue, 0.60f, 0.42f))
    val deep: Int = ColorUtils.HSLToColor(floatArrayOf((hue + 20f) % 360f, 0.55f, 0.08f))

    companion object {
        @JvmStatic
        fun fromColor(argb: Int): PlayerPalette {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(argb, hsl)
            return PlayerPalette(if (hsl[1] < 0.08f) 212f else hsl[0])
        }

        @JvmStatic
        fun forSeed(seed: Int): PlayerPalette {
            val hue = (((seed and 0x7fffffff) % 300 + 180) % 360).toFloat()
            return PlayerPalette(hue)
        }

        @JvmStatic
        fun motionAllowed(c: Context): Boolean {
            return MotionPolicy.decorativeMotionEnabled(c)
        }
    }
}

package ir.mmxkarimi.shockwave.ui.player

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Runtime contrast guard. Given the downscaled backdrop pixels, finds the smallest scrim alpha so that text keeps
 * at least [minRatio] against every backdrop pixel.
 */
object ContrastGuard {
    @JvmStatic
    fun luminance(rgb: Int): Double {
        return 0.2126 * lin((rgb shr 16) and 0xFF) + 0.7152 * lin((rgb shr 8) and 0xFF) + 0.0722 * lin(rgb and 0xFF)
    }

    private fun lin(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }

    @JvmStatic
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @JvmStatic
    fun blend(top: Int, bottom: Int, alpha: Float): Int {
        val r = (((top shr 16) and 0xFF) * alpha + ((bottom shr 16) and 0xFF) * (1 - alpha)).roundToInt()
        val g = (((top shr 8) and 0xFF) * alpha + ((bottom shr 8) and 0xFF) * (1 - alpha)).roundToInt()
        val bl = ((top and 0xFF) * alpha + (bottom and 0xFF) * (1 - alpha)).roundToInt()
        return -0x1000000 or (r shl 16) or (g shl 8) or bl
    }

    @JvmStatic
    fun requiredScrimAlpha(
        px: IntArray?,
        w: Int,
        h: Int,
        bandTop: Float,
        bandBottom: Float,
        scrimRgb: Int,
        baseAlpha: Float,
        textArgb: Int,
        minRatio: Double
    ): Float {
        if (px == null || w <= 0 || h <= 0 || px.size < w * h) return baseAlpha
        val y0 = max(0, min(h - 1, floor(bandTop * h).toInt()))
        val y1 = max(y0 + 1, min(h, ceil(bandBottom * h).toInt()))
        val textAlpha = ((textArgb ushr 24) and 0xFF) / 255f
        val textOpaque = -0x1000000 or (textArgb and 0x00FFFFFF)

        var worstAlpha = baseAlpha
        for (y in y0 until y1) {
            val row = y * w
            for (x in 0 until w) {
                val back = -0x1000000 or (px[row + x] and 0x00FFFFFF)
                var alpha = worstAlpha
                while (alpha < 1f) {
                    val compBack = blend(scrimRgb, back, alpha)
                    val effText = blend(textOpaque, compBack, textAlpha)
                    if (ratio(effText, compBack) >= minRatio) break
                    alpha = min(1f, alpha + 0.05f)
                }
                if (alpha > worstAlpha) worstAlpha = alpha
            }
        }
        return worstAlpha
    }
}

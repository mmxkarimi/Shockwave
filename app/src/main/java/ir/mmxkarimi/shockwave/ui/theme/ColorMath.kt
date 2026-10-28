package ir.mmxkarimi.shockwave.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pure color math (no Android dependencies, so it is JVM-testable). Colors are packed ARGB ints. */
object ColorMath {
    @JvmStatic
    fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    }

    @JvmStatic
    fun alpha(c: Int): Int = c ushr 24

    @JvmStatic
    fun red(c: Int): Int = (c shr 16) and 0xFF

    @JvmStatic
    fun green(c: Int): Int = (c shr 8) and 0xFF

    @JvmStatic
    fun blue(c: Int): Int = c and 0xFF

    @JvmStatic
    fun withAlpha(c: Int, alpha: Float): Int {
        val a = (clamp01(alpha) * 255f).roundToInt()
        return (c and 0x00FFFFFF) or (a shl 24)
    }

    /** Linear interpolation per channel, t in [0,1]. */
    @JvmStatic
    fun lerp(from: Int, to: Int, t: Float): Int {
        val clampedT = clamp01(t)
        return argb(
            (alpha(from) + (alpha(to) - alpha(from)) * clampedT).roundToInt(),
            (red(from) + (red(to) - red(from)) * clampedT).roundToInt(),
            (green(from) + (green(to) - green(from)) * clampedT).roundToInt(),
            (blue(from) + (blue(to) - blue(from)) * clampedT).roundToInt()
        )
    }

    /** RGB to HSL; out = {h 0..360, s 0..1, l 0..1}. */
    @JvmStatic
    fun rgbToHsl(c: Int, out: FloatArray) {
        val r = red(c) / 255f
        val g = green(c) / 255f
        val b = blue(c) / 255f
        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        val l = (maxVal + minVal) / 2f
        val d = maxVal - minVal
        var h: Float
        val s: Float
        if (d == 0f) {
            h = 0f
            s = 0f
        } else {
            s = d / (1f - abs(2f * l - 1f))
            h = when (maxVal) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h *= 60f
            if (h < 0f) h += 360f
        }
        out[0] = h
        out[1] = clamp01(s)
        out[2] = clamp01(l)
    }

    /** HSL to packed ARGB (alpha 255). */
    @JvmStatic
    fun hslToRgb(h: Float, s: Float, l: Float): Int {
        val clampedS = clamp01(s)
        val clampedL = clamp01(l)
        val c = (1f - abs(2f * clampedL - 1f)) * clampedS
        val normalizedH = (h % 360f + 360f) % 360f
        val x = c * (1f - abs((normalizedH / 60f) % 2f - 1f))
        val m = clampedL - c / 2f
        var r = 0f
        var g = 0f
        var b = 0f
        val sextant = (normalizedH / 60f).toInt()
        when (sextant) {
            0 -> { r = c; g = x; b = 0f }
            1 -> { r = x; g = c; b = 0f }
            2 -> { r = 0f; g = c; b = x }
            3 -> { r = 0f; g = x; b = c }
            4 -> { r = x; g = 0f; b = c }
            5 -> { r = c; g = 0f; b = x }
        }
        return argb(255, ((r + m) * 255f).roundToInt(), ((g + m) * 255f).roundToInt(), ((b + m) * 255f).roundToInt())
    }

    /** WCAG relative luminance [0,1]. */
    @JvmStatic
    fun luminance(c: Int): Double {
        val r = srgbToLinear(red(c) / 255.0)
        val g = srgbToLinear(green(c) / 255.0)
        val b = srgbToLinear(blue(c) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun srgbToLinear(c: Double): Double {
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }

    /** WCAG 2 contrast ratio in [1, 21]. */
    @JvmStatic
    fun contrast(c1: Int, c2: Int): Double {
        val l1 = luminance(c1)
        val l2 = luminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @JvmStatic
    fun clamp01(v: Float): Float = max(0f, min(1f, v))
}

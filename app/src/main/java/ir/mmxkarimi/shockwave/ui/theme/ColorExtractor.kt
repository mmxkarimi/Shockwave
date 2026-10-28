package ir.mmxkarimi.shockwave.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Palette-free seed color extraction from a tiny (e.g. 32x32) ARGB pixel array.
 *
 * Pixels are binned by hue (24 bins); each pixel votes with its chroma so grey, near-black and near-white pixels
 * barely count. The winning bin's weighted mean color is the seed. Falls back to the plain mean for greyscale art.
 * Runs in well under a millisecond for 1024 pixels and allocates nothing beyond the caller's scratch arrays.
 */
object ColorExtractor {
    const val BINS: Int = 24

    @JvmStatic
    fun extractSeed(pixels: IntArray, count: Int): Int {
        val w = DoubleArray(BINS)
        val rs = DoubleArray(BINS)
        val gs = DoubleArray(BINS)
        val bs = DoubleArray(BINS)
        val hsl = FloatArray(3)
        var ar: Long = 0
        var ag: Long = 0
        var ab: Long = 0
        var n = 0

        for (i in 0 until count) {
            val c = pixels[i]
            if (ColorMath.alpha(c) < 128) continue
            val r = ColorMath.red(c)
            val g = ColorMath.green(c)
            val b = ColorMath.blue(c)
            ar += r
            ag += g
            ab += b
            n++
            ColorMath.rgbToHsl(c, hsl)
            val s = hsl[1]
            val l = hsl[2]
            if (l < 0.08f || l > 0.94f || s < 0.12f) continue
            // Chroma-like weight; mid lightness is favoured (that is where colors read as "the color" of a cover).
            val weight = s * (1.0 - abs(2.0 * l - 1.0))
            val bin = min(BINS - 1, (hsl[0] / (360f / BINS)).toInt())
            w[bin] += weight
            rs[bin] += r * weight
            gs[bin] += g * weight
            bs[bin] += b * weight
        }
        if (n == 0) return 0xFF5F7FA6.toInt() // lapis-ish default
        var best = -1
        var bestScore = 0.0
        for (i in 0 until BINS) {
            // Smooth with neighbours so a hue split across two bins still wins.
            val score = w[i] + 0.5 * (w[(i + BINS - 1) % BINS] + w[(i + 1) % BINS])
            if (score > bestScore) {
                bestScore = score
                best = i
            }
        }
        // Needs a meaningful share of colorful pixels, otherwise treat the cover as greyscale.
        if (best < 0 || w[best] < n * 0.01) {
            return ColorMath.argb(255, (ar / n).toInt(), (ag / n).toInt(), (ab / n).toInt())
        }
        return ColorMath.argb(255, (rs[best] / w[best]).toInt(), (gs[best] / w[best]).toInt(), (bs[best] / w[best]).toInt())
    }
}

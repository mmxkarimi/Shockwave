package ir.mmxkarimi.shockwave.ui.widget

import ir.mmxkarimi.shockwave.ui.theme.ColorMath

/** Deterministic placeholder gradient colors from Track.placeholderSeed (pure, JVM-testable). */
object PlaceholderArt {
    /** Two colors (top-left, bottom-right) for a seed; same seed, same colors, on every device. */
    @JvmStatic
    fun colors(seed: Int): IntArray {
        val h = mix(seed)
        val hue = (h and 0xFFFF) % 360f
        val hue2 = (hue + 28f + ((h ushr 16) and 0x1F)) % 360f
        return intArrayOf(
            ColorMath.hslToRgb(hue, 0.42f, 0.42f),
            ColorMath.hslToRgb(hue2, 0.48f, 0.26f)
        )
    }

    /** The "seed color" of the placeholder, for ThemeController.setFromSeedColor. */
    @JvmStatic
    fun seedColor(seed: Int): Int {
        return colors(seed)[0]
    }

    @JvmStatic
    fun mix(initialX: Int): Int {
        var x = initialX
        x = x xor (x ushr 16)
        x *= 0x7feb352d
        x = x xor (x ushr 15)
        x *= -0x7b935975 // 0x846ca68b as signed Int
        x = x xor (x ushr 16)
        return x and 0x7FFFFFFF
    }
}

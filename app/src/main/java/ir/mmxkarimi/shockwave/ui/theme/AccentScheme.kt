package ir.mmxkarimi.shockwave.ui.theme

import kotlin.math.max
import kotlin.math.min

/**
 * Artwork-derived roles: primary/onPrimary/container/onContainer/glow.
 * Tones approximate M3 tones with HSL lightness: dark 80/20/30/90, light 40/100/90/10.
 */
data class AccentScheme(
    val primary: Int,
    val onPrimary: Int,
    val container: Int,
    val onContainer: Int,
    val glow: Int
) {
    companion object {
        /** Lapis default used when there is no artwork or "Colors from artwork" is off. */
        @JvmStatic
        fun base(dark: Boolean): AccentScheme {
            return if (dark) {
                AccentScheme(
                    primary = 0xFFA6C8FF.toInt(),
                    onPrimary = 0xFF00315F.toInt(),
                    container = 0xFF1E4878.toInt(),
                    onContainer = 0xFFD4E3FF.toInt(),
                    glow = 0xFF1E4878.toInt()
                )
            } else {
                AccentScheme(
                    primary = 0xFF2F5F95.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    container = 0xFFD4E3FF.toInt(),
                    onContainer = 0xFF001C3A.toInt(),
                    glow = 0xFFD4E3FF.toInt()
                )
            }
        }

        @JvmStatic
        fun fromSeed(seed: Int, dark: Boolean): AccentScheme {
            val hsl = FloatArray(3)
            ColorMath.rgbToHsl(seed, hsl)
            var h = hsl[0]
            // Keep fidelity to the cover's hue but tame saturation so text stays calm; greys get a hint of lapis.
            val s = if (hsl[1] < 0.08f) 0.18f else max(0.35f, min(0.80f, hsl[1]))
            if (hsl[1] < 0.08f) h = 212f
            if (dark) {
                val onPrimary = ColorMath.hslToRgb(h, s, 0.16f)
                val onContainer = ColorMath.hslToRgb(h, s, 0.91f)
                return AccentScheme(
                    primary = guard(h, s, 0.80f, onPrimary, +1),
                    onPrimary = onPrimary,
                    container = guard(h, s * 0.85f, 0.27f, onContainer, -1),
                    onContainer = onContainer,
                    glow = ColorMath.hslToRgb(h, s, 0.35f)
                )
            }
            val onContainer = ColorMath.hslToRgb(h, s, 0.12f)
            return AccentScheme(
                primary = guard(h, s, 0.36f, 0xFFFFFFFF.toInt(), -1),
                onPrimary = 0xFFFFFFFF.toInt(),
                container = guard(h, s, 0.90f, onContainer, +1),
                onContainer = onContainer,
                glow = ColorMath.hslToRgb(h, s, 0.82f)
            )
        }

        /**
         * Contrast guard: move lightness away from [against] ([dir] +1 lighter / -1 darker) until text on it reaches 4.5:1.
         */
        @JvmStatic
        fun guard(h: Float, s: Float, initialL: Float, against: Int, dir: Int): Int {
            var l = initialL
            var c = ColorMath.hslToRgb(h, s, l)
            var i = 0
            while (i < 40 && ColorMath.contrast(c, against) < 4.5) {
                l = ColorMath.clamp01(l + dir * 0.02f)
                c = ColorMath.hslToRgb(h, s, l)
                i++
            }
            return c
        }

        @JvmStatic
        fun lerp(from: AccentScheme, to: AccentScheme, t: Float): AccentScheme {
            return AccentScheme(
                primary = ColorMath.lerp(from.primary, to.primary, t),
                onPrimary = ColorMath.lerp(from.onPrimary, to.onPrimary, t),
                container = ColorMath.lerp(from.container, to.container, t),
                onContainer = ColorMath.lerp(from.onContainer, to.onContainer, t),
                glow = ColorMath.lerp(from.glow, to.glow, t)
            )
        }
    }
}

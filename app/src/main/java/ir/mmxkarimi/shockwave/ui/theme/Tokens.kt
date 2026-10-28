package ir.mmxkarimi.shockwave.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import ir.mmxkarimi.shockwave.R

/** Design tokens as constants (dp / ms) plus theme-attribute helpers. Sizes mirror res/values/dimens.xml. */
object Tokens {
    // Shape (dp)
    const val RADIUS_ROW_ART: Float = 12f
    const val RADIUS_GROUP_OUTER: Float = 22f
    const val RADIUS_GROUP_INNER: Float = 6f
    const val RADIUS_CARD: Float = 24f
    const val RADIUS_HERO: Float = 28f
    const val RADIUS_PLAYER_COVER: Float = 32f
    const val RADIUS_MINI_PLAYER: Float = 24f
    const val RADIUS_CHIP: Float = 12f
    const val TOUCH_TARGET: Float = 48f

    // Material 3 Expressive Radii (dp)
    const val RADIUS_EXPRESSIVE_HERO: Float = 28f
    const val RADIUS_EXPRESSIVE_SHEET: Float = 28f
    const val RADIUS_EXPRESSIVE_GROUP: Float = 24f
    const val RADIUS_EXPRESSIVE_CARD: Float = 20f
    const val RADIUS_EXPRESSIVE_ITEM: Float = 14f
    const val RADIUS_EXPRESSIVE_PILL: Float = 50f

    // Spring Motion
    const val SPRING_DAMPING_BOUNCY: Float = 0.7f
    const val SPRING_STIFFNESS_LOW: Float = 300f
    const val SPRING_STIFFNESS_MEDIUM: Float = 600f

    // Seek bar
    const val WAVE_AMPLITUDE: Float = 3f
    const val WAVE_LENGTH: Float = 26f
    const val WAVE_PERIOD_MS: Long = 1600L
    const val WAVE_SETTLE_MS: Long = 300L
    const val SEEK_STEP_MS: Long = 10_000L

    // Motion (ms)
    const val MOTION_FAST: Long = 150L
    const val MOTION_MORPH: Long = 350L
    const val MOTION_COLOR: Long = 600L
    const val MOTION_COLOR_REDUCED: Long = 150L
    /** Decorative frame interval: 30 fps is enough for slow waves and halves the GPU work of 60 fps. */
    const val DECOR_FRAME_MS: Long = 33L

    @JvmStatic
    fun dp(c: Context, dp: Float): Float {
        return dp * c.resources.displayMetrics.density
    }

    @JvmStatic
    fun sp(c: Context, sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, c.resources.displayMetrics)
    }

    @ColorInt
    @JvmStatic
    fun color(c: Context, @AttrRes attr: Int): Int {
        val tv = TypedValue()
        if (c.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                try {
                    return c.getColor(tv.resourceId)
                } catch (ignored: Resources.NotFoundException) {
                    // fall through
                }
            }
            return tv.data
        }
        return 0xFFFF00FF.toInt()
    }

    @JvmStatic
    fun onSurface(c: Context): Int = color(c, R.attr.mpOnSurface)

    @JvmStatic
    fun onSurfaceVariant(c: Context): Int = color(c, R.attr.mpOnSurfaceVariant)

    @JvmStatic
    fun primary(c: Context): Int = color(c, R.attr.mpPrimary)

    @JvmStatic
    fun outlineVariant(c: Context): Int = color(c, R.attr.mpOutlineVariant)

    @JvmStatic
    fun isDark(c: Context): Boolean {
        return (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

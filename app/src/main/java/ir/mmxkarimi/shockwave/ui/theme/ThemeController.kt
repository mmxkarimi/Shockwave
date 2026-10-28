package ir.mmxkarimi.shockwave.ui.theme

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.animation.PathInterpolator
import androidx.annotation.MainThread
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.R

/**
 * App-wide theme state: theme mode (System/Light/Dark/Black) and the artwork-derived [AccentScheme].
 */
class ThemeController private constructor(context: Context) {
    private val app: Context = context.applicationContext
    private val accentLiveData = MutableLiveData<AccentScheme>()
    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private var from: AccentScheme
    private var to: AccentScheme
    private var artworkColors = true
    private var dark: Boolean
    private var lastSeed: Int? = null
    private var currentKey: String? = null

    init {
        dark = Tokens.isDark(app)
        val base = AccentScheme.base(dark)
        accentLiveData.value = base
        from = base
        to = base
        animator.interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        animator.addUpdateListener { a ->
            accentLiveData.value = AccentScheme.lerp(from, to, a.animatedValue as Float)
        }
    }

    /** Current accent roles; emits interpolated values during the crossfade. */
    fun accent(): LiveData<AccentScheme> = accentLiveData

    fun currentAccent(): AccentScheme {
        return accentLiveData.value ?: AccentScheme.base(Tokens.isDark(app))
    }

    /** Settings: "Colors from artwork". Off reverts to the base scheme. */
    @MainThread
    fun setArtworkColorsEnabled(enabled: Boolean) {
        artworkColors = enabled
        if (!enabled) animateTo(AccentScheme.base(Tokens.isDark(app)))
    }

    /** Extracts the seed from [artwork] (any size; downscaled to 32x32). [key] dedupes repeats. */
    @MainThread
    fun setFromArtwork(artwork: Bitmap?, key: String?) {
        if (key != null && key == currentKey) return
        currentKey = key
        if (!artworkColors || artwork == null) {
            lastSeed = null
            animateTo(AccentScheme.base(Tokens.isDark(app)))
            return
        }
        setSeed(seedOf(artwork))
    }

    /** For tracks without artwork: derive the accent from the placeholder gradient color. */
    @MainThread
    fun setFromSeedColor(seedColor: Int, key: String?) {
        if (key != null && key == currentKey) return
        currentKey = key
        if (!artworkColors) return
        setSeed(seedColor)
    }

    private fun setSeed(seed: Int) {
        lastSeed = seed
        animateTo(AccentScheme.fromSeed(seed, Tokens.isDark(app)))
    }

    /** Light/dark switched (theme setting or system): re-derive the roles from the same seed, no animation. */
    private fun checkUiMode(context: Context) {
        val nowDark = Tokens.isDark(context)
        if (nowDark == dark) return
        dark = nowDark
        animator.cancel()
        val s = if (lastSeed != null && artworkColors) {
            AccentScheme.fromSeed(lastSeed!!, nowDark)
        } else {
            AccentScheme.base(nowDark)
        }
        from = s
        to = s
        accentLiveData.value = s
    }

    private fun animateTo(target: AccentScheme) {
        animator.cancel()
        from = currentAccent()
        to = target
        animator.duration = if (MotionPolicy.decorativeMotionEnabled(app)) {
            Tokens.MOTION_COLOR
        } else {
            Tokens.MOTION_COLOR_REDUCED
        }
        animator.start()
    }

    companion object {
        @Volatile
        private var instance: ThemeController? = null

        @JvmStatic
        @MainThread
        fun get(context: Context): ThemeController {
            val inst = instance
            if (inst == null) {
                return ThemeController(context).also { instance = it }
            } else {
                inst.checkUiMode(context)
                return inst
            }
        }

        /** Seed color of a bitmap; safe to call from a background thread. */
        @JvmStatic
        fun seedOf(artwork: Bitmap): Int {
            val small = if (artwork.width <= 32 && artwork.height <= 32) {
                artwork
            } else {
                Bitmap.createScaledBitmap(artwork, 32, 32, true)
            }
            val w = small.width
            val h = small.height
            val px = IntArray(w * h)
            small.getPixels(px, 0, w, 0, 0, w, h)
            if (small != artwork) small.recycle()
            return ColorExtractor.extractSeed(px, px.size)
        }

        /** Call from Application.onCreate and when the setting changes. */
        @JvmStatic
        fun applyMode(mode: ThemeMode) {
            val night = when (mode) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK, ThemeMode.BLACK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(night)
        }

        /** Theme to pass to [Activity.setTheme] before `super.onCreate`. */
        @JvmStatic
        @StyleRes
        fun themeResFor(mode: ThemeMode): Int {
            return if (mode == ThemeMode.BLACK) R.style.Theme_Shockwave_Black else R.style.Theme_Shockwave
        }
    }
}

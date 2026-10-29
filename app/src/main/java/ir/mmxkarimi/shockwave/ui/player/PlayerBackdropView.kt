package ir.mmxkarimi.shockwave.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Animated backdrop view that draws a rich ambient gradient / glow reflecting the current track's artwork palette.
 */
class PlayerBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isLight = false
    private var palette: PlayerPalette = PlayerPalette.forSeed(0)
    private var tinyArt: Bitmap? = null
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setLight(light: Boolean) {
        if (this.isLight != light) {
            this.isLight = light
            invalidate()
        }
    }

    fun setContent(palette: PlayerPalette, tinyArt: Bitmap?, animated: Boolean) {
        this.palette = palette
        this.tinyArt = tinyArt
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val baseColor = if (isLight) 0xFFF7F9FF.toInt() else 0xFF0F1216.toInt()
        val glowColor = if (isLight) {
            Color.argb(0x28, Color.red(palette.glow), Color.green(palette.glow), Color.blue(palette.glow))
        } else {
            Color.argb(0x35, Color.red(palette.primary), Color.green(palette.primary), Color.blue(palette.primary))
        }

        // Base gradient
        val shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(glowColor, baseColor),
            floatArrayOf(0f, 0.7f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Radial ambient glow near top-center
        val radial = RadialGradient(
            w * 0.5f, h * 0.35f, w * 0.65f,
            intArrayOf(glowColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        glowPaint.shader = radial
        canvas.drawCircle(w * 0.5f, h * 0.35f, w * 0.65f, glowPaint)
    }

    companion object {
        const val TEXT_ON_LIGHT: Int = 0xFF141B24.toInt()
        const val TEXT_ON_DARK: Int = 0xFFF0F4F9.toInt()

        @JvmStatic
        fun prepare(cover: Bitmap): Bitmap? {
            return try {
                if (cover.width <= 32 && cover.height <= 32) cover
                else Bitmap.createScaledBitmap(cover, 32, 32, true)
            } catch (e: Exception) {
                null
            }
        }
    }
}

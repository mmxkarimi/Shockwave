package ir.mmxkarimi.shockwave.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Animated Play/Pause button with smooth icon morphing, circular background, and buffering state.
 */
class MorphPlayPauseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bgColor = 0xFFC6F135.toInt()
    private var fgColor = 0xFF283500.toInt()
    private var isPlaying = false
    private var isBuffering = false

    // 0f = play (triangle), 1f = pause (two bars)
    private var morphFraction = 0f
    private var morphAnimator: ValueAnimator? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        isClickable = true
        isFocusable = true
    }

    fun setColors(background: Int, foreground: Int) {
        if (bgColor != background || fgColor != foreground) {
            bgColor = background
            fgColor = foreground
            invalidate()
        }
    }

    fun setPlaying(playing: Boolean, animated: Boolean) {
        if (this.isPlaying == playing) return
        this.isPlaying = playing
        val target = if (playing) 1f else 0f
        if (!animated) {
            morphFraction = target
            invalidate()
            return
        }
        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofFloat(morphFraction, target).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                morphFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setBuffering(buffering: Boolean) {
        if (this.isBuffering != buffering) {
            this.isBuffering = buffering
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val radius = Math.min(cx, cy) * 0.95f

        // Draw background cookie/circle
        bgPaint.color = bgColor
        bgPaint.alpha = if (isEnabled) 255 else 100
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Draw play / pause morph
        iconPaint.color = fgColor
        iconPaint.alpha = if (isEnabled) 255 else 100
        val size = radius * 0.55f

        if (morphFraction >= 0.99f) {
            // Pause: two rounded bars
            val barW = size * 0.28f
            val barH = size * 1.1f
            val gap = size * 0.32f
            val r = barW * 0.45f

            val leftX1 = cx - gap / 2f - barW
            val leftX2 = cx - gap / 2f
            val rightX1 = cx + gap / 2f
            val rightX2 = cx + gap / 2f + barW
            val topY = cy - barH / 2f
            val bottomY = cy + barH / 2f

            canvas.drawRoundRect(leftX1, topY, leftX2, bottomY, r, r, iconPaint)
            canvas.drawRoundRect(rightX1, topY, rightX2, bottomY, r, r, iconPaint)
        } else if (morphFraction <= 0.01f) {
            // Play: triangle offset slightly right for optical balance
            val shift = size * 0.1f
            path.reset()
            path.moveTo(cx - size * 0.42f + shift, cy - size * 0.65f)
            path.lineTo(cx + size * 0.65f + shift, cy)
            path.lineTo(cx - size * 0.42f + shift, cy + size * 0.65f)
            path.close()
            canvas.drawPath(path, iconPaint)
        } else {
            // Interpolating between play and pause
            val f = morphFraction
            val shift = (size * 0.1f) * (1f - f)
            val barW = size * 0.28f * f
            val barH = size * (1.3f - 0.2f * f)
            val topY = cy - barH / 2f
            val bottomY = cy + barH / 2f

            path.reset()
            path.moveTo(cx - size * 0.42f + shift, cy - size * 0.65f)
            path.lineTo(cx + size * 0.65f * (1f - f) + (cx + size * 0.35f) * f + shift, cy)
            path.lineTo(cx - size * 0.42f + shift, cy + size * 0.65f)
            path.close()
            canvas.drawPath(path, iconPaint)
        }
    }
}

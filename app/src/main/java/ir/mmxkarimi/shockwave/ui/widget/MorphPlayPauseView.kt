package ir.mmxkarimi.shockwave.ui.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Animated Play/Pause button with:
 * - On pause: a pure circle background and Play triangle icon (▶).
 * - On play: a continuously spinning 10-lobe cookie background (via [ShapeMath]) and Pause bars icon (||).
 * - Ultra-smooth geometric morphing between Play and Pause without distortion.
 */
class MorphPlayPauseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bgColor: Int = 0xFFC6F135.toInt()
    private var fgColor: Int = 0xFF283500.toInt()
    private var isPlaying: Boolean = false
    private var isBuffering: Boolean = false

    // 0f = paused (circle, play icon), 1f = playing (10-lobe cookie, pause icon)
    private var morphFraction: Float = 0f
    private var morphAnimator: ValueAnimator? = null

    // Spinning angle (0..360) for the 10-lobe cookie while playing
    private var spinAngle: Float = 0f
    private var spinAnimator: ValueAnimator? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bufferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val cookiePath = Path()
    private val leftPath = Path()
    private val rightPath = Path()
    private val playPath = Path()

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
        val target = if (playing) 1f else 0f
        if (this.isPlaying == playing && morphFraction == target) return
        if (this.isPlaying == playing && morphAnimator?.isRunning == true) return

        this.isPlaying = playing

        if (!animated) {
            morphAnimator?.cancel()
            morphFraction = target
            if (playing) {
                startSpinning()
            } else {
                stopSpinning()
            }
            invalidate()
            return
        }

        morphAnimator?.cancel()
        morphAnimator = ValueAnimator.ofFloat(morphFraction, target).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                morphFraction = it.animatedValue as Float
                if (!isPlaying && morphFraction <= 0.001f) {
                    stopSpinning()
                }
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isPlaying) {
                        stopSpinning()
                        invalidate()
                    }
                }
            })
            start()
        }

        if (playing) {
            startSpinning()
        }
    }

    private fun startSpinning() {
        if (spinAnimator?.isRunning == true) return
        spinAnimator?.cancel()
        spinAnimator = ValueAnimator.ofFloat(spinAngle, spinAngle + 360f).apply {
            duration = 4500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                spinAngle = (it.animatedValue as Float) % 360f
                if (morphFraction > 0.001f) {
                    invalidate()
                }
            }
            start()
        }
    }

    private fun stopSpinning() {
        spinAnimator?.cancel()
        spinAnimator = null
        spinAngle = 0f
    }

    fun setBuffering(buffering: Boolean) {
        if (this.isBuffering != buffering) {
            this.isBuffering = buffering
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isPlaying && morphFraction > 0.001f) {
            startSpinning()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        morphAnimator?.cancel()
        spinAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val radius = min(cx, cy) * 0.95f

        bgPaint.color = bgColor
        bgPaint.alpha = if (isEnabled) 255 else 100

        // 1. Draw background: circle on pause, 10-lobe cookie on play
        if (morphFraction <= 0.001f) {
            canvas.drawCircle(cx, cy, radius, bgPaint)
        } else {
            canvas.save()
            canvas.rotate(spinAngle, cx, cy)
            cookiePath.reset()
            val points = 120
            for (i in 0 until points) {
                val theta = (i * 2.0 * PI) / points
                val r = radius * ShapeMath.radius(theta, morphFraction)
                val px = cx + r * cos(theta).toFloat()
                val py = cy + r * sin(theta).toFloat()
                if (i == 0) cookiePath.moveTo(px, py) else cookiePath.lineTo(px, py)
            }
            cookiePath.close()
            canvas.drawPath(cookiePath, bgPaint)
            canvas.restore()
        }

        // 2. Buffering ring indicator if active
        if (isBuffering) {
            bufferPaint.color = fgColor
            bufferPaint.strokeWidth = 3f * resources.displayMetrics.density
            bufferPaint.alpha = 180
            canvas.drawCircle(cx, cy, radius * 0.96f, bufferPaint)
        }

        // 3. Smooth icon morph: Play triangle (f = 0) <-> Pause two bars (f = 1)
        val f = morphFraction
        iconPaint.color = fgColor
        iconPaint.alpha = if (isEnabled) 255 else 100

        val size = radius * 0.44f
        val W = size * 1.05f
        val H = size * 1.15f
        val gap = size * 0.35f
        val barW = size * 0.28f

        // Pause bars geometry
        val pauseL1 = cx - gap / 2f - barW
        val pauseL2 = cx - gap / 2f
        val pauseR1 = cx + gap / 2f
        val pauseR2 = cx + gap / 2f + barW

        // Play triangle geometry (slightly shifted right for optical balance)
        val shift = size * 0.08f
        val xLeft = cx - W * 0.45f + shift
        val xTip = cx + W * 0.55f + shift
        val xCut = xLeft + barW * 1.2f
        val hCut = (H / 2f) * (xTip - xCut) / (xTip - xLeft)

        if (f <= 0.001f) {
            // Crisp Play triangle with rounded corners
            val cornerR = 3.5f * resources.displayMetrics.density
            iconPaint.pathEffect = CornerPathEffect(cornerR)
            playPath.reset()
            playPath.moveTo(xLeft, cy - H / 2f)
            playPath.lineTo(xTip, cy)
            playPath.lineTo(xLeft, cy + H / 2f)
            playPath.close()
            canvas.drawPath(playPath, iconPaint)
        } else if (f >= 0.999f) {
            // Crisp Pause bars with rounded corners
            iconPaint.pathEffect = null
            val barCornerR = barW * 0.45f
            canvas.drawRoundRect(pauseL1, cy - H / 2f, pauseL2, cy + H / 2f, barCornerR, barCornerR, iconPaint)
            canvas.drawRoundRect(pauseR1, cy - H / 2f, pauseR2, cy + H / 2f, barCornerR, barCornerR, iconPaint)
        } else {
            // Smooth morph between triangle and two bars
            val cornerR = (barW * 0.45f) * f
            iconPaint.pathEffect = if (cornerR > 0.5f) CornerPathEffect(cornerR) else null

            // Left piece
            leftPath.reset()
            val p1x = lerp(xLeft, pauseL1, f)
            val p1y = lerp(cy - H / 2f, cy - H / 2f, f)
            val p2x = lerp(xCut, pauseL2, f)
            val p2y = lerp(cy - hCut, cy - H / 2f, f)
            val p3x = lerp(xCut, pauseL2, f)
            val p3y = lerp(cy + hCut, cy + H / 2f, f)
            val p4x = lerp(xLeft, pauseL1, f)
            val p4y = lerp(cy + H / 2f, cy + H / 2f, f)

            leftPath.moveTo(p1x, p1y)
            leftPath.lineTo(p2x, p2y)
            leftPath.lineTo(p3x, p3y)
            leftPath.lineTo(p4x, p4y)
            leftPath.close()
            canvas.drawPath(leftPath, iconPaint)

            // Right piece
            rightPath.reset()
            val q1x = lerp(xCut, pauseR1, f)
            val q1y = lerp(cy - hCut, cy - H / 2f, f)
            val q2x = lerp(xTip, pauseR2, f)
            val q2y = lerp(cy, cy - H / 2f, f)
            val q3x = lerp(xTip, pauseR2, f)
            val q3y = lerp(cy, cy + H / 2f, f)
            val q4x = lerp(xCut, pauseR1, f)
            val q4y = lerp(cy + hCut, cy + H / 2f, f)

            rightPath.moveTo(q1x, q1y)
            rightPath.lineTo(q2x, q2y)
            rightPath.lineTo(q3x, q3y)
            rightPath.lineTo(q4x, q4y)
            rightPath.close()
            canvas.drawPath(rightPath, iconPaint)
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

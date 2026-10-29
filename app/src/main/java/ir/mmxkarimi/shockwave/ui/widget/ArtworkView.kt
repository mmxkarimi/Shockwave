package ir.mmxkarimi.shockwave.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * Custom artwork view with rounded corners that asynchronously loads and caches track album art.
 */
class ArtworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cornerRadiusPx = 32f * resources.displayMetrics.density
    private var listener: ((Bitmap?) -> Unit)? = null
    private var bitmap: Bitmap? = null
    private var seed: Int = 0

    private val clipPath = Path()
    private val rectF = RectF()
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setCornerRadiusDp(dp: Int) {
        cornerRadiusPx = dp * resources.displayMetrics.density
        invalidate()
    }

    fun setListener(listener: ((Bitmap?) -> Unit)?) {
        this.listener = listener
        if (bitmap != null) {
            listener?.invoke(bitmap)
        }
    }

    fun bind(track: Track) {
        this.seed = track.placeholderSeed
        val uri = track.artworkUri
        if (uri != null) {
            val cached = ArtworkLoader.get(context).cached(uri, 512)
            if (cached != null) {
                this.bitmap = cached
                listener?.invoke(cached)
                invalidate()
                return
            }
            this.bitmap = null
            invalidate()
            ArtworkLoader.get(context).load(uri, 512) { _, bmp ->
                this.bitmap = bmp
                listener?.invoke(bmp)
                invalidate()
            }
        } else {
            this.bitmap = null
            listener?.invoke(null)
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipPath.reset()
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.addRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.save()
        canvas.clipPath(clipPath)

        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            srcRect.set(0, 0, bmp.width, bmp.height)
            dstRect.set(0f, 0f, w, h)
            canvas.drawBitmap(bmp, srcRect, dstRect, paint)
        } else {
            // Draw placeholder gradient
            val colors = PlaceholderArt.colors(seed)
            gradPaint.shader = LinearGradient(
                0f, 0f, w, h,
                colors[0], colors[1],
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, gradPaint)
        }

        canvas.restore()
    }
}

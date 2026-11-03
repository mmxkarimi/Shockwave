package ir.mmxkarimi.shockwave.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.media3.common.C
import androidx.media3.common.Player
import ir.mmxkarimi.shockwave.playback.PlayerHolder
import ir.mmxkarimi.shockwave.ui.widget.ArtworkLoader
import kotlin.coroutines.resume
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Everything the home-screen widget draws, read once per update from the process-wide player. */
internal data class WidgetSnapshot(
    val hasTrack: Boolean,
    val title: String,
    val artist: String,
    val playing: Boolean,
    /** 0..1; only refreshed when the player state changes, not every second. */
    val progress: Float,
    /** Square, pre-rounded cover; null for tracks without artwork (the widget draws the note placeholder). */
    val artwork: Bitmap?,
)

internal object WidgetSnapshotReader {
    /** RemoteViews carry bitmaps through binder: keep the cover small. */
    private const val ARTWORK_PX = 256
    private const val CORNER_RATIO = 0.22f

    private var cachedUri: String? = null
    private var cachedBitmap: Bitmap? = null

    suspend fun read(context: Context): WidgetSnapshot {
        val app = context.applicationContext
        // The player lives on the main thread.
        val raw = withContext(Dispatchers.Main) {
            val player = PlayerHolder.get(app)
            val metadata = player.mediaMetadata
            val has = player.mediaItemCount > 0 && player.currentMediaItem != null
            var duration = player.duration
            if (duration == C.TIME_UNSET || duration <= 0) duration = metadata.durationMs ?: 0L
            val position = player.currentPosition.coerceAtLeast(0L)
            Raw(
                hasTrack = has,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                playing = player.isPlaying ||
                    (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
                progress = if (duration > 0) (position.coerceAtMost(duration).toFloat() / duration) else 0f,
                artworkUri = metadata.artworkUri?.toString(),
            )
        }
        val artwork = raw.artworkUri?.let { loadArtwork(app, it) }
        return WidgetSnapshot(raw.hasTrack, raw.title, raw.artist, raw.playing, raw.progress, artwork)
    }

    private class Raw(
        val hasTrack: Boolean,
        val title: String,
        val artist: String,
        val playing: Boolean,
        val progress: Float,
        val artworkUri: String?,
    )

    private suspend fun loadArtwork(context: Context, uri: String): Bitmap? {
        synchronized(this) {
            if (cachedUri == uri) cachedBitmap?.let { return it }
        }
        val source = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Bitmap?> { cont ->
                ArtworkLoader.get(context).load(uri, ARTWORK_PX) { _, bitmap ->
                    if (cont.isActive) cont.resume(bitmap)
                }
            }
        } ?: return null
        val rounded = withContext(Dispatchers.Default) { roundedSquare(source, ARTWORK_PX) }
        synchronized(this) {
            cachedUri = uri
            cachedBitmap = rounded
        }
        return rounded
    }

    /**
     * Centre-crops [src] to a square of [sizePx] and rounds its corners proportionally, so the cover is rounded on
     * every Android version (Glance's cornerRadius on images only works on Android 12+).
     */
    private fun roundedSquare(src: Bitmap, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val side = min(src.width, src.height)
        val left = (src.width - side) / 2f
        val top = (src.height - side) / 2f
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix()
        matrix.setScale(sizePx.toFloat() / side, sizePx.toFloat() / side)
        matrix.preTranslate(-left, -top)
        shader.setLocalMatrix(matrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = shader
        val radius = sizePx * CORNER_RATIO
        Canvas(out).drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, paint)
        return out
    }
}

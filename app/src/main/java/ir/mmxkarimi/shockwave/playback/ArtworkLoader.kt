package ir.mmxkarimi.shockwave.playback

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Notification / lock-screen artwork loader that decodes straight down to at most [MAX_PX] px in RGB_565
 * (the stock loader decodes full size). Local sources only (content, file, resource); anything else fails the
 * future, which Media3 treats as "no artwork". One worker thread that exits after 10 s idle.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class ArtworkLoader(context: Context) : BitmapLoader {
    private val resolver: ContentResolver = context.getContentResolver()
    private val executor: ListeningExecutorService

    init {
        val pool = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue())
        pool.allowCoreThreadTimeOut(true)
        executor = MoreExecutors.listeningDecorator(pool)
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return executor.submit(Callable {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions(bounds))
            if (bitmap == null) throw IOException("Undecodable artwork")
            bitmap
        })
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return executor.submit(Callable {
            val scheme = uri.getScheme()
            if (ContentResolver.SCHEME_CONTENT != scheme && ContentResolver.SCHEME_FILE != scheme
                    && ContentResolver.SCHEME_ANDROID_RESOURCE != scheme) {
                throw IOException("Unsupported artwork scheme: $scheme")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val uriStr = uri.toString()
                    val albumId = uri.lastPathSegment?.toLongOrNull()
                    val targetUri = if (albumId != null && albumId > 0 && uriStr.contains("albumart")) {
                        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                    } else {
                        uri
                    }
                    return@Callable resolver.loadThumbnail(targetUri, Size(MAX_PX, MAX_PX), null)
                } catch (ignored: Exception) {
                }
            }
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            val bitmap = resolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, decodeOptions(bounds))
            }
            if (bitmap == null) throw IOException("Undecodable artwork: $uri")
            bitmap
        })
    }

    companion object {
        internal const val MAX_PX = 512

        /** Largest power-of-two sample size that keeps both sides at or above `maxPx` (1 for small images). */
        @JvmStatic
        @JvmName("sampleSize")
        internal fun sampleSize(width: Int, height: Int, maxPx: Int): Int {
            var sample = 1
            while (width / (sample * 2) >= maxPx && height / (sample * 2) >= maxPx) sample *= 2
            return sample
        }

        @JvmStatic
        private fun decodeOptions(bounds: BitmapFactory.Options): BitmapFactory.Options {
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PX)
            options.inPreferredConfig = Bitmap.Config.RGB_565
            return options
        }
    }
}

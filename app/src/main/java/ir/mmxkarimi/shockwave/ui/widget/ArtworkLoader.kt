package ir.mmxkarimi.shockwave.ui.widget

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import androidx.annotation.MainThread
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * Tiny artwork loader: async decode with inSampleSize downsampling and memory LruCache.
 */
class ArtworkLoader private constructor(context: Context) {

    fun interface Callback {
        fun onLoaded(key: String, bitmap: Bitmap?)
    }

    private val resolver: ContentResolver = context.applicationContext.contentResolver
    private val maxCache = min(16L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 16).toInt()
    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxCache) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount
        }
    }
    private val executor: ExecutorService = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "artwork").apply { priority = Thread.MIN_PRIORITY }
    }
    private val main = Handler(Looper.getMainLooper())

    fun cached(uri: String, sizePx: Int): Bitmap? {
        return cache.get(key(uri, sizePx))
    }

    /** Loads [uri] scaled so its shorter side is about [sizePx]. */
    @MainThread
    fun load(uri: String, sizePx: Int, callback: Callback) {
        val k = key(uri, sizePx)
        val hit = cache.get(k)
        if (hit != null) {
            callback.onLoaded(k, hit)
            return
        }
        executor.execute {
            val bmp = decode(uri, sizePx)
            main.post {
                if (bmp != null) cache.put(k, bmp)
                callback.onLoaded(k, bmp)
            }
        }
    }

    /** Drop everything (call from onTrimMemory at RUNNING_LOW or worse). */
    fun trim() {
        cache.evictAll()
    }

    private fun decode(uri: String, sizePx: Int): Bitmap? {
        return try {
            val u = Uri.parse(uri)
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(u)?.use { `in` ->
                BitmapFactory.decodeStream(`in`, null, o)
            } ?: return null

            o.inSampleSize = sampleSize(o.outWidth, o.outHeight, sizePx)
            o.inJustDecodeBounds = false
            o.inPreferredConfig = if (sizePx <= 160) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
            resolver.openInputStream(u)?.use { `in` ->
                BitmapFactory.decodeStream(`in`, null, o)
            }
        } catch (e: Exception) {
            null
        } catch (oom: OutOfMemoryError) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: ArtworkLoader? = null

        @JvmStatic
        @MainThread
        fun get(context: Context): ArtworkLoader {
            return instance ?: synchronized(this) {
                instance ?: ArtworkLoader(context).also { instance = it }
            }
        }

        @JvmStatic
        fun key(uri: String, sizePx: Int): String {
            return "$uri#$sizePx"
        }

        /** Largest power of two that keeps the shorter side at or above [target]. */
        @JvmStatic
        fun sampleSize(w: Int, h: Int, target: Int): Int {
            var s = 1
            if (w <= 0 || h <= 0 || target <= 0) return 1
            val shorter = min(w, h)
            while (shorter / (s * 2) >= target) s *= 2
            return s
        }
    }
}

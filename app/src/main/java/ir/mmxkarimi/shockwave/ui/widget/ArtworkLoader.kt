package ir.mmxkarimi.shockwave.ui.widget

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
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

    /** URIs known to have no cover (main thread only), so list scrolling does not probe them again. */
    private val noArt = HashSet<String>()
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(String) -> Unit>()

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
        if (noArt.contains(uri)) {
            callback.onLoaded(k, null)
            return
        }
        executor.execute {
            val bmp = decode(uri, sizePx)
            main.post {
                if (bmp != null) cache.put(k, bmp) else noArt.add(uri)
                callback.onLoaded(k, bmp)
            }
        }
    }

    /**
     * Forgets every cached size of [uri] (after its track was deleted or its file changed), so the next load decodes
     * again instead of serving a stale cover.
     */
    @MainThread
    fun evict(uri: String) {
        val prefix = "$uri#"
        for (k in cache.snapshot().keys) {
            if (k.startsWith(prefix)) cache.remove(k)
        }
        noArt.remove(uri)
        listeners.forEach { it(uri) }
    }

    /** Called with the uri on every [evict] (the widget drops its own rounded copy). */
    fun addEvictionListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    /** Drop everything (call from onTrimMemory at RUNNING_LOW or worse). */
    fun trim() {
        cache.evictAll()
        noArt.clear()
    }

    private fun decode(uri: String, sizePx: Int): Bitmap? {
        return try {
            val u = Uri.parse(uri)
            if (isAudioItem(uri)) return decodeEmbedded(u, sizePx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val albumId = u.lastPathSegment?.toLongOrNull()
                    val targetUri = if (albumId != null && albumId > 0 && uri.contains("albumart")) {
                        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                    } else {
                        u
                    }
                    return resolver.loadThumbnail(targetUri, Size(sizePx, sizePx), null)
                } catch (ignored: Exception) {
                }
            }

            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(u)?.use { `in` ->
                BitmapFactory.decodeStream(`in`, null, o)
            } ?: return null
            // Not an image: an audio file opened from another app; use the cover embedded in it.
            if (o.outWidth <= 0 || o.outHeight <= 0) return decodeEmbedded(u, sizePx)

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

    /**
     * The cover embedded in one audio file: MediaStore's thumbnail of that item, else the raw picture tag. Never the
     * album's shared thumbnail, which MediaStore builds from whichever file of the album it saw first.
     */
    private fun decodeEmbedded(u: Uri, sizePx: Int): Bitmap? {
        try {
            return resolver.loadThumbnail(u, Size(sizePx, sizePx), null)
        } catch (ignored: Exception) {
        }
        val bytes = try {
            val retriever = MediaMetadataRetriever()
            try {
                resolver.openFileDescriptor(u, "r")?.use { fd ->
                    retriever.setDataSource(fd.fileDescriptor)
                    retriever.embeddedPicture
                }
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            null
        } ?: return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        o.inSampleSize = sampleSize(o.outWidth, o.outHeight, sizePx)
        o.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
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

        /** `content://media/<volume>/audio/media/<id>`: a single track, whose own embedded cover is wanted. */
        @JvmStatic
        fun isAudioItem(uri: String): Boolean = uri.startsWith("content://media/") && uri.contains("/audio/media/")

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

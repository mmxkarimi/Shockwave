package ir.mmxkarimi.shockwave.data.waveform

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File
import java.util.function.Function

/**
 * `size:modified:duration` of the track's file, the cache validity key. Does I/O, so call it off the main
 * thread. Anything unreadable degrades to 0 for that part, never throws.
 */
class MediaFingerprint(context: Context) : Function<Track, String> {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override fun apply(track: Track): String {
        var size = 0L
        var modified = 0L
        val mediaUri = track.mediaUri
        if (mediaUri != null) {
            val uri = Uri.parse(mediaUri)
            val path = uri.path
            if ("file" == uri.scheme && path != null) {
                val f = File(path)
                size = f.length()
                modified = f.lastModified()
            } else {
                try {
                    val afd = resolver.openAssetFileDescriptor(uri, "r")
                    size = if (afd == null) 0L else afd.length
                    afd?.close()
                } catch (ignored: Exception) {
                    /* unreadable: size stays 0 */
                }
                try {
                    resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use { c ->
                        if (c.moveToFirst()) modified = c.getLong(0)
                    }
                } catch (ignored: Exception) {
                    /* not a MediaStore uri: modified stays 0 */
                }
            }
        }
        return "$size:$modified:${track.durationMs}"
    }
}

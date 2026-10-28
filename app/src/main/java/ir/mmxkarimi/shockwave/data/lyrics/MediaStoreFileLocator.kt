package ir.mmxkarimi.shockwave.data.lyrics

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File

/** [AudioFileLocator] that resolves `content://media/...` URIs to their file path via MediaStore. */
class MediaStoreFileLocator(private val resolver: ContentResolver) : AudioFileLocator {

    override fun locate(track: Track): File? {
        val uri = track.mediaUri
        if (uri == null || !uri.startsWith("content://")) return AudioFileLocator.FILE_ONLY.locate(track)
        // DATA is deprecated but is the only way to get a path for the sidecar lookup; null/unreadable = skip.
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
            if (cursor == null || !cursor.moveToFirst()) return null
            val path = cursor.getString(0)
            return if (path == null) null else File(path)
        } catch (e: RuntimeException) { // SecurityException, IllegalArgumentException: not accessible
            return null
        } finally {
            cursor?.close()
        }
    }
}

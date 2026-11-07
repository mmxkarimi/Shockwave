package ir.mmxkarimi.shockwave.playback

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.WorkerThread
import ir.mmxkarimi.shockwave.data.db.query.Seeds
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * Turns an audio file opened from another app (a file manager's "Open with", a download notification, a chat
 * attachment) into a playable [Track].
 *
 * When the file is also in MediaStore, the track gets the library's id (so likes and play counts line up with the
 * library entry) and plays through its MediaStore URI, which stays readable after the sender's temporary grant ends.
 * Otherwise it plays through the URI it was given, with title / artist / album read from the file's own tags.
 */
object ExternalAudio {
    private const val MEDIA_AUDIO_PREFIX = "content://media/external/audio/media/"

    /** True for an intent that hands us an audio file to play. */
    @JvmStatic
    fun isPlayIntent(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        val scheme = intent.data?.scheme ?: return false
        return scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE
    }

    /** Reads the file's metadata; null when the file cannot be opened at all. */
    @WorkerThread
    @JvmStatic
    fun resolve(context: Context, uri: Uri): Track? {
        val resolver = context.contentResolver
        keepAccess(resolver, uri)

        val mediaId = mediaStoreId(context, uri)
        val playUri = if (mediaId != null) Uri.parse(MEDIA_AUDIO_PREFIX + mediaId) else uri

        val retriever = MediaMetadataRetriever()
        try {
            try {
                if (uri.scheme == ContentResolver.SCHEME_FILE) {
                    retriever.setDataSource(uri.path)
                } else {
                    resolver.openFileDescriptor(uri, "r")?.use { fd -> retriever.setDataSource(fd.fileDescriptor) }
                        ?: return null
                }
            } catch (e: Exception) {
                return null // unreadable, or not a media file
            }
            val tagTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim()
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0
            val trackNo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
            val hasArt = retriever.embeddedPicture != null

            val title = tagTitle.takeUnless { it.isNullOrEmpty() } ?: fileTitle(context, uri) ?: "Unknown title"
            val artistName = artist.takeUnless { it.isNullOrEmpty() } ?: "Unknown artist"
            val albumTitle = album.takeUnless { it.isNullOrEmpty() } ?: "Unknown album"
            val id = mediaId?.toString() ?: ("ext:" + uri.toString().hashCode().toUInt().toString(16))
            val albumKey = "ext:" + albumTitle.lowercase()
            return Track(
                id, title, "ext:" + artistName.lowercase(), artistName, albumKey, albumTitle, duration,
                // The cover is read from the audio file itself (embedded picture).
                if (hasArt) playUri.toString() else null, Seeds.of(albumKey), playUri.toString(),
                false, false, year, trackNo, System.currentTimeMillis(),
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
            }
        }
    }

    /** Keeps reading the file after the sender's activity is gone, when the sender allows it. */
    private fun keepAccess(resolver: ContentResolver, uri: Uri) {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (ignored: Exception) {
            // Not persistable (most file managers): the temporary grant covers this session's playback.
        }
    }

    /** The MediaStore audio id of the same file, when MediaStore knows it. */
    private fun mediaStoreId(context: Context, uri: Uri): Long? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == MediaStore.AUTHORITY) {
            return if (uri.pathSegments.contains("audio")) runCatching { ContentUris.parseId(uri) }.getOrNull()
                ?.takeIf { it > 0 } else null
        }
        // Storage Access Framework documents (e.g. "Files" app) map straight to their MediaStore entry.
        try {
            val media = MediaStore.getMediaUri(context, uri)
            if (media != null && media.pathSegments.contains("audio")) {
                return ContentUris.parseId(media).takeIf { it > 0 }
            }
        } catch (ignored: Exception) {
        }
        // Otherwise match by file path (file:// URIs, file-manager providers that expose the real path).
        val path = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path
            else -> queryString(context, uri, MediaStore.MediaColumns.DATA)
        } ?: return null
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID),
                MediaStore.MediaColumns.DATA + " = ?", arrayOf(path), null
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    /** The file name without its extension, as a last-resort title. */
    private fun fileTitle(context: Context, uri: Uri): String? {
        val name = queryString(context, uri, OpenableColumns.DISPLAY_NAME) ?: uri.lastPathSegment ?: return null
        val base = name.substringAfterLast('/')
        val dot = base.lastIndexOf('.')
        return (if (dot > 0) base.substring(0, dot) else base).takeIf { it.isNotBlank() }
    }

    private fun queryString(context: Context, uri: Uri, column: String): String? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        return try {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                val i = c.getColumnIndex(column)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            }
        } catch (e: Exception) {
            null // providers may reject unknown columns
        }
    }
}

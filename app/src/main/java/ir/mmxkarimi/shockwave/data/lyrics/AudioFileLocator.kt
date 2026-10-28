package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/** Finds the audio file of a track on disk, or null when it has no accessible path (then file sources are skipped). */
fun interface AudioFileLocator {

    fun locate(track: Track): File?

    /** Handles `file://` URIs and absolute paths only; use [MediaStoreFileLocator] for content URIs. */
    companion object {
        @JvmField val FILE_ONLY: AudioFileLocator = AudioFileLocator { track ->
            val uri = track.mediaUri
            when {
                uri == null -> null
                uri.startsWith("file://") -> try {
                    File(URI(uri))
                } catch (e: URISyntaxException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                }
                uri.startsWith("/") -> File(uri)
                else -> null
            }
        }
    }
}

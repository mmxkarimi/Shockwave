package ir.mmxkarimi.shockwave.domain.model

import java.util.Collections

/**
 * A user playlist.
 *
 * @param artworkUris up to 4 artwork URIs for a mosaic cover; may be empty (use [placeholderSeed])
 */
data class Playlist(
    @get:JvmName("id") val id: String,
    @get:JvmName("name") val name: String,
    @get:JvmName("trackCount") val trackCount: Int,
    @get:JvmName("description") val description: String?,
    @get:JvmName("artworkUris") var artworkUris: List<String>,
    @get:JvmName("placeholderSeed") val placeholderSeed: Int,
) {
    init {
        artworkUris = Collections.unmodifiableList(ArrayList(artworkUris))
    }

    /** Copy with a different track count (counts are derived by repositories). */
    fun withTrackCount(trackCount: Int): Playlist = copy(trackCount = trackCount)
}

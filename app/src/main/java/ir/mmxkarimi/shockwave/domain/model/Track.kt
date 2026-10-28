package ir.mmxkarimi.shockwave.domain.model

/**
 * A playable song. Immutable value object.
 *
 * Artwork: when [artworkUri] is null the UI draws a deterministic gradient from [placeholderSeed].
 * [year] and [trackNumber] use 0 for "unknown".
 *
 * @param mediaUri playable URI (content://, file://, https://); null for fakes
 * @param addedAtMs epoch millis when added to the library (drives "Recently saved" / "New releases")
 */
data class Track(
    @get:JvmName("id") val id: String,
    @get:JvmName("title") val title: String,
    @get:JvmName("artistId") val artistId: String,
    @get:JvmName("artistName") val artistName: String,
    @get:JvmName("albumId") val albumId: String,
    @get:JvmName("albumTitle") val albumTitle: String,
    @get:JvmName("durationMs") val durationMs: Long,
    @get:JvmName("artworkUri") val artworkUri: String?,
    @get:JvmName("placeholderSeed") val placeholderSeed: Int,
    @get:JvmName("mediaUri") val mediaUri: String?,
    @get:JvmName("explicit") val explicit: Boolean,
    @get:JvmName("liked") val liked: Boolean,
    @get:JvmName("year") val year: Int,
    @get:JvmName("trackNumber") val trackNumber: Int,
    @get:JvmName("addedAtMs") val addedAtMs: Long,
) {
    /** Copy of this track with a different like flag. */
    fun withLiked(liked: Boolean): Track = copy(liked = liked)
}

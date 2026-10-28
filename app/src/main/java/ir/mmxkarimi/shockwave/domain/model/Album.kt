package ir.mmxkarimi.shockwave.domain.model

/** An album. [year] is 0 when unknown; artwork rules as in [Track]. Immutable. */
data class Album(
    @get:JvmName("id") val id: String,
    @get:JvmName("title") val title: String,
    @get:JvmName("artistId") val artistId: String,
    @get:JvmName("artistName") val artistName: String,
    @get:JvmName("year") val year: Int,
    @get:JvmName("trackCount") val trackCount: Int,
    @get:JvmName("artworkUri") val artworkUri: String?,
    @get:JvmName("placeholderSeed") val placeholderSeed: Int,
)

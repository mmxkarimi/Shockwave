package ir.mmxkarimi.shockwave.domain.model

/** An artist. Image/placeholder rules as in [Track]. Immutable. */
data class Artist(
    @get:JvmName("id") val id: String,
    @get:JvmName("name") val name: String,
    @get:JvmName("albumCount") val albumCount: Int,
    @get:JvmName("trackCount") val trackCount: Int,
    @get:JvmName("imageUri") val imageUri: String?,
    @get:JvmName("placeholderSeed") val placeholderSeed: Int,
)

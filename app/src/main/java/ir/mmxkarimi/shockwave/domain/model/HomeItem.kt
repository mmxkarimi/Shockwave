package ir.mmxkarimi.shockwave.domain.model

/**
 * One entry of a home section: exactly one of track / album / playlist, as given by [kind].
 */
data class HomeItem(
    @get:JvmName("kind") val kind: Kind,
    @get:JvmName("track") val track: Track?,
    @get:JvmName("album") val album: Album?,
    @get:JvmName("playlist") val playlist: Playlist?,
) {
    enum class Kind { TRACK, ALBUM, PLAYLIST }

    companion object {
        @JvmStatic fun of(track: Track): HomeItem = HomeItem(Kind.TRACK, track, null, null)

        @JvmStatic fun of(album: Album): HomeItem = HomeItem(Kind.ALBUM, null, album, null)

        @JvmStatic fun of(playlist: Playlist): HomeItem = HomeItem(Kind.PLAYLIST, null, null, playlist)
    }
}

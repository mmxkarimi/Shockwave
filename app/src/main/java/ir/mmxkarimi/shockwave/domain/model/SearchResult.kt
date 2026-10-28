package ir.mmxkarimi.shockwave.domain.model

/**
 * One search hit: exactly one of track / album / artist / playlist, as given by [kind].
 */
data class SearchResult(
    @get:JvmName("kind") val kind: Kind,
    @get:JvmName("track") val track: Track?,
    @get:JvmName("album") val album: Album?,
    @get:JvmName("artist") val artist: Artist?,
    @get:JvmName("playlist") val playlist: Playlist?,
) {
    enum class Kind { TRACK, ALBUM, ARTIST, PLAYLIST }

    companion object {
        @JvmStatic fun of(track: Track): SearchResult = SearchResult(Kind.TRACK, track, null, null, null)

        @JvmStatic fun of(album: Album): SearchResult = SearchResult(Kind.ALBUM, null, album, null, null)

        @JvmStatic fun of(artist: Artist): SearchResult = SearchResult(Kind.ARTIST, null, null, artist, null)

        @JvmStatic fun of(playlist: Playlist): SearchResult = SearchResult(Kind.PLAYLIST, null, null, null, playlist)
    }
}

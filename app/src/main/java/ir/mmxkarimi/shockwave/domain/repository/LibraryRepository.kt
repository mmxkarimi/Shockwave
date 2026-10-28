package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.SearchFilter
import ir.mmxkarimi.shockwave.domain.model.SearchResult
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * The user's library. All `LiveData` are delivered on the main thread and update when the underlying data
 * changes (like, playlist edit, rescan). Writes are fire-and-forget; observers see the result.
 */
interface LibraryRepository {

    fun getAllTracks(): LiveData<List<Track>>

    fun getLikedTracks(): LiveData<List<Track>>

    fun getAlbums(): LiveData<List<Album>>

    fun getArtists(): LiveData<List<Artist>>

    fun getPlaylists(): LiveData<List<Playlist>>

    /** Most recently played first. */
    fun getHistory(): LiveData<List<Track>>

    /** Most recently added first. */
    fun getRecentlySaved(): LiveData<List<Track>>

    /** Value is null when the id is unknown. */
    fun getAlbum(id: String): LiveData<Album?>

    fun getArtist(id: String): LiveData<Artist?>

    fun getPlaylist(id: String): LiveData<Playlist?>

    fun getAlbumTracks(albumId: String): LiveData<List<Track>>

    fun getArtistTracks(artistId: String): LiveData<List<Track>>

    fun getPlaylistTracks(playlistId: String): LiveData<List<Track>>

    /** Empty list for a blank query. Result order: tracks, albums, artists, playlists. */
    fun search(query: String, filter: SearchFilter): LiveData<List<SearchResult>>

    fun toggleLike(trackId: String)

    /** Moves the track to the top of the history. */
    fun recordPlay(trackId: String)

    /** Creates an empty playlist; [callback] (may be null) receives it on the main thread (null if creation failed). */
    fun createPlaylist(name: String, callback: Callback<Playlist?>?)

    fun addToPlaylist(playlistId: String, trackId: String)
}

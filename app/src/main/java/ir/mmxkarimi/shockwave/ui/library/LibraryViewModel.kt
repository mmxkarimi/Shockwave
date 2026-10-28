package ir.mmxkarimi.shockwave.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.PlayerController

/** Library hub: Liked songs card, Albums / Playlists / Artists / History tiles, Recently saved. */
class LibraryViewModel(
    private val library: LibraryRepository,
    private val player: PlayerController
) : ViewModel() {

    fun getAllTracks(): LiveData<List<Track>> = library.allTracks
    fun getLiked(): LiveData<List<Track>> = library.likedTracks
    fun getAlbums(): LiveData<List<Album>> = library.albums
    fun getArtists(): LiveData<List<Artist>> = library.artists
    fun getPlaylists(): LiveData<List<Playlist>> = library.playlists
    fun getHistory(): LiveData<List<Track>> = library.history
    fun getRecentlySaved(): LiveData<List<Track>> = library.recentlySaved

    fun toggleLike(trackId: String) {
        library.toggleLike(trackId)
    }

    fun shuffle(tracks: List<Track>, playingFrom: String) {
        if (tracks.isEmpty()) return
        val list = ArrayList(tracks)
        list.shuffle()
        player.play(list, 0, playingFrom)
    }

    /** Plays the liked songs from the top; no-op when there are none. */
    fun playLiked(playingFrom: String) {
        val liked = library.likedTracks.value
        if (!liked.isNullOrEmpty()) {
            player.play(liked, 0, playingFrom)
        }
    }

    fun play(tracks: List<Track>, index: Int, playingFrom: String) {
        player.play(tracks, index, playingFrom)
    }

    companion object {
        /** Total duration of [tracks] in ms. */
        @JvmStatic
        fun totalMs(tracks: List<Track>): Long {
            var sum = 0L
            for (t in tracks) {
                sum += t.durationMs
            }
            return sum
        }

        @JvmStatic
        fun factory(library: LibraryRepository, player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LibraryViewModel(library, player) as T
                }
            }
        }
    }
}

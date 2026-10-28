package ir.mmxkarimi.shockwave.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator.TrackList

/** Album / Artist / Playlist detail and the Liked, History and Recently saved lists. */
class TrackListViewModel(
    library: LibraryRepository,
    private val player: PlayerController,
    kind: TrackList,
    id: String?
) : ViewModel() {

    /** Header shown above the list. [title] is null for LIKED/HISTORY/RECENT. */
    data class Header(
        val title: String?,
        val subtitle: String?,
        val seed: Int,
        val circle: Boolean
    )

    private val tracksLiveData: LiveData<List<Track>>
    private val headerLiveData: LiveData<Header?>

    init {
        when (kind) {
            TrackList.ALBUM -> {
                tracksLiveData = library.getAlbumTracks(id)
                headerLiveData = library.getAlbum(id).map { a ->
                    if (a == null) null
                    else Header(
                        title = a.title,
                        subtitle = a.artistName + (if (a.year > 0) " · " + a.year else ""),
                        seed = a.placeholderSeed,
                        circle = false
                    )
                }
            }
            TrackList.ARTIST -> {
                tracksLiveData = library.getArtistTracks(id)
                headerLiveData = library.getArtist(id).map { a ->
                    if (a == null) null
                    else Header(
                        title = a.name,
                        subtitle = null,
                        seed = a.placeholderSeed,
                        circle = true
                    )
                }
            }
            TrackList.PLAYLIST -> {
                tracksLiveData = library.getPlaylistTracks(id)
                headerLiveData = library.getPlaylist(id).map { p ->
                    if (p == null) null
                    else Header(
                        title = p.name,
                        subtitle = p.description,
                        seed = p.placeholderSeed,
                        circle = false
                    )
                }
            }
            TrackList.LIKED -> {
                tracksLiveData = library.likedTracks
                headerLiveData = MutableLiveData(Header(null, null, 1, false))
            }
            TrackList.HISTORY -> {
                tracksLiveData = library.history
                headerLiveData = MutableLiveData(Header(null, null, 2, false))
            }
            TrackList.RECENT -> {
                tracksLiveData = library.recentlySaved
                headerLiveData = MutableLiveData(Header(null, null, 3, false))
            }
        }
    }

    fun getTracks(): LiveData<List<Track>> = tracksLiveData

    fun getHeader(): LiveData<Header?> = headerLiveData

    fun play(tracks: List<Track>, index: Int, playingFrom: String) {
        player.play(tracks, index, playingFrom)
    }

    fun shuffle(tracks: List<Track>, playingFrom: String) {
        val copy = ArrayList(tracks)
        if (copy.isEmpty()) return
        copy.shuffle()
        player.play(copy, 0, playingFrom)
    }

    companion object {
        @JvmStatic
        fun factory(
            library: LibraryRepository,
            player: PlayerController,
            kind: TrackList,
            id: String?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrackListViewModel(library, player, kind, id) as T
                }
            }
        }
    }
}

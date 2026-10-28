package ir.mmxkarimi.shockwave.fake

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.HomeItem
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.model.HomeSectionStyle
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.HomeFeedSource
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository

/**
 * Builds Home sections from the library: New releases (newest albums), Throwbacks (oldest), playlists, Quick picks.
 */
class FakeHomeFeedSource(library: LibraryRepository) : HomeFeedSource {
    private val sections = MediatorLiveData<List<HomeSection>>()

    init {
        val albums = library.getAlbums()
        val recent = library.getRecentlySaved()
        val history = library.getHistory()
        val playlists = library.getPlaylists()
        val rebuild = Runnable {
            if (albums.value == null || recent.value == null
                || history.value == null || playlists.value == null
            ) {
                return@Runnable
            }
            sections.setValue(build(albums.value!!, recent.value!!, history.value!!, playlists.value!!))
        }
        for (source in listOf<LiveData<*>>(albums, recent, history, playlists)) {
            sections.addSource(source) { rebuild.run() }
        }
    }

    override fun getSections(): LiveData<List<HomeSection>> = sections

    private companion object {
        @JvmStatic private fun build(
            albums: List<Album>,
            recent: List<Track>,
            history: List<Track>,
            playlists: List<Playlist>,
        ): List<HomeSection> {
            val newest = ArrayList(albums)
            newest.sortByDescending { it.year }
            val oldest = ArrayList(albums)
            oldest.sortBy { it.year }
            val quick = LinkedHashMap<String, Track>()
            for (t in history) quick.putIfAbsent(t.id, t)
            for (t in recent) quick.putIfAbsent(t.id, t)

            val out = ArrayList<HomeSection>()
            out.add(
                HomeSection(
                    "new-releases", "New releases", "Fresh in your library",
                    HomeSectionStyle.HERO_CAROUSEL, albumItems(newest),
                ),
            )
            out.add(
                HomeSection(
                    "throwbacks", "Throwbacks", "From a while ago",
                    HomeSectionStyle.CAROUSEL, albumItems(oldest),
                ),
            )
            val playlistItems = ArrayList<HomeItem>()
            for (p in playlists) playlistItems.add(HomeItem.of(p))
            out.add(HomeSection("playlists", "Your playlists", null, HomeSectionStyle.CAROUSEL, playlistItems))
            val quickItems = ArrayList<HomeItem>()
            for (t in quick.values) {
                if (quickItems.size == 8) break
                quickItems.add(HomeItem.of(t))
            }
            out.add(
                HomeSection(
                    "quick-picks", "Quick picks", "Start radio from a song",
                    HomeSectionStyle.LIST, quickItems,
                ),
            )
            out.removeAll { s -> s.items.isEmpty() }
            return out
        }

        @JvmStatic private fun albumItems(albums: List<Album>): List<HomeItem> {
            val items = ArrayList<HomeItem>()
            for (a in albums) {
                if (items.size == 6) break
                items.add(HomeItem.of(a))
            }
            return items
        }
    }
}

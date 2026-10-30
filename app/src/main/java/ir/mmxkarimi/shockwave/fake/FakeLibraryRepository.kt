package ir.mmxkarimi.shockwave.fake

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.ScanResult
import ir.mmxkarimi.shockwave.domain.model.SearchFilter
import ir.mmxkarimi.shockwave.domain.model.SearchResult
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.Callback
import ir.mmxkarimi.shockwave.domain.repository.LibraryAdmin
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.function.Supplier

/**
 * In-memory [LibraryRepository] seeded from [SampleData]. Main thread only; callbacks run inline.
 */
class FakeLibraryRepository : LibraryRepository, LibraryAdmin {
    private val tracks = ArrayList<Track>()
    private val playlists = LinkedHashMap<String, Playlist>()
    private val playlistTrackIds = LinkedHashMap<String, MutableList<String>>()
    private val historyIds = ArrayList<String>(SampleData.SEED_HISTORY_IDS)
    private var nextPlaylist = 1

    /** Bumped on every write; every query is derived from it. */
    private val version = MutableLiveData(0)

    init {
        for (t in SampleData.TRACKS) tracks.add(t.withLiked(SampleData.INITIALLY_LIKED_IDS.contains(t.id)))
        SampleData.SEED_PLAYLISTS.forEach { (p, ids) ->
            playlists[p.id] = p
            playlistTrackIds[p.id] = ArrayList(ids)
        }
    }

    // Transformations.map is @Deprecated(HIDDEN) for Kotlin, so replicate it exactly: seed the result with
    // transform(source.getValue()) when the source is already initialized (as map does in lifecycle 2.11),
    // then recompute through a MediatorLiveData source on every change.
    private fun <T> derive(compute: Supplier<T>): LiveData<T> {
        val out = if (version.isInitialized) MediatorLiveData(compute.get()) else MediatorLiveData()
        out.addSource(version) { out.setValue(compute.get()) }
        return out
    }

    private fun changed() {
        version.setValue(version.value!! + 1)
    }

    private fun byIds(ids: List<String>): MutableList<Track> {
        val byId = LinkedHashMap<String, Track>()
        for (t in tracks) byId[t.id] = t
        val out = ArrayList<Track>()
        for (id in ids) if (byId.containsKey(id)) out.add(byId.getValue(id))
        return out
    }

    private fun filterTracks(p: (Track) -> Boolean): MutableList<Track> {
        val out = ArrayList<Track>()
        for (t in tracks) if (p(t)) out.add(t)
        return out
    }

    private fun withCount(p: Playlist): Playlist {
        return p.withTrackCount(playlistTrackIds.getValue(p.id).size)
    }

    override fun getAllTracks(): LiveData<List<Track>> {
        return derive { ArrayList(tracks) }
    }

    override fun getLikedTracks(): LiveData<List<Track>> {
        return derive { filterTracks(Track::liked) }
    }

    override fun getAlbums(): LiveData<List<Album>> {
        return derive { SampleData.ALBUMS }
    }

    override fun getArtists(): LiveData<List<Artist>> {
        return derive { SampleData.ARTISTS }
    }

    override fun getPlaylists(): LiveData<List<Playlist>> {
        return derive {
            val out = ArrayList<Playlist>()
            for (p in playlists.values) out.add(withCount(p))
            out
        }
    }

    override fun getHistory(): LiveData<List<Track>> {
        return derive { byIds(historyIds) }
    }

    override fun getRecentlySaved(): LiveData<List<Track>> {
        return derive {
            val out = ArrayList(tracks)
            out.sortByDescending { it.addedAtMs }
            out
        }
    }

    override fun getAlbum(id: String): LiveData<Album?> {
        return derive {
            for (a in SampleData.ALBUMS) if (a.id == id) return@derive a
            null
        }
    }

    override fun getArtist(id: String): LiveData<Artist?> {
        return derive {
            for (a in SampleData.ARTISTS) if (a.id == id) return@derive a
            null
        }
    }

    override fun getPlaylist(id: String): LiveData<Playlist?> {
        return derive { if (playlists.containsKey(id)) withCount(playlists.getValue(id)) else null }
    }

    override fun getAlbumTracks(albumId: String): LiveData<List<Track>> {
        return derive {
            val out = filterTracks { t -> t.albumId == albumId }
            out.sortBy { it.trackNumber }
            out
        }
    }

    override fun getArtistTracks(artistId: String): LiveData<List<Track>> {
        return derive { filterTracks { t -> t.artistId == artistId } }
    }

    override fun getPlaylistTracks(playlistId: String): LiveData<List<Track>> {
        return derive { byIds(playlistTrackIds.getOrDefault(playlistId, Collections.emptyList())) }
    }

    override fun search(query: String, filter: SearchFilter): LiveData<List<SearchResult>> {
        return derive {
            val q = query.trim().lowercase(Locale.ROOT)
            val out = ArrayList<SearchResult>()
            if (q.isEmpty()) return@derive out
            val all = filter == SearchFilter.ALL
            if (all || filter == SearchFilter.SONGS) {
                for (t in tracks) {
                    if (has(t.title, q) || has(t.artistName, q) || has(t.albumTitle, q)) {
                        out.add(SearchResult.of(t))
                    }
                }
            }
            if (all || filter == SearchFilter.ALBUMS) {
                for (a in SampleData.ALBUMS) if (has(a.title, q) || has(a.artistName, q)) out.add(SearchResult.of(a))
            }
            if (all || filter == SearchFilter.ARTISTS) {
                for (a in SampleData.ARTISTS) if (has(a.name, q)) out.add(SearchResult.of(a))
            }
            if (all || filter == SearchFilter.PLAYLISTS) {
                for (p in playlists.values) if (has(p.name, q)) out.add(SearchResult.of(withCount(p)))
            }
            out
        }
    }

    override fun toggleLike(trackId: String) {
        for (i in tracks.indices) {
            val t = tracks[i]
            if (t.id == trackId) tracks[i] = t.withLiked(!t.liked)
        }
        changed()
    }

    override fun recordPlay(trackId: String) {
        historyIds.remove(trackId)
        historyIds.add(0, trackId)
        changed()
    }

    override fun createPlaylist(name: String, callback: Callback<Playlist?>?) {
        val p = Playlist("playlist-new-${nextPlaylist++}", name, 0, null, emptyList(), name.hashCode())
        playlists[p.id] = p
        playlistTrackIds[p.id] = ArrayList()
        changed()
        if (callback != null) callback.onResult(p)
    }

    override fun addToPlaylist(playlistId: String, trackId: String) {
        val ids = playlistTrackIds[playlistId]
        if (ids == null || ids.contains(trackId)) return
        ids.add(trackId)
        changed()
    }

    // ---- LibraryAdmin: nothing to scan in the fake; playlist edits work on the in-memory lists ----

    override fun isScanning(): LiveData<Boolean> {
        return MutableLiveData(false)
    }

    override fun rescan(callback: Callback<ScanResult>?) {
        if (callback != null) callback.onResult(ScanResult(true, 0, 0, 0))
    }

    override fun renamePlaylist(playlistId: String, name: String) {
        val p = playlists[playlistId]
        if (p == null || name.trim().isEmpty()) return
        playlists[playlistId] = Playlist(
            p.id, name.trim(), p.trackCount, p.description, p.artworkUris,
            p.placeholderSeed,
        )
        changed()
    }

    override fun deletePlaylist(playlistId: String) {
        if (playlists.remove(playlistId) == null) return
        playlistTrackIds.remove(playlistId)
        changed()
    }

    override fun deleteTrack(trackId: String) {
        tracks.remove(trackId)
        changed()
    }

    override fun removeFromPlaylist(playlistId: String, trackId: String) {
        val ids = playlistTrackIds[playlistId]
        if (ids != null && ids.remove(trackId)) changed()
    }

    override fun moveInPlaylist(playlistId: String, from: Int, to: Int) {
        val ids = playlistTrackIds[playlistId]
        if (ids == null || from < 0 || from >= ids.size || to < 0 || to >= ids.size || from == to) return
        ids.add(to, ids.removeAt(from))
        changed()
    }

    private companion object {
        @JvmStatic private fun has(haystack: String, lowerNeedle: String): Boolean {
            return haystack.lowercase(Locale.ROOT).contains(lowerNeedle)
        }
    }
}

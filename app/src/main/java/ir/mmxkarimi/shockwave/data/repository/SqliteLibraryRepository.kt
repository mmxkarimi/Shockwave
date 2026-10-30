package ir.mmxkarimi.shockwave.data.repository

import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.db.query.AlbumQueries
import ir.mmxkarimi.shockwave.data.db.query.ArtistQueries
import ir.mmxkarimi.shockwave.data.db.query.LikePattern
import ir.mmxkarimi.shockwave.data.db.query.LikeQueries
import ir.mmxkarimi.shockwave.data.db.query.PlayStatsQueries
import ir.mmxkarimi.shockwave.data.db.query.PlaylistQueries
import ir.mmxkarimi.shockwave.data.db.query.TrackQueries
import ir.mmxkarimi.shockwave.data.scanner.MediaStoreScanner
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
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [LibraryRepository] on framework SQLite. Reads run on the io executor and are delivered as LiveData that reload
 * only while observed ([DbLiveData]); writes run on the io executor and notify through [ChangeNotifier].
 * Also the [LibraryAdmin]: library scanning and the remaining playlist edits (rename, delete, remove, reorder).
 */
class SqliteLibraryRepository(
    private val helper: DbHelper,
    private val io: Executor,
    private val notifier: ChangeNotifier,
    private val scanner: MediaStoreScanner,
) : LibraryRepository, LibraryAdmin {
    private val main = Handler(Looper.getMainLooper())
    private val scanRunning = AtomicBoolean()
    private val scanning: MutableLiveData<Boolean> = MutableLiveData(false)

    private val allTracks: LiveData<List<Track>>
    private val likedTracks: LiveData<List<Track>>
    private val albums: LiveData<List<Album>>
    private val artists: LiveData<List<Artist>>
    private val playlists: LiveData<List<Playlist>>
    private val history: LiveData<List<Track>>
    private val recentlySaved: LiveData<List<Track>>

    init {
        val tracksAndLikes = ChangeNotifier.TRACKS or ChangeNotifier.LIKES
        allTracks = live(tracksAndLikes) { TrackQueries(it).all() }
        likedTracks = live(tracksAndLikes) { TrackQueries(it).liked() }
        albums = live(ChangeNotifier.TRACKS) { AlbumQueries(it).all() }
        artists = live(ChangeNotifier.TRACKS) { ArtistQueries(it).all() }
        playlists = live(ChangeNotifier.TRACKS or ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).all() }
        history = live(tracksAndLikes or ChangeNotifier.HISTORY) { TrackQueries(it).history(HISTORY_LIMIT) }
        recentlySaved = live(tracksAndLikes) { TrackQueries(it).recentlySaved(RECENT_LIMIT) }
    }

    private fun <T> live(topics: Int, query: (SQLiteDatabase) -> T): LiveData<T> =
        DbLiveData(io, notifier, topics) { query(helper.writableDatabase) }

    // ---- reads --------------------------------------------------------------------------------------------------

    override fun getAllTracks(): LiveData<List<Track>> = allTracks

    override fun getLikedTracks(): LiveData<List<Track>> = likedTracks

    override fun getAlbums(): LiveData<List<Album>> = albums

    override fun getArtists(): LiveData<List<Artist>> = artists

    override fun getPlaylists(): LiveData<List<Playlist>> = playlists

    override fun getHistory(): LiveData<List<Track>> = history

    override fun getRecentlySaved(): LiveData<List<Track>> = recentlySaved

    override fun getAlbum(id: String): LiveData<Album?> =
        live<Album?>(ChangeNotifier.TRACKS) { AlbumQueries(it).byId(id) }

    override fun getArtist(id: String): LiveData<Artist?> =
        live<Artist?>(ChangeNotifier.TRACKS) { ArtistQueries(it).byId(id) }

    override fun getPlaylist(id: String): LiveData<Playlist?> =
        live<Playlist?>(ChangeNotifier.TRACKS or ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).byId(id) }

    override fun getAlbumTracks(albumId: String): LiveData<List<Track>> =
        live(ChangeNotifier.TRACKS or ChangeNotifier.LIKES) { TrackQueries(it).byAlbum(albumId) }

    override fun getArtistTracks(artistId: String): LiveData<List<Track>> =
        live(ChangeNotifier.TRACKS or ChangeNotifier.LIKES) { TrackQueries(it).byArtist(artistId) }

    override fun getPlaylistTracks(playlistId: String): LiveData<List<Track>> =
        live(ChangeNotifier.ALL and ChangeNotifier.HISTORY.inv()) { TrackQueries(it).byPlaylist(playlistId) }

    override fun search(query: String, filter: SearchFilter): LiveData<List<SearchResult>> {
        val pattern = LikePattern.contains(query)
        return live(ChangeNotifier.ALL and ChangeNotifier.HISTORY.inv()) {
            if (pattern == null) emptyList() else searchNow(it, pattern, filter)
        }
    }

    // ---- writes -------------------------------------------------------------------------------------------------

    override fun toggleLike(trackId: String) {
        write(ChangeNotifier.LIKES) { LikeQueries(it).toggle(trackId, System.currentTimeMillis()) }
    }

    override fun recordPlay(trackId: String) {
        write(ChangeNotifier.HISTORY) { PlayStatsQueries(it).recordPlay(trackId, System.currentTimeMillis()) }
    }

    override fun createPlaylist(name: String, callback: Callback<Playlist?>?) {
        val clean = if (name.trim().isEmpty()) DEFAULT_PLAYLIST_NAME else name.trim()
        io.execute {
            val queries = PlaylistQueries(helper.writableDatabase)
            val created = queries.byId(queries.create(clean, System.currentTimeMillis()))
            notifier.notifyChanged(ChangeNotifier.PLAYLISTS)
            if (callback != null) main.post { callback.onResult(created) }
        }
    }

    override fun addToPlaylist(playlistId: String, trackId: String) {
        write(ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).addTrack(playlistId, trackId) }
    }

    override fun renamePlaylist(playlistId: String, name: String) {
        if (name.trim().isEmpty()) return
        write(ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).rename(playlistId, name.trim()) }
    }

    override fun deletePlaylist(playlistId: String) {
        write(ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).delete(playlistId) }
    }

    override fun deleteTrack(trackId: String) {
        write(ChangeNotifier.TRACKS) { TrackQueries(it).deleteByIds(listOf(trackId)) }
    }

    override fun removeFromPlaylist(playlistId: String, trackId: String) {
        write(ChangeNotifier.PLAYLISTS) { PlaylistQueries(it).removeTrack(playlistId, trackId) }
    }

    /** Moves the member at index [from] to index [to] (indices into the playlist's current order). */
    override fun moveInPlaylist(playlistId: String, from: Int, to: Int) {
        write(ChangeNotifier.PLAYLISTS) { db ->
            val queries = PlaylistQueries(db)
            val ids = queries.orderedTrackIds(playlistId).toMutableList()
            if (from < 0 || from >= ids.size || to < 0 || to >= ids.size || from == to) return@write
            ids.add(to, ids.removeAt(from))
            db.beginTransaction()
            try {
                queries.setOrder(playlistId, ids)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun write(topic: Int, action: (SQLiteDatabase) -> Unit) {
        io.execute {
            action(helper.writableDatabase)
            notifier.notifyChanged(topic)
        }
    }

    // ---- scanning -----------------------------------------------------------------------------------------------

    /** True while a scan runs (for a progress indicator). */
    override fun isScanning(): LiveData<Boolean> = scanning

    /** Manual rescan. `callback` (may be null) gets the result on the main thread; FAILED if one is already running. */
    override fun rescan(callback: Callback<ScanResult>?) {
        io.execute { scanNow(callback) }
    }

    private fun scanNow(callback: Callback<ScanResult>?) {
        var result = ScanResult.FAILED
        if (scanRunning.compareAndSet(false, true)) {
            scanning.postValue(true)
            try {
                result = scanner.scan()
                if (result.changed()) notifier.notifyChanged(ChangeNotifier.TRACKS)
            } finally {
                scanRunning.set(false)
                scanning.postValue(false)
            }
        }
        if (callback != null) {
            val delivered = result
            main.post { callback.onResult(delivered) }
        }
    }

    companion object {
        private const val HISTORY_LIMIT = 200
        private const val RECENT_LIMIT = 100
        private const val SEARCH_LIMIT = 50
        private const val DEFAULT_PLAYLIST_NAME = "New playlist"

        @JvmStatic private fun searchNow(db: SQLiteDatabase, pattern: String, filter: SearchFilter): List<SearchResult> {
            val all = filter == SearchFilter.ALL
            val out = ArrayList<SearchResult>()
            if (all || filter == SearchFilter.SONGS) {
                for (t in TrackQueries(db).search(pattern, SEARCH_LIMIT)) out.add(SearchResult.of(t))
            }
            if (all || filter == SearchFilter.ALBUMS) {
                for (a in AlbumQueries(db).search(pattern, SEARCH_LIMIT)) out.add(SearchResult.of(a))
            }
            if (all || filter == SearchFilter.ARTISTS) {
                for (a in ArtistQueries(db).search(pattern, SEARCH_LIMIT)) out.add(SearchResult.of(a))
            }
            if (all || filter == SearchFilter.PLAYLISTS) {
                for (p in PlaylistQueries(db).search(pattern, SEARCH_LIMIT)) out.add(SearchResult.of(p))
            }
            return out
        }
    }
}

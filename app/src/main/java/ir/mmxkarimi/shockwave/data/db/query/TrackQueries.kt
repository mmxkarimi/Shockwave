package ir.mmxkarimi.shockwave.data.db.query

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.LikesContract
import ir.mmxkarimi.shockwave.data.db.contract.PlayStatsContract
import ir.mmxkarimi.shockwave.data.db.contract.PlaylistTracksContract
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.domain.model.Track

/** All SQL touching the `tracks` table (plus the likes / play_stats / playlist joins that decorate a Track). */
class TrackQueries(private val db: SQLiteDatabase) {
    /** A track with its listening numbers, input of the Home "Quick picks" ranking. */
    data class TrackStat(
        @get:JvmName("track") val track: Track,
        @get:JvmName("plays") val plays: Int,
        @get:JvmName("lastPlayedMs") val lastPlayedMs: Long,
    ) {
        // Record-style accessors, callable from Kotlin as functions; JVM names differ from the property getters.
        @JvmName("trackComponent") fun track(): Track = track
        @JvmName("playsComponent") fun plays(): Int = plays
        @JvmName("lastPlayedMsComponent") fun lastPlayedMs(): Long = lastPlayedMs
    }

    fun all(): List<Track> = run(BY_TITLE)

    fun liked(): List<Track> = run(
        " WHERE l." + LikesContract.COL_TRACK_ID + " IS NOT NULL ORDER BY l." + LikesContract.COL_LIKED_AT + " DESC"
    )

    /** Most recently played first. */
    fun history(limit: Int): List<Track> = run(
        " JOIN " + PlayStatsContract.TABLE + " ps ON ps." + PlayStatsContract.COL_TRACK_ID + " = t." + TracksContract.COL_ID +
                " ORDER BY ps." + PlayStatsContract.COL_LAST_PLAYED_AT + " DESC LIMIT " + limit
    )

    fun recentlySaved(limit: Int): List<Track> = run(" ORDER BY t." + TracksContract.COL_ADDED_AT_MS + " DESC LIMIT " + limit)

    fun byAlbum(albumId: String): List<Track> = run(
        " WHERE t." + TracksContract.COL_ALBUM_ID + " = ? ORDER BY t." + TracksContract.COL_TRACK_NUMBER + ", t." +
                TracksContract.COL_TITLE + " COLLATE LOCALIZED", albumId
    )

    fun byArtist(artistId: String): List<Track> = run(
        " WHERE t." + TracksContract.COL_ARTIST_ID + " = ? ORDER BY t." + TracksContract.COL_ALBUM_TITLE +
                " COLLATE LOCALIZED, t." + TracksContract.COL_TRACK_NUMBER, artistId
    )

    fun byPlaylist(playlistId: String): List<Track> = run(
        " JOIN " + PlaylistTracksContract.TABLE + " pt ON pt." + PlaylistTracksContract.COL_TRACK_ID + " = t." +
                TracksContract.COL_ID + " WHERE pt." + PlaylistTracksContract.COL_PLAYLIST_ID + " = ? ORDER BY pt." +
                PlaylistTracksContract.COL_POSITION, playlistId
    )

    fun getById(trackId: String): Track? = run(" WHERE t." + TracksContract.COL_ID + " = ?", trackId).firstOrNull()

    /** @param pattern from [LikePattern.contains] */
    fun search(pattern: String, limit: Int): List<Track> = run(
        " WHERE t." + TracksContract.COL_TITLE + LikePattern.CLAUSE + " OR t." + TracksContract.COL_ARTIST_NAME +
                LikePattern.CLAUSE + " OR t." + TracksContract.COL_ALBUM_TITLE + LikePattern.CLAUSE + BY_TITLE +
                " LIMIT " + limit,
        pattern, pattern, pattern
    )

    /** Played or liked tracks, most played first, at most `limit`. */
    fun quickPickCandidates(limit: Int): List<TrackStat> {
        val sql = "SELECT " + COLUMNS + ", COALESCE(ps." + PlayStatsContract.COL_PLAY_COUNT + ", 0), COALESCE(ps." +
                PlayStatsContract.COL_LAST_PLAYED_AT + ", 0)" + FROM + JOIN_STATS +
                " WHERE ps." + PlayStatsContract.COL_PLAY_COUNT + " > 0 OR l." + LikesContract.COL_TRACK_ID +
                " IS NOT NULL ORDER BY ps." + PlayStatsContract.COL_PLAY_COUNT + " DESC LIMIT " + limit
        val out = ArrayList<TrackStat>()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) out.add(TrackStat(read(c), c.getInt(13), c.getLong(14)))
        }
        return out
    }

    /** Track id to file modification time, what the incremental scan compares against. */
    fun modifiedById(): Map<String, Long> {
        val out = HashMap<String, Long>()
        db.query(
            TracksContract.TABLE, arrayOf(TracksContract.COL_ID, TracksContract.COL_MODIFIED_MS),
            null, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) out[c.getString(0)] = c.getLong(1)
        }
        return out
    }

    /** Inserts or replaces the row of [track]. The liked flag is ignored (likes live in their own table). */
    fun upsert(track: Track, modifiedMs: Long) {
        val v = ContentValues()
        v.put(TracksContract.COL_ID, track.id)
        v.put(TracksContract.COL_TITLE, track.title)
        v.put(TracksContract.COL_ARTIST_ID, track.artistId)
        v.put(TracksContract.COL_ARTIST_NAME, track.artistName)
        v.put(TracksContract.COL_ALBUM_ID, track.albumId)
        v.put(TracksContract.COL_ALBUM_TITLE, track.albumTitle)
        v.put(TracksContract.COL_DURATION_MS, track.durationMs)
        v.put(TracksContract.COL_ARTWORK_URI, track.artworkUri)
        v.put(TracksContract.COL_MEDIA_URI, track.mediaUri)
        v.put(TracksContract.COL_YEAR, track.year)
        v.put(TracksContract.COL_TRACK_NUMBER, track.trackNumber)
        v.put(TracksContract.COL_ADDED_AT_MS, track.addedAtMs)
        v.put(TracksContract.COL_MODIFIED_MS, modifiedMs)
        db.insertWithOnConflict(TracksContract.TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Deletes the given track ids in chunks; returns how many rows went away. */
    fun deleteByIds(ids: Collection<String>): Int {
        val all = ArrayList(ids)
        var deleted = 0
        for (from in 0 until all.size step CHUNK) {
            val part = all.subList(from, minOf(all.size, from + CHUNK))
            val marks = part.joinToString(",") { "?" }
            deleted += db.delete(TracksContract.TABLE, TracksContract.COL_ID + " IN (" + marks + ")", part.toTypedArray())
        }
        return deleted
    }

    private fun run(tail: String, vararg args: String): List<Track> {
        val out = ArrayList<Track>()
        db.rawQuery(SELECT + tail, if (args.isEmpty()) null else args).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    private companion object {
        private const val CHUNK = 500 // stays below SQLite's 999 bound-variable limit
        private const val BY_TITLE = " ORDER BY t." + TracksContract.COL_TITLE + " COLLATE LOCALIZED"

        /** Columns 0..12 map to a [Track]; the liked flag comes from a LEFT JOIN on likes. */
        private const val COLUMNS = "t." + TracksContract.COL_ID + ", t." + TracksContract.COL_TITLE +
                ", t." + TracksContract.COL_ARTIST_ID + ", t." + TracksContract.COL_ARTIST_NAME + ", t." +
                TracksContract.COL_ALBUM_ID + ", t." + TracksContract.COL_ALBUM_TITLE + ", t." +
                TracksContract.COL_DURATION_MS + ", t." + TracksContract.COL_ARTWORK_URI + ", t." +
                TracksContract.COL_MEDIA_URI + ", t." + TracksContract.COL_YEAR + ", t." +
                TracksContract.COL_TRACK_NUMBER + ", t." + TracksContract.COL_ADDED_AT_MS + ", l." +
                LikesContract.COL_TRACK_ID + " IS NOT NULL"
        private const val FROM = " FROM " + TracksContract.TABLE + " t LEFT JOIN " + LikesContract.TABLE + " l ON l." +
                LikesContract.COL_TRACK_ID + " = t." + TracksContract.COL_ID
        private const val SELECT = "SELECT " + COLUMNS + FROM

        private const val JOIN_STATS = " LEFT JOIN " + PlayStatsContract.TABLE + " ps ON ps." +
                PlayStatsContract.COL_TRACK_ID + " = t." + TracksContract.COL_ID

        private fun read(c: Cursor): Track {
            val albumId = c.getString(4)
            return Track(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3), albumId, c.getString(5),
                c.getLong(6), c.getString(7), Seeds.of(albumId), c.getString(8), false, c.getInt(12) != 0,
                c.getInt(9), c.getInt(10) % 1000, c.getLong(11)
            )
        }
    }
}

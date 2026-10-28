package ir.mmxkarimi.shockwave.data.db.query

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.PlaylistTracksContract
import ir.mmxkarimi.shockwave.data.db.contract.PlaylistsContract
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.domain.model.Playlist

/**
 * All SQL touching playlists and their ordered members. Playlist ids are rowids passed around as strings; SQLite
 * converts them for the INTEGER columns, an unknown / non-numeric id simply matches nothing.
 */
class PlaylistQueries(private val db: SQLiteDatabase) {
    /** Newest playlist first. */
    fun all(): List<Playlist> = run("", " ORDER BY p." + PlaylistsContract.COL_CREATED_AT + " DESC, p." + PlaylistsContract.COL_ID + " DESC")

    /** Null when the id is unknown. */
    fun byId(id: String): Playlist? {
        val found = run(" WHERE p." + PlaylistsContract.COL_ID + " = ?", "", id)
        return if (found.isEmpty()) null else found[0]
    }

    /** @param pattern from [LikePattern.contains] */
    fun search(pattern: String, limit: Int): List<Playlist> = run(
        " WHERE p." + PlaylistsContract.COL_NAME + LikePattern.CLAUSE,
        " ORDER BY p." + PlaylistsContract.COL_NAME + " COLLATE LOCALIZED LIMIT " + limit, pattern
    )

    /** Returns the new playlist id. */
    fun create(name: String, nowMs: Long): String {
        val v = ContentValues()
        v.put(PlaylistsContract.COL_NAME, name)
        v.put(PlaylistsContract.COL_CREATED_AT, nowMs)
        return db.insertOrThrow(PlaylistsContract.TABLE, null, v).toString()
    }

    fun rename(id: String, name: String) {
        val v = ContentValues()
        v.put(PlaylistsContract.COL_NAME, name)
        db.update(PlaylistsContract.TABLE, v, PlaylistsContract.COL_ID + " = ?", arrayOf(id))
    }

    /** Members go with it (ON DELETE CASCADE). */
    fun delete(id: String) {
        db.delete(PlaylistsContract.TABLE, PlaylistsContract.COL_ID + " = ?", arrayOf(id))
    }

    /** Appends the track; no-op if it is already in the playlist or the playlist does not exist. */
    fun addTrack(playlistId: String, trackId: String) {
        db.execSQL(
            "INSERT OR IGNORE INTO " + PT + " (" + PT_PLAYLIST + ", " + PT_TRACK + ", " + PT_POSITION +
                    ") SELECT ?, ?, next FROM (SELECT COALESCE(MAX(" + PT_POSITION + ") + 1, 0) AS next FROM " + PT +
                    " WHERE " + PT_PLAYLIST + " = ?) WHERE EXISTS (SELECT 1 FROM " + PlaylistsContract.TABLE +
                    " WHERE " + PlaylistsContract.COL_ID + " = ?)",
            arrayOf<Any>(playlistId, trackId, playlistId, playlistId)
        )
    }

    fun removeTrack(playlistId: String, trackId: String) {
        db.delete(PT, PT_PLAYLIST + " = ? AND " + PT_TRACK + " = ?", arrayOf(playlistId, trackId))
    }

    /** Member track ids in playlist order (including ones currently missing from the library). */
    fun orderedTrackIds(playlistId: String): List<String> {
        val ids = ArrayList<String>()
        db.query(PT, arrayOf(PT_TRACK), PT_PLAYLIST + " = ?", arrayOf(playlistId), null, null, PT_POSITION).use { c ->
            while (c.moveToNext()) ids.add(c.getString(0))
        }
        return ids
    }

    /** Rewrites positions to match `orderedTrackIds`. Run inside a transaction. */
    fun setOrder(playlistId: String, orderedTrackIds: List<String>) {
        for (i in orderedTrackIds.indices) {
            db.execSQL(
                "UPDATE " + PT + " SET " + PT_POSITION + " = ? WHERE " + PT_PLAYLIST + " = ? AND " + PT_TRACK +
                        " = ?",
                arrayOf<Any>(i, playlistId, orderedTrackIds[i])
            )
        }
    }

    private fun run(where: String, tail: String, vararg args: String): List<Playlist> {
        val out = ArrayList<Playlist>()
        db.rawQuery(SELECT + where + GROUP + tail, if (args.isEmpty()) null else args).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                out.add(Playlist(id, c.getString(1), c.getInt(3), c.getString(2), mosaic(id), Seeds.of(id)))
            }
        }
        return out
    }

    /** Up to four distinct cover URIs of the first members. */
    private fun mosaic(playlistId: String): List<String> {
        val uris = ArrayList<String>(MOSAIC)
        val sql = "SELECT t." + TracksContract.COL_ARTWORK_URI + " FROM " + PT + " pt JOIN " + TracksContract.TABLE +
                " t ON t." + TracksContract.COL_ID + " = pt." + PT_TRACK + " WHERE pt." + PT_PLAYLIST +
                " = ? AND t." + TracksContract.COL_ARTWORK_URI + " IS NOT NULL GROUP BY t." +
                TracksContract.COL_ARTWORK_URI + " ORDER BY MIN(pt." + PT_POSITION + ") LIMIT " + MOSAIC
        db.rawQuery(sql, arrayOf(playlistId)).use { c ->
            while (c.moveToNext()) uris.add(c.getString(0))
        }
        return uris
    }

    private companion object {
        private const val MOSAIC = 4
        private const val PT = PlaylistTracksContract.TABLE
        private const val PT_PLAYLIST = PlaylistTracksContract.COL_PLAYLIST_ID
        private const val PT_TRACK = PlaylistTracksContract.COL_TRACK_ID
        private const val PT_POSITION = PlaylistTracksContract.COL_POSITION

        /** Track count only counts members still present in the library (inner join on tracks). */
        private const val SELECT = "SELECT p." + PlaylistsContract.COL_ID + ", p." + PlaylistsContract.COL_NAME +
                ", p." + PlaylistsContract.COL_DESCRIPTION + ", COUNT(t." + TracksContract.COL_ID + ") FROM " +
                PlaylistsContract.TABLE + " p LEFT JOIN " + PT + " pt ON pt." + PT_PLAYLIST + " = p." +
                PlaylistsContract.COL_ID + " LEFT JOIN " + TracksContract.TABLE + " t ON t." + TracksContract.COL_ID +
                " = pt." + PT_TRACK
        private const val GROUP = " GROUP BY p." + PlaylistsContract.COL_ID
    }
}

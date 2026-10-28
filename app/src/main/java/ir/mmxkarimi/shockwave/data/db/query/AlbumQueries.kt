package ir.mmxkarimi.shockwave.data.db.query

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.PlayStatsContract
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.domain.model.Album

/** Albums are derived from the tracks table with GROUP BY album_id (no album table to keep in sync). */
class AlbumQueries(private val db: SQLiteDatabase) {
    /** An album with listening / freshness numbers, input of the Home ranking. */
    data class AlbumStat(
        @get:JvmName("album") val album: Album,
        @get:JvmName("newestAddedMs") val newestAddedMs: Long,
        @get:JvmName("plays") val plays: Int,
        @get:JvmName("lastPlayedMs") val lastPlayedMs: Long,
    ) {
        // Record-style accessors, callable from Kotlin as functions; JVM names differ from the property getters.
        @JvmName("albumComponent") fun album(): Album = album
        @JvmName("newestAddedMsComponent") fun newestAddedMs(): Long = newestAddedMs
        @JvmName("playsComponent") fun plays(): Int = plays
        @JvmName("lastPlayedMsComponent") fun lastPlayedMs(): Long = lastPlayedMs
    }

    fun all(): List<Album> = run("", BY_TITLE)

    /** Null when no track has this album id. */
    fun byId(id: String): Album? {
        val found = run(" WHERE t." + TracksContract.COL_ALBUM_ID + " = ?", "", id)
        return if (found.isEmpty()) null else found[0]
    }

    /** @param pattern from [LikePattern.contains]; matches album title or artist name. */
    fun search(pattern: String, limit: Int): List<Album> = run(
        " WHERE t." + TracksContract.COL_ALBUM_TITLE + LikePattern.CLAUSE +
                " OR t." + TracksContract.COL_ARTIST_NAME + LikePattern.CLAUSE,
        BY_TITLE + " LIMIT " + limit, pattern, pattern
    )

    fun stats(): List<AlbumStat> {
        val sql = "SELECT " + COLUMNS + ", MAX(t." + TracksContract.COL_ADDED_AT_MS + "), COALESCE(SUM(ps." +
                PlayStatsContract.COL_PLAY_COUNT + "), 0), COALESCE(MAX(ps." + PlayStatsContract.COL_LAST_PLAYED_AT +
                "), 0) FROM " + TracksContract.TABLE + " t LEFT JOIN " + PlayStatsContract.TABLE + " ps ON ps." +
                PlayStatsContract.COL_TRACK_ID + " = t." + TracksContract.COL_ID + GROUP + BY_TITLE
        val out = ArrayList<AlbumStat>()
        db.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) out.add(AlbumStat(read(c), c.getLong(7), c.getInt(8), c.getLong(9)))
        }
        return out
    }

    private fun run(where: String, tail: String, vararg args: String): List<Album> {
        val out = ArrayList<Album>()
        db.rawQuery(
            "SELECT " + COLUMNS + " FROM " + TracksContract.TABLE + " t" + where + GROUP + tail,
            if (args.isEmpty()) null else args
        ).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    private companion object {
        private const val COLUMNS = "t." + TracksContract.COL_ALBUM_ID + ", t." + TracksContract.COL_ALBUM_TITLE +
                ", t." + TracksContract.COL_ARTIST_ID + ", t." + TracksContract.COL_ARTIST_NAME +
                ", MAX(t." + TracksContract.COL_YEAR + "), COUNT(*), MAX(t." + TracksContract.COL_ARTWORK_URI + ")"
        private const val GROUP = " GROUP BY t." + TracksContract.COL_ALBUM_ID
        private const val BY_TITLE = " ORDER BY t." + TracksContract.COL_ALBUM_TITLE + " COLLATE LOCALIZED"

        private fun read(c: Cursor): Album {
            val id = c.getString(0)
            return Album(
                id, c.getString(1), c.getString(2), c.getString(3), c.getInt(4), c.getInt(5),
                c.getString(6), Seeds.of(id)
            )
        }
    }
}

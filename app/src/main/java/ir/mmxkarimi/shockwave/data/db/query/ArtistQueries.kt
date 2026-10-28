package ir.mmxkarimi.shockwave.data.db.query

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.domain.model.Artist

/** Artists are derived from the tracks table with GROUP BY artist_id. The artist image is one of its album covers. */
class ArtistQueries(private val db: SQLiteDatabase) {
    fun all(): List<Artist> = run("", BY_NAME)

    /** Null when no track has this artist id. */
    fun byId(id: String): Artist? {
        val found = run(" WHERE t." + TracksContract.COL_ARTIST_ID + " = ?", "", id)
        return if (found.isEmpty()) null else found[0]
    }

    /** @param pattern from [LikePattern.contains] */
    fun search(pattern: String, limit: Int): List<Artist> =
        run(" WHERE t." + TracksContract.COL_ARTIST_NAME + LikePattern.CLAUSE, BY_NAME + " LIMIT " + limit, pattern)

    private fun run(where: String, tail: String, vararg args: String): List<Artist> {
        val out = ArrayList<Artist>()
        db.rawQuery(SELECT + where + GROUP + tail, if (args.isEmpty()) null else args).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                out.add(Artist(id, c.getString(1), c.getInt(2), c.getInt(3), c.getString(4), Seeds.of(id)))
            }
        }
        return out
    }

    private companion object {
        private const val SELECT = "SELECT t." + TracksContract.COL_ARTIST_ID + ", t." + TracksContract.COL_ARTIST_NAME +
                ", COUNT(DISTINCT t." + TracksContract.COL_ALBUM_ID + "), COUNT(*), MAX(t." +
                TracksContract.COL_ARTWORK_URI + ") FROM " + TracksContract.TABLE + " t"
        private const val GROUP = " GROUP BY t." + TracksContract.COL_ARTIST_ID
        private const val BY_NAME = " ORDER BY t." + TracksContract.COL_ARTIST_NAME + " COLLATE LOCALIZED"
    }
}

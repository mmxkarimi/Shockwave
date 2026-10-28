package ir.mmxkarimi.shockwave.data.db.query

import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.data.db.contract.WaveformsContract

/** All SQL touching the waveform cache. Validity is checked against `tracks.modified_ms` inside SQL. */
class WaveformQueries(private val db: SQLiteDatabase) {
    /** The cached blob, or null if absent or the file changed since it was stored. */
    fun get(trackId: String, bars: Int): ByteArray? {
        val sql = "SELECT w." + WaveformsContract.COL_DATA + " FROM " + WaveformsContract.TABLE + " w JOIN " +
                TracksContract.TABLE + " t ON t." + TracksContract.COL_ID + " = w." +
                WaveformsContract.COL_TRACK_ID + " AND t." + TracksContract.COL_MODIFIED_MS + " = w." +
                WaveformsContract.COL_MODIFIED_MS + " WHERE w." + WaveformsContract.COL_TRACK_ID + " = ? AND w." +
                WaveformsContract.COL_BARS + " = ?"
        db.rawQuery(sql, arrayOf(trackId, bars.toString())).use { c ->
            if (c.moveToFirst()) return c.getBlob(0)
        }
        return null
    }

    /** Stores the blob stamped with the track's current modified time and drops this track's stale entries. */
    fun put(trackId: String, bars: Int, data: ByteArray) {
        db.beginTransaction()
        try {
            db.execSQL(
                "INSERT OR REPLACE INTO " + WaveformsContract.TABLE + " (" + WaveformsContract.COL_TRACK_ID + ", " +
                        WaveformsContract.COL_BARS + ", " + WaveformsContract.COL_MODIFIED_MS + ", " +
                        WaveformsContract.COL_DATA + ") SELECT ?, ?, " + TracksContract.COL_MODIFIED_MS + ", ? FROM " +
                        TracksContract.TABLE + " WHERE " + TracksContract.COL_ID + " = ?",
                arrayOf<Any>(trackId, bars, data, trackId)
            )
            db.execSQL(
                "DELETE FROM " + WaveformsContract.TABLE + " WHERE " + WaveformsContract.COL_TRACK_ID + " = ? AND " +
                        WaveformsContract.COL_MODIFIED_MS + " != (SELECT " + TracksContract.COL_MODIFIED_MS +
                        " FROM " + TracksContract.TABLE + " WHERE " + TracksContract.COL_ID + " = ?)",
                arrayOf<Any>(trackId, trackId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Removes entries of tracks that no longer exist (called after a scan). */
    fun deleteOrphans() {
        db.execSQL(
            "DELETE FROM " + WaveformsContract.TABLE + " WHERE " + WaveformsContract.COL_TRACK_ID +
                    " NOT IN (SELECT " + TracksContract.COL_ID + " FROM " + TracksContract.TABLE + ")"
        )
    }
}

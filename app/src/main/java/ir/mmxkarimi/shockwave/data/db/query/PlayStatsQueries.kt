package ir.mmxkarimi.shockwave.data.db.query

import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.PlayStatsContract

/** All SQL touching `play_stats` (history + play counts). Reads of it are joins in the track / album queries. */
class PlayStatsQueries(private val db: SQLiteDatabase) {
    /**
     * Counts one play and moves the track to the top of the history. The stored time is forced strictly above every
     * existing one, so plays within the same millisecond keep their order. (No UPSERT: SQLite on API 26 is 3.18.)
     */
    fun recordPlay(trackId: String, nowMs: Long) {
        db.beginTransaction()
        try {
            db.execSQL(
                "INSERT OR IGNORE INTO " + PlayStatsContract.TABLE + " (" + PlayStatsContract.COL_TRACK_ID + ", " +
                        PlayStatsContract.COL_PLAY_COUNT + ", " + PlayStatsContract.COL_LAST_PLAYED_AT +
                        ") VALUES (?, 0, ?)",
                arrayOf<Any>(trackId, nowMs)
            )
            db.execSQL(
                "UPDATE " + PlayStatsContract.TABLE + " SET " + PlayStatsContract.COL_PLAY_COUNT + " = " +
                        PlayStatsContract.COL_PLAY_COUNT + " + 1, " + PlayStatsContract.COL_LAST_PLAYED_AT + " = MAX(?, (SELECT MAX(" +
                        PlayStatsContract.COL_LAST_PLAYED_AT + ") FROM " + PlayStatsContract.TABLE +
                        ") + 1) WHERE " + PlayStatsContract.COL_TRACK_ID + " = ?",
                arrayOf<Any>(nowMs, trackId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

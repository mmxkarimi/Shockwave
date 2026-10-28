package ir.mmxkarimi.shockwave.data.db.query

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.LikesContract

/** All SQL touching the `likes` table. Call off the main thread. Example of the query-class pattern. */
class LikeQueries(private val db: SQLiteDatabase) {
    fun allTrackIds(): Set<String> {
        val ids = HashSet<String>()
        db.query(
            LikesContract.TABLE, arrayOf(LikesContract.COL_TRACK_ID),
            null, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) ids.add(c.getString(0))
        }
        return ids
    }

    /** Likes the track, or removes the like if present. Returns the new state (true = liked). */
    fun toggle(trackId: String, nowMs: Long): Boolean {
        val removed = db.delete(LikesContract.TABLE, LikesContract.COL_TRACK_ID + " = ?", arrayOf(trackId))
        if (removed > 0) return false
        val values = ContentValues()
        values.put(LikesContract.COL_TRACK_ID, trackId)
        values.put(LikesContract.COL_LIKED_AT, nowMs)
        db.insertWithOnConflict(LikesContract.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return true
    }
}

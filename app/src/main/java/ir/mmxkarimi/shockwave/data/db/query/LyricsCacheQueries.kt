package ir.mmxkarimi.shockwave.data.db.query

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import ir.mmxkarimi.shockwave.data.db.contract.LyricsCacheContract

/** All SQL touching `lyrics_cache`. Call off the main thread. */
class LyricsCacheQueries(private val db: SQLiteDatabase) {
    /** @param content raw LRC / plain text; null = cached "no lyrics" */
    data class Entry(
        @get:JvmName("content") val content: String?,
        @get:JvmName("source") val source: String?,
        @get:JvmName("expiresAtMs") val expiresAtMs: Long,
    ) {
        // Record-style accessors, callable from Kotlin as functions; JVM names differ from the property getters.
        @JvmName("contentComponent") fun content(): String? = content
        @JvmName("sourceComponent") fun source(): String? = source
        @JvmName("expiresAtMsComponent") fun expiresAtMs(): Long = expiresAtMs
    }

    /** Null when nothing is cached for the track (expired rows are returned too: the caller judges expiry). */
    fun get(trackId: String): Entry? {
        db.query(
            LyricsCacheContract.TABLE, arrayOf(LyricsCacheContract.COL_CONTENT, LyricsCacheContract.COL_SOURCE, LyricsCacheContract.COL_EXPIRES_AT),
            LyricsCacheContract.COL_TRACK_ID + " = ?", arrayOf(trackId), null, null, null
        ).use { c ->
            if (c.moveToFirst()) return Entry(c.getString(0), c.getString(1), c.getLong(2))
        }
        return null
    }

    fun put(trackId: String, entry: Entry) {
        val v = ContentValues()
        v.put(LyricsCacheContract.COL_TRACK_ID, trackId)
        v.put(LyricsCacheContract.COL_CONTENT, entry.content)
        v.put(LyricsCacheContract.COL_SOURCE, entry.source)
        v.put(LyricsCacheContract.COL_EXPIRES_AT, entry.expiresAtMs)
        db.insertWithOnConflict(LyricsCacheContract.TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun delete(trackId: String) {
        db.delete(LyricsCacheContract.TABLE, LyricsCacheContract.COL_TRACK_ID + " = ?", arrayOf(trackId))
    }

    /** Drops every row that expired at or before `nowMs`. */
    fun deleteExpired(nowMs: Long) {
        db.delete(LyricsCacheContract.TABLE, LyricsCacheContract.COL_EXPIRES_AT + " <= ?", arrayOf(nowMs.toString()))
    }
}

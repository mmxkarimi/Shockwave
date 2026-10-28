package ir.mmxkarimi.shockwave.data.db.contract

/**
 * Cache of network lyric lookups, one row per track. A row is valid until [COL_EXPIRES_AT]; a "not found" or
 * failed lookup is stored too ([COL_CONTENT] null) so the network is asked once per period.
 */
class LyricsCacheContract private constructor() {
    companion object {
        const val TABLE = "lyrics_cache"
        const val COL_TRACK_ID = "track_id"

        /** Raw LRC / plain text; null = cached "no lyrics". */
        const val COL_CONTENT = "content"

        const val COL_SOURCE = "source"
        const val COL_EXPIRES_AT = "expires_at"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_TRACK_ID + " TEXT PRIMARY KEY NOT NULL, " +
                COL_CONTENT + " TEXT, " +
                COL_SOURCE + " TEXT, " +
                COL_EXPIRES_AT + " INTEGER NOT NULL)"

        @JvmField
        val SCHEMA: Array<String> = arrayOf(CREATE)

        /** v2 -> v3 replaced this table's columns; it only holds re-fetchable data, so it is dropped and recreated. */
        const val DROP = "DROP TABLE IF EXISTS " + TABLE
    }
}

package ir.mmxkarimi.shockwave.data.db.contract

/** Play history and play counts in one row per track: history = rows ordered by last play, counts drive Home. */
class PlayStatsContract private constructor() {
    companion object {
        const val TABLE = "play_stats"
        const val COL_TRACK_ID = "track_id"
        const val COL_PLAY_COUNT = "play_count"
        const val COL_LAST_PLAYED_AT = "last_played_at"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_TRACK_ID + " TEXT PRIMARY KEY NOT NULL, " +
                COL_PLAY_COUNT + " INTEGER NOT NULL, " +
                COL_LAST_PLAYED_AT + " INTEGER NOT NULL)"

        @JvmField
        val SCHEMA: Array<String> = arrayOf(
            CREATE,
            "CREATE INDEX idx_play_stats_last ON " + TABLE + " (" + COL_LAST_PLAYED_AT + ")"
        )
    }
}

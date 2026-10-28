package ir.mmxkarimi.shockwave.data.db.contract

/**
 * Ordered membership of tracks in playlists. A track appears at most once per playlist. There is deliberately no
 * foreign key to `tracks`: a track that disappears from the library (SD card removed) keeps its slot and is
 * simply hidden by the inner join until it is back.
 */
class PlaylistTracksContract private constructor() {
    companion object {
        const val TABLE = "playlist_tracks"
        const val COL_PLAYLIST_ID = "playlist_id"
        const val COL_TRACK_ID = "track_id"
        const val COL_POSITION = "position"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_PLAYLIST_ID + " INTEGER NOT NULL REFERENCES " + PlaylistsContract.TABLE +
                "(" + PlaylistsContract.COL_ID + ") ON DELETE CASCADE, " +
                COL_TRACK_ID + " TEXT NOT NULL, " +
                COL_POSITION + " INTEGER NOT NULL, " +
                "PRIMARY KEY (" + COL_PLAYLIST_ID + ", " + COL_TRACK_ID + "))"

        @JvmField
        val SCHEMA: Array<String> = arrayOf(
            CREATE,
            "CREATE INDEX idx_playlist_tracks_order ON " + TABLE + " (" + COL_PLAYLIST_ID + ", " + COL_POSITION + ")"
        )
    }
}

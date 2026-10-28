package ir.mmxkarimi.shockwave.data.db.contract

/**
 * Table and column names of the scanned track library. Albums and artists are not tables: they are derived with
 * `GROUP BY` over this table (see `AlbumQueries` / `ArtistQueries`).
 * [COL_TRACK_NUMBER] stores MediaStore's raw value (disc * 1000 + track) so ordering respects discs.
 */
class TracksContract private constructor() {
    companion object {
        const val TABLE = "tracks"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_ARTIST_ID = "artist_id"
        const val COL_ARTIST_NAME = "artist_name"
        const val COL_ALBUM_ID = "album_id"
        const val COL_ALBUM_TITLE = "album_title"
        const val COL_DURATION_MS = "duration_ms"
        const val COL_ARTWORK_URI = "artwork_uri"
        const val COL_MEDIA_URI = "media_uri"
        const val COL_YEAR = "year"
        const val COL_TRACK_NUMBER = "track_number"
        const val COL_ADDED_AT_MS = "added_at_ms"

        /** File modification time (ms); the scanner re-reads a row only when this changes, waveform cache keys on it. */
        const val COL_MODIFIED_MS = "modified_ms"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_ID + " TEXT PRIMARY KEY NOT NULL, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_ARTIST_ID + " TEXT NOT NULL, " +
                COL_ARTIST_NAME + " TEXT NOT NULL, " +
                COL_ALBUM_ID + " TEXT NOT NULL, " +
                COL_ALBUM_TITLE + " TEXT NOT NULL, " +
                COL_DURATION_MS + " INTEGER NOT NULL, " +
                COL_ARTWORK_URI + " TEXT, " +
                COL_MEDIA_URI + " TEXT NOT NULL, " +
                COL_YEAR + " INTEGER NOT NULL DEFAULT 0, " +
                COL_TRACK_NUMBER + " INTEGER NOT NULL DEFAULT 0, " +
                COL_ADDED_AT_MS + " INTEGER NOT NULL, " +
                COL_MODIFIED_MS + " INTEGER NOT NULL)"

        /** Table plus the indices the album / artist / recently-added queries use. */
        @JvmField
        val SCHEMA: Array<String> = arrayOf(
            CREATE,
            "CREATE INDEX idx_tracks_album ON " + TABLE + " (" + COL_ALBUM_ID + ")",
            "CREATE INDEX idx_tracks_artist ON " + TABLE + " (" + COL_ARTIST_ID + ")",
            "CREATE INDEX idx_tracks_added ON " + TABLE + " (" + COL_ADDED_AT_MS + ")"
        )
    }
}

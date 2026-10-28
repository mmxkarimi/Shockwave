package ir.mmxkarimi.shockwave.data.db.contract

/** Table and column names of user playlists. The id is the SQLite rowid, exposed to the app as a string. */
class PlaylistsContract private constructor() {
    companion object {
        const val TABLE = "playlists"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_DESCRIPTION = "description"
        const val COL_CREATED_AT = "created_at"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT NOT NULL, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_CREATED_AT + " INTEGER NOT NULL)"

        @JvmField
        val SCHEMA: Array<String> = arrayOf(CREATE)
    }
}

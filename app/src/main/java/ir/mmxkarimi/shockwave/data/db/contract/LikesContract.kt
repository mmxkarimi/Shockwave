package ir.mmxkarimi.shockwave.data.db.contract

/** Table and column names of liked tracks. Example of the contract pattern: one final class per table. */
class LikesContract private constructor() {
    companion object {
        const val TABLE = "likes"
        const val COL_TRACK_ID = "track_id"
        const val COL_LIKED_AT = "liked_at"

        const val CREATE = "CREATE TABLE " + TABLE + " (" +
                COL_TRACK_ID + " TEXT PRIMARY KEY NOT NULL, " +
                COL_LIKED_AT + " INTEGER NOT NULL)"
    }
}

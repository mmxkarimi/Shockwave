package ir.mmxkarimi.shockwave.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ir.mmxkarimi.shockwave.data.db.contract.LikesContract
import ir.mmxkarimi.shockwave.data.db.contract.LyricsCacheContract
import ir.mmxkarimi.shockwave.data.db.contract.PlayStatsContract
import ir.mmxkarimi.shockwave.data.db.contract.PlaylistTracksContract
import ir.mmxkarimi.shockwave.data.db.contract.PlaylistsContract
import ir.mmxkarimi.shockwave.data.db.contract.TracksContract
import ir.mmxkarimi.shockwave.data.db.contract.WaveformsContract

/**
 * The app's single SQLite database (framework SQLite, no Room). Schema statements live in the
 * `contract` classes, queries in the `query` classes; this class only wires them to the lifecycle.
 * Bump [VERSION] and add a migration step in [onUpgrade] whenever a contract changes.
 *
 * History: v1 = likes only (scaffold), v2 = library, playlists, play stats, waveform + lyrics caches,
 * v3 = lyrics cache keyed by expiry instead of fetch time.
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(LikesContract.CREATE)
        createV2Tables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createV2Tables(db)
        if (oldVersion == 2) { // v1 databases just got the v3 shape from createV2Tables
            db.execSQL(LyricsCacheContract.DROP)
            for (sql in LyricsCacheContract.SCHEMA) db.execSQL(sql)
        }
    }

    /** Playlists must exist before playlist_tracks (foreign key). */
    private fun createV2Tables(db: SQLiteDatabase) {
        val schemas = arrayOf(
            TracksContract.SCHEMA, PlaylistsContract.SCHEMA, PlaylistTracksContract.SCHEMA,
            PlayStatsContract.SCHEMA, WaveformsContract.SCHEMA, LyricsCacheContract.SCHEMA
        )
        for (statements in schemas) {
            for (sql in statements) db.execSQL(sql)
        }
    }

    companion object {
        const val NAME = "shockwave.db"
        const val VERSION = 3
    }
}

package ir.mmxkarimi.shockwave.data.scanner

import android.content.Context
import android.database.Cursor
import android.os.Process
import android.provider.MediaStore
import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.db.query.TrackQueries
import ir.mmxkarimi.shockwave.data.db.query.WaveformQueries
import ir.mmxkarimi.shockwave.data.settings.SettingsStore
import ir.mmxkarimi.shockwave.domain.model.ScanResult

/**
 * Reads music from MediaStore into the `tracks` table: incremental (only new / modified rows are written, vanished
 * ids deleted), single transaction, low thread priority. Permission-agnostic: the caller ensures READ_MEDIA_AUDIO
 * (API 33+) or READ_EXTERNAL_STORAGE (<= 32); without it [scan] returns [ScanResult.FAILED] and leaves the
 * library untouched. The method blocks: call it from the io executor. Nothing here schedules itself (no polling).
 */
class MediaStoreScanner(
    context: Context,
    private val helper: DbHelper,
    private val settings: SettingsStore,
) {
    private val context: Context = context.applicationContext

    fun scan(): ScanResult {
        val oldPriority = Process.getThreadPriority(Process.myTid())
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        try {
            return scanInternal()
        } finally {
            Process.setThreadPriority(oldPriority)
        }
    }

    private fun scanInternal(): ScanResult {
        val db = helper.writableDatabase
        val tracks = TrackQueries(db)
        val diff = IncrementalDiff(tracks.modifiedById())
        val folders = FolderFilter(
            orEmpty(settings.includedFolders().value),
            orEmpty(settings.excludedFolders().value),
        )
        var added = 0
        var updated = 0
        var removed = 0
        db.beginTransaction()
        try {
            val c = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTION,
                SELECTION, arrayOf(MIN_DURATION_MS.toString()), null,
            )
            if (c == null) return ScanResult.FAILED
            c.use {
                while (it.moveToNext()) {
                    val audio = read(it)
                    if (!folders.accepts(audio.path)) continue
                    val change = diff.visit(audio.id.toString(), audio.modifiedMs)
                    if (change == IncrementalDiff.Change.UNCHANGED) continue
                    tracks.upsert(audio.toTrack(), audio.modifiedMs)
                    if (change == IncrementalDiff.Change.ADDED) added++ else updated++
                }
                removed = tracks.deleteByIds(diff.missingIds())
                WaveformQueries(db).deleteOrphans()
                db.setTransactionSuccessful()
            }
        } catch (noPermission: SecurityException) {
            return ScanResult.FAILED
        } finally {
            db.endTransaction()
        }
        return ScanResult(true, added, updated, removed)
    }

    companion object {
        /** Files shorter than this are ringtones, notifications, voice notes ... */
        internal const val MIN_DURATION_MS = 30_000L

        @Suppress("DEPRECATION") // DATA is the only column with the absolute path on API 26-28, still readable later
        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA,
        )
        private val SELECTION = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
            MediaStore.Audio.Media.DURATION + " >= ?"

        @JvmStatic private fun read(c: Cursor): ScannedAudio = ScannedAudio(
            c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4),
            c.getString(5), c.getLong(6), c.getLong(7), c.getInt(8), c.getInt(9),
            c.getLong(10) * 1000, c.getLong(11) * 1000, c.getString(12),
        )

        @JvmStatic private fun orEmpty(set: Set<String>?): Set<String> = set ?: emptySet()
    }
}

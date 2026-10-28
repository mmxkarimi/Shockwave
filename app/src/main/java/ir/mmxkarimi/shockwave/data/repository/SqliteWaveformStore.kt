package ir.mmxkarimi.shockwave.data.repository

import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.db.query.WaveformQueries
import ir.mmxkarimi.shockwave.data.waveform.WaveformMath
import ir.mmxkarimi.shockwave.data.waveform.WaveformStore

/**
 * SQLite-backed [WaveformStore]: one 256-bucket master envelope per track (row key = track id +
 * [WaveformMath.MASTER_BUCKETS]). The blob codec is identical to `WaveformMath.encode`, so the bytes pass
 * through untouched; an empty blob is the negative cache. Validity is judged in SQL against the track's current
 * `modified_ms` (a rescan that sees an edited file invalidates the row), so the `fingerprint` argument is
 * not stored. Blocks on the database: background thread only.
 */
class SqliteWaveformStore(private val helper: DbHelper) : WaveformStore {

    override fun get(trackId: String, fingerprint: String): ByteArray? =
        WaveformQueries(helper.writableDatabase).get(trackId, WaveformMath.MASTER_BUCKETS)

    /** Ignored if the track is not in the library (the SQL inserts only for known tracks). */
    override fun put(trackId: String, fingerprint: String, blob: ByteArray) {
        WaveformQueries(helper.writableDatabase).put(trackId, WaveformMath.MASTER_BUCKETS, blob)
    }
}

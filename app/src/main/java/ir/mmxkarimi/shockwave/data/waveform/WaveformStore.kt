package ir.mmxkarimi.shockwave.data.waveform

/**
 * Persistent cache of master envelopes: one compact blob per track ([WaveformMath.MASTER_BUCKETS] bytes, one per
 * bar). Implementations may block (disk / SQLite); the repository only calls them off the main thread.
 *
 * The `fingerprint` (file size + modified time + duration) lets an implementation drop a stale blob after the
 * file changed: [put] replaces whatever is stored for the track id (so a database can use track id as primary
 * key), [get] returns null when nothing is stored OR the stored fingerprint differs.
 *
 * An empty array means "this track cannot be decoded, do not try again" (negative cache).
 */
interface WaveformStore {
    fun get(trackId: String, fingerprint: String): ByteArray?

    fun put(trackId: String, fingerprint: String, blob: ByteArray)
}

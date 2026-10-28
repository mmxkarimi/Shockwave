package ir.mmxkarimi.shockwave.data.lyrics

/**
 * Persistent cache of network lyric lookups, keyed by track id. The data layer binds this to the `lyrics_cache`
 * SQLite table (via `LyricsCacheQueries`); [InMemoryLyricsCacheStore] serves tests. Called on a
 * background thread only. Expiry is judged by the caller from [Entry.expiresAtMs].
 */
interface LyricsCacheStore {

    /** @param text raw LRC/plain text; null is a cached "no lyrics" (negative result) */
    data class Entry(
        @get:JvmName("text") val text: String?,
        @get:JvmName("source") val source: String?,
        @get:JvmName("expiresAtMs") val expiresAtMs: Long,
    )

    fun get(trackId: String): Entry?

    fun put(trackId: String, entry: Entry)
}

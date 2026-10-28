package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LyricsCacheStore.Entry
import ir.mmxkarimi.shockwave.data.lyrics.LyricsSource.Found
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.IOException
import java.util.function.BooleanSupplier
import java.util.function.LongSupplier

/**
 * The source-selection policy, blocking (background thread only):
 * <ol>
 *   <li>local sources in order (embedded tag, sidecar file) - cheap, never cached, so edits show at once;</li>
 *   <li>the lookup cache, then the remote source (LRCLIB); every remote outcome is cached, including "not found"
 *       (days) and failures (minutes) so a track never causes repeated requests.</li>
 * </ol>
 * When `remoteEnabled` is false (the user's "lyrics online" setting) a valid cache entry is still used but no request
 * is made and nothing is cached, so switching it back on works at once.
 *
 * Synced beats plain: a local plain-text result is kept as fallback and only replaced by a synced remote result.
 * The first synced result wins immediately. Returns null when cancelled.
 *
 * @param remoteEnabled read on the resolving thread for every lookup
 */
class LyricsResolver(
    private val local: List<LyricsSource>,
    private val remote: LyricsSource,
    private val cache: LyricsCacheStore,
    private val clock: LongSupplier,
    private val remoteEnabled: BooleanSupplier,
) {

    constructor(local: List<LyricsSource>, remote: LyricsSource, cache: LyricsCacheStore, clock: LongSupplier) :
        this(local, remote, cache, clock, BooleanSupplier { true })

    /** @return the lyrics or [Lyrics.none]; null if `cancel` fired */
    fun resolve(track: Track, cancel: CancelToken): Lyrics? {
        var plainFallback: Lyrics? = null
        for (source in local) {
            if (cancel.isCancelled()) return null
            val found = parse(track, tryLoad(source, track, cancel)) ?: continue
            if (found.synced) return found
            if (plainFallback == null) plainFallback = found
        }
        if (cancel.isCancelled()) return null
        val fromRemote = viaCache(track, cancel)
        if (cancel.isCancelled()) return null
        if (fromRemote != null && (fromRemote.synced || plainFallback == null)) return fromRemote
        return plainFallback ?: Lyrics.none(track.id)
    }

    private fun viaCache(track: Track, cancel: CancelToken): Lyrics? {
        val now = clock.getAsLong()
        val cached = cache.get(track.id)
        if (cached != null && cached.expiresAtMs > now) {
            return if (cached.text == null) null else LrcParser.toLyrics(track.id, cached.text, cached.source)
        }
        if (!remoteEnabled.getAsBoolean()) return null
        return try {
            val found = remote.load(track, cancel)
            if (cancel.isCancelled()) return null
            val lyrics = found?.let { LrcParser.toLyrics(track.id, it.text, it.source) }
            if (found == null || lyrics == null) {
                cache.put(track.id, Entry(null, null, now + NOT_FOUND_TTL_MS))
            } else {
                cache.put(track.id, Entry(found.text, found.source, now + FOUND_TTL_MS))
            }
            lyrics
        } catch (e: IOException) {
            if (!cancel.isCancelled()) cache.put(track.id, Entry(null, null, now + ERROR_TTL_MS))
            null
        }
    }

    companion object {
        internal const val FOUND_TTL_MS = 30L * 24 * 3600 * 1000
        internal const val NOT_FOUND_TTL_MS = 3L * 24 * 3600 * 1000
        internal const val ERROR_TTL_MS = 5L * 60 * 1000

        /** A broken local file must not stop the lookup: failure counts as "none here". */
        @JvmStatic private fun tryLoad(source: LyricsSource, track: Track, cancel: CancelToken): Found? {
            return try {
                source.load(track, cancel)
            } catch (e: IOException) {
                null
            } catch (e: RuntimeException) {
                null
            }
        }

        @JvmStatic private fun parse(track: Track, found: Found?): Lyrics? {
            return found?.let { LrcParser.toLyrics(track.id, it.text, it.source) }
        }
    }
}

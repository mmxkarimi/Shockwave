package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Download all lyrics" must look up tracks again that the cache remembers as "not found". */
class LyricsResolverBypassTest {
    private val track = Track(
        "42", "Song", "1", "Artist", "7", "Album", 180_000L, null, 0, "content://media/external/audio/media/42",
        false, false, 2020, 1, 0L,
    )
    private val now = 1_000_000L

    private class MapCache : LyricsCacheStore {
        val map = HashMap<String, LyricsCacheStore.Entry>()
        override fun get(trackId: String) = map[trackId]
        override fun put(trackId: String, entry: LyricsCacheStore.Entry) {
            map[trackId] = entry
        }
    }

    private class CountingRemote(private val text: String?) : LyricsSource {
        var calls = 0
        override fun load(track: Track, cancel: CancelToken): LyricsSource.Found? {
            calls++
            return text?.let { LyricsSource.Found(it, "LRCLIB") }
        }
    }

    private fun resolver(remote: LyricsSource, cache: LyricsCacheStore) =
        LyricsResolver(emptyList(), remote, cache, { now }, { true })

    @Test
    fun cachedMissIsTrustedByDefault() {
        val cache = MapCache().apply { put("42", LyricsCacheStore.Entry(null, null, now + 60_000)) }
        val remote = CountingRemote("[00:01.00]hello")
        val result = resolver(remote, cache).resolve(track, CancelToken())
        assertEquals(0, remote.calls)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun bypassLooksUpCachedMissAgainAndCachesTheHit() {
        val cache = MapCache().apply { put("42", LyricsCacheStore.Entry(null, null, now + 60_000)) }
        val remote = CountingRemote("[00:01.00]hello")
        val result = resolver(remote, cache).resolve(track, CancelToken(), bypassMissCache = true)
        assertEquals(1, remote.calls)
        assertFalse(result!!.isEmpty())
        assertEquals("[00:01.00]hello", cache.map["42"]!!.text)
    }

    @Test
    fun bypassStillReusesCachedLyrics() {
        val cache = MapCache().apply { put("42", LyricsCacheStore.Entry("[00:01.00]cached", "LRCLIB", now + 60_000)) }
        val remote = CountingRemote("[00:01.00]remote")
        val result = resolver(remote, cache).resolve(track, CancelToken(), bypassMissCache = true)
        assertEquals(0, remote.calls)
        assertFalse(result!!.isEmpty())
    }
}

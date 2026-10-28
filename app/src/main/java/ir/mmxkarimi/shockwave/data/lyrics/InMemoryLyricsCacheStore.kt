package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LyricsCacheStore.Entry
import java.util.concurrent.ConcurrentHashMap

/** Non-persistent [LyricsCacheStore] for tests and previews. */
class InMemoryLyricsCacheStore : LyricsCacheStore {
    private val map = ConcurrentHashMap<String, Entry>()

    override fun get(trackId: String): Entry? = map[trackId]

    override fun put(trackId: String, entry: Entry) {
        map[trackId] = entry
    }
}

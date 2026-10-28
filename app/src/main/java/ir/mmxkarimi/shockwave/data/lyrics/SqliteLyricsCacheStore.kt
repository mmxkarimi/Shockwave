package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.db.query.LyricsCacheQueries
import java.util.function.LongSupplier

/**
 * [LyricsCacheStore] on the `lyrics_cache` table. The database opens on the first call, which comes from
 * the lyrics thread. Every write also drops expired rows, so the table cannot grow without bound.
 */
class SqliteLyricsCacheStore(
    private val helper: DbHelper,
    private val clock: LongSupplier,
) : LyricsCacheStore {

    override fun get(trackId: String): LyricsCacheStore.Entry? {
        val row = queries().get(trackId) ?: return null
        return LyricsCacheStore.Entry(row.content, row.source, row.expiresAtMs)
    }

    override fun put(trackId: String, entry: LyricsCacheStore.Entry) {
        val queries = queries()
        queries.deleteExpired(clock.getAsLong())
        queries.put(trackId, LyricsCacheQueries.Entry(entry.text, entry.source, entry.expiresAtMs))
    }

    private fun queries(): LyricsCacheQueries = LyricsCacheQueries(helper.getWritableDatabase())
}

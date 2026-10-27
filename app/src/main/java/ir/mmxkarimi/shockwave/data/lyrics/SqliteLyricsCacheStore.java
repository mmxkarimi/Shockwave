package ir.mmxkarimi.shockwave.data.lyrics;

import androidx.annotation.Nullable;
import ir.mmxkarimi.shockwave.data.db.DbHelper;
import ir.mmxkarimi.shockwave.data.db.query.LyricsCacheQueries;
import java.util.function.LongSupplier;

/**
 * {@link LyricsCacheStore} on the {@code lyrics_cache} table. The database opens on the first call, which comes from
 * the lyrics thread. Every write also drops expired rows, so the table cannot grow without bound.
 */
public final class SqliteLyricsCacheStore implements LyricsCacheStore {
    private final DbHelper helper;
    private final LongSupplier clock;

    public SqliteLyricsCacheStore(DbHelper helper, LongSupplier clock) {
        this.helper = helper;
        this.clock = clock;
    }

    @Nullable @Override public Entry get(String trackId) {
        LyricsCacheQueries.Entry row = queries().get(trackId);
        return row == null ? null : new Entry(row.content(), row.source(), row.expiresAtMs());
    }

    @Override public void put(String trackId, Entry entry) {
        LyricsCacheQueries queries = queries();
        queries.deleteExpired(clock.getAsLong());
        queries.put(trackId, new LyricsCacheQueries.Entry(entry.text(), entry.source(), entry.expiresAtMs()));
    }

    private LyricsCacheQueries queries() {
        return new LyricsCacheQueries(helper.getWritableDatabase());
    }
}

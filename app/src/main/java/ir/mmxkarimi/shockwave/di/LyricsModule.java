package ir.mmxkarimi.shockwave.di;

import android.content.Context;
import ir.mmxkarimi.shockwave.data.lyrics.AudioFileLocator;
import ir.mmxkarimi.shockwave.data.lyrics.EmbeddedLyricsSource;
import ir.mmxkarimi.shockwave.data.lyrics.LrclibClient;
import ir.mmxkarimi.shockwave.data.lyrics.LyricsCacheStore;
import ir.mmxkarimi.shockwave.data.lyrics.LyricsRepositoryImpl;
import ir.mmxkarimi.shockwave.data.lyrics.LyricsResolver;
import ir.mmxkarimi.shockwave.data.lyrics.MediaStoreFileLocator;
import ir.mmxkarimi.shockwave.data.lyrics.SidecarLyricsSource;
import ir.mmxkarimi.shockwave.data.lyrics.UrlConnectionFetcher;
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Builds the real {@link LyricsRepository} (embedded tag, sidecar file, LRCLIB, cache). {@code AppContainer} calls
 * {@link #create} once. Lyrics get their own single thread (exits after 10 s idle) so a slow HTTP call never blocks
 * the shared {@code ioExecutor}.
 */
public final class LyricsModule {
    private LyricsModule() { }

    /**
     * @param versionName app version, sent in the User-Agent as LRCLIB requests
     * @param cache       persistent cache bound to the {@code lyrics_cache} table
     * @param onlineAllowed the "lyrics online" setting, read per lookup on the lyrics thread; false = never call LRCLIB
     */
    public static LyricsRepository create(Context context, String versionName, LyricsCacheStore cache,
            BooleanSupplier onlineAllowed) {
        AudioFileLocator files = new MediaStoreFileLocator(context.getApplicationContext().getContentResolver());
        String userAgent = "Shockwave/" + versionName + " (https://github.com/mmxkarimi/Shockwave)";
        LyricsResolver resolver = new LyricsResolver(
                Arrays.asList(new EmbeddedLyricsSource(files), new SidecarLyricsSource(files)),
                new LrclibClient(new UrlConnectionFetcher(userAgent)),
                cache,
                System::currentTimeMillis,
                onlineAllowed);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        return new LyricsRepositoryImpl(resolver, executor);
    }
}

package ir.mmxkarimi.shockwave.di

import android.content.Context
import ir.mmxkarimi.shockwave.data.lyrics.AudioFileLocator
import ir.mmxkarimi.shockwave.data.lyrics.EmbeddedLyricsSource
import ir.mmxkarimi.shockwave.data.lyrics.LrclibClient
import ir.mmxkarimi.shockwave.data.lyrics.LyricsCacheStore
import ir.mmxkarimi.shockwave.data.lyrics.LyricsRepositoryImpl
import ir.mmxkarimi.shockwave.data.lyrics.LyricsResolver
import ir.mmxkarimi.shockwave.data.lyrics.MediaStoreFileLocator
import ir.mmxkarimi.shockwave.data.lyrics.SidecarLyricsSource
import ir.mmxkarimi.shockwave.data.lyrics.UrlConnectionFetcher
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier

/**
 * Builds the real [LyricsRepository] (embedded tag, sidecar file, LRCLIB, cache). `AppContainer` calls
 * [create] once. Lyrics get their own single thread (exits after 10 s idle) so a slow HTTP call never blocks
 * the shared `ioExecutor`.
 */
class LyricsModule private constructor() {
    companion object {
        /**
         * @param versionName app version, sent in the User-Agent as LRCLIB requests
         * @param cache       persistent cache bound to the `lyrics_cache` table
         * @param onlineAllowed the "lyrics online" setting, read per lookup on the lyrics thread; false = never call LRCLIB
         */
        @JvmStatic fun create(
            context: Context,
            versionName: String,
            cache: LyricsCacheStore,
            onlineAllowed: BooleanSupplier,
        ): LyricsRepository {
            val files: AudioFileLocator = MediaStoreFileLocator(context.applicationContext.contentResolver)
            val userAgent = "Shockwave/$versionName (https://github.com/mmxkarimi/Shockwave)"
            val resolver = LyricsResolver(
                listOf(EmbeddedLyricsSource(files), SidecarLyricsSource(files)),
                LrclibClient(UrlConnectionFetcher(userAgent)),
                cache,
                System::currentTimeMillis,
                onlineAllowed,
            )
            val executor = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue())
            executor.allowCoreThreadTimeOut(true)
            return LyricsRepositoryImpl(resolver, executor)
        }
    }
}

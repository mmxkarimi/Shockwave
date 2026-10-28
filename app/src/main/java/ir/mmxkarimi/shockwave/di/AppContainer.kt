package ir.mmxkarimi.shockwave.di

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import ir.mmxkarimi.shockwave.BuildConfig
import ir.mmxkarimi.shockwave.data.lyrics.SqliteLyricsCacheStore
import ir.mmxkarimi.shockwave.data.settings.SettingsStore
import ir.mmxkarimi.shockwave.domain.repository.HomeFeedSource
import ir.mmxkarimi.shockwave.domain.repository.LibraryAdmin
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository
import ir.mmxkarimi.shockwave.domain.repository.MusicSource
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.domain.repository.WaveformRepository
import ir.mmxkarimi.shockwave.fake.FakeHomeFeedSource
import ir.mmxkarimi.shockwave.fake.FakeLibraryRepository
import ir.mmxkarimi.shockwave.fake.FakeLyricsRepository
import ir.mmxkarimi.shockwave.fake.FakeMusicSource
import ir.mmxkarimi.shockwave.fake.FakePlayerController
import ir.mmxkarimi.shockwave.fake.FakeWaveformRepository
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hand-written dependency container (plain Java DI), created once by `ShockwaveApp`. Screens never construct
 * repositories; they read them from here.
 *
 * Real implementations by default; [USE_FAKES] (the single switch) or the two-argument constructor binds the
 * in-memory fakes instead (debug galleries, tests). Construction is cheap: no database, MediaStore or network access
 * happens until something is observed or called; the only eager piece is the player, which must exist when the
 * process is started by a media button.
 */
class AppContainer {
    companion object {
        /** The one place that decides real vs fake for the running app. */
        const val USE_FAKES = false
    }

    /** Shared background thread for disk / DB / network work. Its thread exits after 30 s idle (battery, RAM). */
    @JvmField val ioExecutor: Executor
    @JvmField val library: LibraryRepository
    @JvmField val player: PlayerController
    @JvmField val lyrics: LyricsRepository
    @JvmField val waveform: WaveformRepository
    @JvmField val homeFeed: HomeFeedSource

    /** Always the fake: there is no online catalog in Phase 1. */
    @JvmField val musicSource: MusicSource

    private val appContext: Context
    private val useFakes: Boolean
    private val settings: SettingsStore
    private val libraryAdmin: LibraryAdmin
    private val foregroundScanDone = AtomicBoolean()

    constructor(context: Context) : this(context, USE_FAKES)

    /** @param context any context (the application context is kept) */
    constructor(context: Context, useFakes: Boolean) {
        appContext = context.applicationContext
        this.useFakes = useFakes
        val io = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
        io.allowCoreThreadTimeOut(true)
        ioExecutor = io
        settings = SettingsStore(appContext)
        musicSource = FakeMusicSource()

        if (useFakes) {
            val fake = FakeLibraryRepository()
            library = fake
            libraryAdmin = fake
            player = FakePlayerController()
            lyrics = FakeLyricsRepository()
            waveform = FakeWaveformRepository()
            homeFeed = FakeHomeFeedSource(library)
        } else {
            val data = DataModule.create(appContext, io, settings)
            library = data.library
            libraryAdmin = data.library
            homeFeed = data.homeFeed
            waveform = WaveformModule.create(appContext, data.waveformStore, this::batterySaverActive)
            // Wavy is the only seek style, which reads no envelope: nothing to prefetch for it.
            player = PlaybackModule.createController(appContext, data.library::recordPlay, null)
            lyrics = LyricsModule.create(
                appContext,
                BuildConfig.VERSION_NAME,
                SqliteLyricsCacheStore(data.dbHelper, System::currentTimeMillis),
                { !java.lang.Boolean.FALSE.equals(settings.lyricsOnline().value) },
            )
        }
    }

    /** Settings are real in both modes (SharedPreferences only). */
    fun settings(): SettingsStore {
        return settings
    }

    /** Scanning and the extra playlist edits (rename, delete, remove, reorder). */
    fun libraryAdmin(): LibraryAdmin {
        return libraryAdmin
    }

    /** Call when the user grants the audio permission: runs the first scan (or a fresh one). */
    fun onAudioPermissionGranted() {
        foregroundScanDone.set(true)
        libraryAdmin.rescan(null)
    }

    /** Call when the first activity of this process starts: refreshes the music index once per launch. */
    fun onAppForeground() {
        if (!useFakes && hasAudioPermission() && foregroundScanDone.compareAndSet(false, true)) {
            libraryAdmin.rescan(null)
        }
    }

    /** The user's battery-saver setting combined with the system's; read on any thread. */
    private fun batterySaverActive(): Boolean {
        val power = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
        return settings.batterySaver().value!!.active(power != null && power.isPowerSaveMode())
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}

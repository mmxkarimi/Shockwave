package ir.mmxkarimi.shockwave.di

import android.content.Context
import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.repository.ChangeNotifier
import ir.mmxkarimi.shockwave.data.repository.LocalHomeFeedSource
import ir.mmxkarimi.shockwave.data.repository.SqliteLibraryRepository
import ir.mmxkarimi.shockwave.data.repository.SqliteWaveformStore
import ir.mmxkarimi.shockwave.data.scanner.MediaStoreScanner
import ir.mmxkarimi.shockwave.data.settings.SettingsStore
import java.util.concurrent.Executor

/**
 * Builds the whole data layer from a Context and the io executor (which must run tasks one at a time, like
 * `AppContainer.ioExecutor`). Nothing here touches the database or MediaStore until something is observed
 * or called, so creating it on the main thread at startup is cheap.
 */
class DataModule private constructor(context: Context, io: Executor, settings: SettingsStore) {
    @JvmField val dbHelper: DbHelper
    @JvmField val settings: SettingsStore
    @JvmField val scanner: MediaStoreScanner

    /** Also the entry point for `rescan` and the extra playlist edits. */
    @JvmField val library: SqliteLibraryRepository
    @JvmField val homeFeed: LocalHomeFeedSource

    /** Persistent waveform cache, ready to hand to `WaveformModule` (blocking calls, background thread). */
    @JvmField val waveformStore: SqliteWaveformStore

    init {
        dbHelper = DbHelper(context)
        this.settings = settings
        scanner = MediaStoreScanner(context, dbHelper, settings)
        val notifier = ChangeNotifier()
        library = SqliteLibraryRepository(dbHelper, io, notifier, scanner)
        homeFeed = LocalHomeFeedSource(dbHelper, io, notifier)
        waveformStore = SqliteWaveformStore(dbHelper)
    }

    companion object {
        /** @param settings shared with the rest of the app (the scanner reads the folder filters from it) */
        @JvmStatic fun create(context: Context, io: Executor, settings: SettingsStore): DataModule {
            return DataModule(context, io, settings)
        }
    }
}

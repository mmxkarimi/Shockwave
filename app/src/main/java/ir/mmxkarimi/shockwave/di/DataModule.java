package ir.mmxkarimi.shockwave.di;

import android.content.Context;
import ir.mmxkarimi.shockwave.data.db.DbHelper;
import ir.mmxkarimi.shockwave.data.repository.ChangeNotifier;
import ir.mmxkarimi.shockwave.data.repository.LocalHomeFeedSource;
import ir.mmxkarimi.shockwave.data.repository.SqliteLibraryRepository;
import ir.mmxkarimi.shockwave.data.repository.SqliteWaveformStore;
import ir.mmxkarimi.shockwave.data.scanner.MediaStoreScanner;
import ir.mmxkarimi.shockwave.data.settings.SettingsStore;
import java.util.concurrent.Executor;

/**
 * Builds the whole data layer from a Context and the io executor (which must run tasks one at a time, like
 * {@code AppContainer.ioExecutor}). Nothing here touches the database or MediaStore until something is observed
 * or called, so creating it on the main thread at startup is cheap.
 */
public final class DataModule {
    public final DbHelper dbHelper;
    public final SettingsStore settings;
    public final MediaStoreScanner scanner;
    /** Also the entry point for {@code rescan} and the extra playlist edits. */
    public final SqliteLibraryRepository library;
    public final LocalHomeFeedSource homeFeed;
    /** Persistent waveform cache, ready to hand to {@code WaveformModule} (blocking calls, background thread). */
    public final SqliteWaveformStore waveformStore;

    private DataModule(Context context, Executor io, SettingsStore settings) {
        dbHelper = new DbHelper(context);
        this.settings = settings;
        scanner = new MediaStoreScanner(context, dbHelper, settings);
        ChangeNotifier notifier = new ChangeNotifier();
        library = new SqliteLibraryRepository(dbHelper, io, notifier, scanner);
        homeFeed = new LocalHomeFeedSource(dbHelper, io, notifier);
        waveformStore = new SqliteWaveformStore(dbHelper);
    }

    /** @param settings shared with the rest of the app (the scanner reads the folder filters from it) */
    public static DataModule create(Context context, Executor io, SettingsStore settings) {
        return new DataModule(context, io, settings);
    }
}

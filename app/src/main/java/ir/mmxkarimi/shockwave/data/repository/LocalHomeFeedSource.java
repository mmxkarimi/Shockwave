package ir.mmxkarimi.shockwave.data.repository;

import androidx.lifecycle.LiveData;
import ir.mmxkarimi.shockwave.data.db.DbHelper;
import ir.mmxkarimi.shockwave.data.db.query.AlbumQueries;
import ir.mmxkarimi.shockwave.data.db.query.PlaylistQueries;
import ir.mmxkarimi.shockwave.data.db.query.TrackQueries;
import ir.mmxkarimi.shockwave.domain.model.HomeSection;
import ir.mmxkarimi.shockwave.domain.repository.HomeFeedSource;
import java.util.List;
import java.util.concurrent.Executor;

/** Home sections built from the local library and listening history (rules in {@link HomeRanking}). */
public final class LocalHomeFeedSource implements HomeFeedSource {
    private static final int QUICK_PICK_CANDIDATES = 100;

    private final LiveData<List<HomeSection>> sections;

    public LocalHomeFeedSource(DbHelper helper, Executor io, ChangeNotifier notifier) {
        sections = new DbLiveData<>(io, notifier, ChangeNotifier.ALL, () -> {
            var db = helper.getWritableDatabase();
            return HomeRanking.build(new AlbumQueries(db).stats(),
                    new TrackQueries(db).quickPickCandidates(QUICK_PICK_CANDIDATES),
                    new PlaylistQueries(db).all(), System.currentTimeMillis());
        });
    }

    @Override public LiveData<List<HomeSection>> getSections() {
        return sections;
    }
}

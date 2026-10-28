package ir.mmxkarimi.shockwave.data.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.data.db.DbHelper
import ir.mmxkarimi.shockwave.data.db.query.AlbumQueries
import ir.mmxkarimi.shockwave.data.db.query.PlaylistQueries
import ir.mmxkarimi.shockwave.data.db.query.TrackQueries
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.repository.HomeFeedSource
import java.util.concurrent.Executor

/** Home sections built from the local library and listening history (rules in [HomeRanking]). */
class LocalHomeFeedSource(
    helper: DbHelper,
    io: Executor,
    notifier: ChangeNotifier,
) : HomeFeedSource {
    private val sections: LiveData<List<HomeSection>> = DbLiveData(io, notifier, ChangeNotifier.ALL) {
        val db = helper.writableDatabase
        HomeRanking.build(
            AlbumQueries(db).stats(),
            TrackQueries(db).quickPickCandidates(QUICK_PICK_CANDIDATES),
            PlaylistQueries(db).all(),
            System.currentTimeMillis(),
        )
    }

    override fun getSections(): LiveData<List<HomeSection>> = sections

    companion object {
        private const val QUICK_PICK_CANDIDATES = 100
    }
}

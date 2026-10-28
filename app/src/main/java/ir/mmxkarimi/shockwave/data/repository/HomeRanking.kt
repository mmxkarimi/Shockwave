package ir.mmxkarimi.shockwave.data.repository

import ir.mmxkarimi.shockwave.data.db.query.AlbumQueries.AlbumStat
import ir.mmxkarimi.shockwave.data.db.query.TrackQueries.TrackStat
import ir.mmxkarimi.shockwave.domain.model.HomeItem
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.model.HomeSectionStyle
import ir.mmxkarimi.shockwave.domain.model.Playlist

/** Pure Home-feed rules: what goes into each section and in which order. Empty sections are omitted. */
class HomeRanking private constructor() {

    companion object {
        internal const val HERO_COUNT = 8
        internal const val THROWBACK_COUNT = 10
        internal const val QUICK_PICK_COUNT = 8

        /** An album counts as "not played lately" after this long. */
        internal const val STALE_MS = 30L * 24 * 60 * 60 * 1000

        private const val LIKE_WEIGHT = 3

        @JvmStatic fun build(
            albums: List<AlbumStat>,
            tracks: List<TrackStat>,
            playlists: List<Playlist>,
            nowMs: Long,
        ): List<HomeSection> {
            val hero = albums.sortedWith(compareByDescending<AlbumStat> { it.newestAddedMs() }).take(HERO_COUNT)
            val heroIds = HashSet<String>()
            for (a in hero) heroIds.add(a.album().id)

            // Throwbacks: not already in the hero row, unplayed or not played for a month; least played, then oldest first.
            val throwbacks = albums
                .filter { !heroIds.contains(it.album().id) }
                .filter { it.lastPlayedMs() == 0L || nowMs - it.lastPlayedMs() > STALE_MS }
                .sortedWith(compareBy<AlbumStat> { it.plays() }.thenBy { it.newestAddedMs() })
                .take(THROWBACK_COUNT)

            val picks = tracks
                .filter { score(it) > 0 }
                .sortedWith(compareByDescending<TrackStat> { score(it) }.thenByDescending { it.lastPlayedMs() })
                .take(QUICK_PICK_COUNT)

            val out = ArrayList<HomeSection>()
            out.add(
                HomeSection(
                    "new-releases", "Recently added", "Fresh in your library",
                    HomeSectionStyle.HERO_CAROUSEL, albumItems(hero),
                )
            )
            out.add(
                HomeSection(
                    "throwbacks", "Throwbacks", "Albums you haven't played in a while",
                    HomeSectionStyle.CAROUSEL, albumItems(throwbacks),
                )
            )
            out.add(
                HomeSection(
                    "playlists", "Your playlists", null, HomeSectionStyle.CAROUSEL,
                    playlists.map { HomeItem.of(it) },
                )
            )
            out.add(
                HomeSection(
                    "quick-picks", "Quick picks", "Your most played and liked",
                    HomeSectionStyle.LIST, picks.map { HomeItem.of(it.track()) },
                )
            )
            out.removeAll { it.items.isEmpty() }
            return out
        }

        /** Plays count one point each; a like is worth three plays. */
        @JvmStatic internal fun score(t: TrackStat): Int = t.plays() + if (t.track().liked) LIKE_WEIGHT else 0

        @JvmStatic private fun albumItems(stats: List<AlbumStat>): List<HomeItem> {
            val items = ArrayList<HomeItem>()
            for (s in stats) {
                val album = s.album()
                items.add(HomeItem.of(album))
            }
            return items
        }
    }
}

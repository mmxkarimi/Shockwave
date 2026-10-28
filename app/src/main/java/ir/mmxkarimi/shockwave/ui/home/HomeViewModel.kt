package ir.mmxkarimi.shockwave.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.domain.model.HomeItem
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.HomeFeedSource
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import kotlin.math.max

/** Home feed. No Android UI classes; exposes LiveData only and talks to domain interfaces. */
class HomeViewModel(
    feed: HomeFeedSource,
    private val player: PlayerController
) : ViewModel() {
    private val sectionsLiveData: LiveData<List<HomeSection>> = feed.getSections()

    /** null until the first load finishes (the screen shows its loading state). */
    fun getSections(): LiveData<List<HomeSection>> = sectionsLiveData

    /** Plays the section's tracks starting at [track]. */
    fun playFromSection(section: HomeSection, track: Track) {
        val tracks = tracksOf(section)
        val index = max(0, tracks.indexOf(track))
        player.play(tracks, index, section.title)
    }

    fun shuffleSection(section: HomeSection) {
        val tracks = ArrayList(tracksOf(section))
        if (tracks.isEmpty()) return
        tracks.shuffle()
        player.play(tracks, 0, section.title)
    }

    companion object {
        @JvmStatic
        fun tracksOf(section: HomeSection): List<Track> {
            val out = mutableListOf<Track>()
            for (item in section.items) {
                if (item.kind == HomeItem.Kind.TRACK && item.track != null) {
                    out.add(item.track)
                }
            }
            return out
        }

        @JvmStatic
        fun factory(feed: HomeFeedSource, player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(feed, player) as T
                }
            }
        }
    }
}

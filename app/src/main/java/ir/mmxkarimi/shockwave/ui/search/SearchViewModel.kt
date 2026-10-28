package ir.mmxkarimi.shockwave.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import ir.mmxkarimi.shockwave.domain.model.SearchFilter
import ir.mmxkarimi.shockwave.domain.model.SearchResult
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.PlayerController

/** Search: query + filter chips -> results from the library. */
class SearchViewModel(
    library: LibraryRepository,
    private val player: PlayerController
) : ViewModel() {

    enum class Mode { PROMPT, RESULTS, NO_RESULTS }

    private data class Request(val query: String, val filter: SearchFilter)

    private val queryLiveData = MutableLiveData("")
    private val filterLiveData = MutableLiveData(SearchFilter.ALL)
    private val resultsLiveData: LiveData<List<SearchResult>>
    private val modeLiveData = MediatorLiveData<Mode>()

    init {
        val request = MediatorLiveData<Request>()
        request.addSource(queryLiveData) { q ->
            request.value = Request(q ?: "", filterLiveData.value ?: SearchFilter.ALL)
        }
        request.addSource(filterLiveData) { f ->
            request.value = Request(queryLiveData.value ?: "", f ?: SearchFilter.ALL)
        }
        resultsLiveData = request.distinctUntilChanged().switchMap { r ->
            library.search(r.query.trim(), r.filter)
        }
        modeLiveData.addSource(resultsLiveData) { list ->
            modeLiveData.value = modeFor(queryLiveData.value, list)
        }
    }

    fun getQuery(): LiveData<String> = queryLiveData
    fun getFilter(): LiveData<SearchFilter> = filterLiveData
    fun getResults(): LiveData<List<SearchResult>> = resultsLiveData
    fun getMode(): LiveData<Mode> = modeLiveData

    fun setQuery(q: String) {
        if (queryLiveData.value != q) {
            queryLiveData.value = q
            if (q.isBlank()) modeLiveData.value = Mode.PROMPT
        }
    }

    fun setFilter(f: SearchFilter) {
        if (filterLiveData.value != f) {
            filterLiveData.value = f
        }
    }

    fun playTrack(track: Track) {
        player.play(listOf(track), 0, null)
    }

    fun playTracks(tracks: List<Track>, index: Int, playingFrom: String?) {
        player.play(tracks, index, playingFrom)
    }

    companion object {
        @JvmStatic
        fun modeFor(q: String?, list: List<SearchResult>?): Mode {
            if (q.isNullOrBlank()) return Mode.PROMPT
            return if (list.isNullOrEmpty()) Mode.NO_RESULTS else Mode.RESULTS
        }

        @JvmStatic
        fun factory(library: LibraryRepository, player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(library, player) as T
                }
            }
        }
    }
}

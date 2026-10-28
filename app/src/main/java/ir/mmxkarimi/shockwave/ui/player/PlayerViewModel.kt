package ir.mmxkarimi.shockwave.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.RepeatMode
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.domain.repository.WaveformRepository
import java.util.Objects

/**
 * Full player screen state.
 */
class PlayerViewModel(
    private val player: PlayerController,
    private val library: LibraryRepository,
    private val lyricsRepo: LyricsRepository,
    private val waveformRepo: WaveformRepository
) : ViewModel() {

    private val playbackStateLiveData: LiveData<PlaybackState> = player.getState()
    private val currentTrackLiveData = MediatorLiveData<Track?>()
    private val lyricsLiveData: LiveData<Lyrics?>
    private val waveformLiveData: LiveData<FloatArray?>
    private val likedLiveData = MediatorLiveData<Boolean>()
    private val activeLyricLineLiveData = MediatorLiveData<Int>()
    private val showLyricsLiveData = MutableLiveData(false)
    private val lyricsRequest = MediatorLiveData<Track?>()
    private var likedIds: Set<String> = HashSet()
    private var likedLoaded = false

    init {
        currentTrackLiveData.addSource(playbackStateLiveData) { s ->
            val track = s?.currentTrack
            if (!Objects.equals(track, currentTrackLiveData.value)) {
                currentTrackLiveData.value = track
            }
        }
        lyricsRequest.addSource(currentTrackLiveData) { t -> lyricsRequest.value = t }
        lyricsLiveData = lyricsRequest.switchMap { t ->
            if (t == null) MutableLiveData<Lyrics?>(null) else lyricsRepo.lyricsFor(t)
        }
        waveformLiveData = currentTrackLiveData.switchMap { t ->
            if (t == null) MutableLiveData<FloatArray?>(null) else waveformRepo.waveformFor(t, WAVEFORM_BARS)
        }
        likedLiveData.addSource(currentTrackLiveData) { updateLiked() }
        likedLiveData.addSource(library.getLikedTracks()) { tracks ->
            val set = HashSet<String>()
            tracks?.forEach { set.add(it.id) }
            likedIds = set
            likedLoaded = true
            updateLiked()
        }
        activeLyricLineLiveData.addSource(playbackStateLiveData) { s ->
            updateActiveLine(s, lyricsLiveData.value)
        }
        activeLyricLineLiveData.addSource(lyricsLiveData) { lyr ->
            updateActiveLine(playbackStateLiveData.value, lyr)
        }
    }

    private fun updateLiked() {
        val t = currentTrackLiveData.value
        if (t == null) {
            likedLiveData.value = false
            return
        }
        likedLiveData.value = if (likedLoaded) likedIds.contains(t.id) else t.liked
    }

    private fun updateActiveLine(s: PlaybackState?, lyr: Lyrics?) {
        if (s == null || lyr == null || lyr.lines.isEmpty()) {
            activeLyricLineLiveData.value = -1
            return
        }
        val pos = s.positionMs
        val lines = lyr.lines
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= pos) active = i else break
        }
        if (activeLyricLineLiveData.value == null || activeLyricLineLiveData.value != active) {
            activeLyricLineLiveData.value = active
        }
    }

    fun getState(): LiveData<PlaybackState> = playbackStateLiveData
    fun getCurrentTrack(): LiveData<Track?> = currentTrackLiveData
    fun getLyrics(): LiveData<Lyrics?> = lyricsLiveData
    fun getWaveform(): LiveData<FloatArray?> = waveformLiveData
    fun getLiked(): LiveData<Boolean> = likedLiveData
    fun getActiveLyricLine(): LiveData<Int> = activeLyricLineLiveData
    fun getShowLyrics(): LiveData<Boolean> = showLyricsLiveData

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeatMode() = player.cycleRepeat()

    fun toggleLike() {
        val t = currentTrackLiveData.value ?: return
        library.toggleLike(t.id)
    }

    fun toggleLyrics() {
        showLyricsLiveData.value = showLyricsLiveData.value != true
    }

    fun setShowLyrics(show: Boolean) {
        showLyricsLiveData.value = show
    }

    fun retryLyrics() {
        lyricsRequest.value = currentTrackLiveData.value
    }

    fun seekToLine(lineIndex: Int) {
        val lyr = lyricsLiveData.value ?: return
        if (lineIndex in lyr.lines.indices) {
            seekTo(lyr.lines[lineIndex].timeMs)
        }
    }

    companion object {
        const val WAVEFORM_BARS: Int = 120

        @JvmStatic
        fun factory(
            player: PlayerController,
            library: LibraryRepository,
            lyricsRepo: LyricsRepository,
            waveformRepo: WaveformRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(player, library, lyricsRepo, waveformRepo) as T
                }
            }
        }
    }
}

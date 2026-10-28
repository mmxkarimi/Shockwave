package ir.mmxkarimi.shockwave.ui.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.ui.player.PlayerFormat
import java.util.Collections

/** Queue sheet state. */
class QueueViewModel(private val player: PlayerController) : ViewModel() {

    data class UiState(
        val upNext: List<Track>,
        val currentIndex: Int,
        val playingFrom: String?,
        val minutesLeft: Int,
        val sleepKind: SleepTimer.Kind,
        val sleepMinutes: Int,
        val canUndoShuffle: Boolean,
        val current: Track?
    )

    data class Removed(val track: Track, val queueIndex: Int)

    private val state: LiveData<PlaybackState> = player.getState()
    private val ui: LiveData<UiState> = state.map { map(it) }

    fun getUi(): LiveData<UiState> = ui

    fun toQueueIndex(upNextPosition: Int): Int {
        val s = state.value ?: return -1
        return s.currentIndex + 1 + upNextPosition
    }

    fun playAt(queueIndex: Int) {
        player.skipTo(queueIndex)
    }

    fun move(fromQueueIndex: Int, toQueueIndex: Int) {
        player.moveQueueItem(fromQueueIndex, toQueueIndex)
    }

    fun remove(queueIndex: Int): Removed? {
        val s = state.value ?: return null
        if (queueIndex < 0 || queueIndex >= s.queue.size) return null
        val t = s.queue[queueIndex]
        player.removeFromQueue(queueIndex)
        return Removed(t, queueIndex)
    }

    fun restore(removed: Removed) {
        player.addToQueue(removed.track, removed.queueIndex)
    }

    fun clearQueue() {
        player.play(emptyList(), -1, null)
    }

    fun undoShuffle() {
        player.undoShuffleQueue()
    }

    fun setSleepCountdown(minutes: Int) {
        player.setSleepTimer(minutes)
    }

    fun setSleepEndOfTrack() {
        player.setSleepTimerEndOfTrack()
    }

    fun cancelSleepTimer() {
        player.cancelSleepTimer()
    }

    companion object {
        @JvmField
        val SLEEP_PRESETS_MIN = intArrayOf(15, 30, 45, 60)

        @JvmStatic
        fun map(state: PlaybackState?): UiState {
            val s = state ?: PlaybackState.empty()
            val cur = s.currentIndex
            val upNext = if (cur >= 0 && cur + 1 <= s.queue.size) {
                s.queue.subList(cur + 1, s.queue.size)
            } else {
                emptyList()
            }
            val st = s.sleepTimer
            val kind = st.kind
            val minLeft = if (kind == SleepTimer.Kind.COUNTDOWN) {
                PlayerFormat.minutesCeil(st.remainingMs)
            } else {
                0
            }
            val configured = if (kind == SleepTimer.Kind.COUNTDOWN) (st.totalMs / 60000L).toInt() else 0
            val current = if (cur in s.queue.indices) s.queue[cur] else null
            return UiState(
                upNext = upNext,
                currentIndex = cur,
                playingFrom = s.playingFrom,
                minutesLeft = minLeft,
                sleepKind = kind,
                sleepMinutes = configured,
                canUndoShuffle = s.canUndoShuffle,
                current = current
            )
        }

        @JvmStatic
        fun factory(player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QueueViewModel(player) as T
                }
            }
        }
    }
}

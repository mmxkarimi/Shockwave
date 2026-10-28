package ir.mmxkarimi.shockwave.domain.model

import java.util.Collections

/**
 * Immutable snapshot of the player, published as `LiveData<PlaybackState>`.
 *
 * @param currentTrack current item, null when the queue is empty
 * @param currentIndex index of [currentTrack] in [queue], -1 when the queue is empty
 * @param playingFrom label of the queue origin, e.g. "NEFFEX" (the "Playing from" chip); may be null
 * @param canUndoShuffle true right after [ir.mmxkarimi.shockwave.domain.repository.PlayerController.shuffleQueue]; drives the Undo snackbar
 */
data class PlaybackState(
    @get:JvmName("currentTrack") val currentTrack: Track?,
    @get:JvmName("playing") val playing: Boolean,
    @get:JvmName("buffering") val buffering: Boolean,
    @get:JvmName("positionMs") val positionMs: Long,
    @get:JvmName("durationMs") val durationMs: Long,
    @get:JvmName("shuffleEnabled") val shuffleEnabled: Boolean,
    @get:JvmName("repeatMode") val repeatMode: RepeatMode,
    @get:JvmName("queue") var queue: List<Track>,
    @get:JvmName("currentIndex") val currentIndex: Int,
    @get:JvmName("sleepTimer") val sleepTimer: SleepTimer,
    @get:JvmName("playingFrom") val playingFrom: String?,
    @get:JvmName("canUndoShuffle") val canUndoShuffle: Boolean,
) {
    init {
        queue = Collections.unmodifiableList(ArrayList(queue))
    }

    fun hasNext(): Boolean = currentIndex >= 0 && currentIndex < queue.size - 1

    fun hasPrevious(): Boolean = currentIndex > 0

    /** Remaining time of the current track plus the durations of all tracks after it. */
    fun remainingQueueMs(): Long {
        if (currentIndex < 0) return 0L
        var total = maxOf(0L, durationMs - positionMs)
        for (i in currentIndex + 1 until queue.size) total += queue[i].durationMs
        return total
    }

    companion object {
        /** Idle player: empty queue, nothing playing. */
        @JvmStatic fun empty(): PlaybackState = PlaybackState(
            null, false, false, 0L, 0L, false, RepeatMode.OFF,
            emptyList(), -1, SleepTimer.OFF, null, false,
        )
    }
}

package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * Domain-facing wrapper over the playback engine. Call from the main thread; calls never block.
 * State is observed through [getState]; the position inside it is only refreshed while playing.
 */
interface PlayerController {

    /** Current snapshot; never null after construction (starts as [PlaybackState.empty] or a seeded queue). */
    fun getState(): LiveData<PlaybackState>

    /** Replaces the queue with [tracks] and starts playing at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int, playingFrom: String?)

    /** Resumes the current queue. */
    fun resume()

    fun pause()

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun next()

    /** Restarts the track when more than 3 s in, otherwise goes to a previous item. */
    fun previous()

    fun skipTo(index: Int)

    fun toggleShuffle()

    /** OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeat()

    fun moveQueueItem(from: Int, to: Int)

    fun removeFromQueue(index: Int)

    /** Appends to the queue. */
    fun addToQueue(track: Track)

    /** Inserts at [index] (clamped). Also used to undo a removal. */
    fun addToQueue(track: Track, index: Int)

    /** Inserts right after the current item. */
    fun playNext(track: Track)

    /** Shuffles the upcoming items (current stays) and sets canUndoShuffle. */
    fun shuffleQueue()

    fun undoShuffleQueue()

    fun setSleepTimer(minutes: Int)

    fun setSleepTimerEndOfTrack()

    fun cancelSleepTimer()
}

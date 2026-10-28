package ir.mmxkarimi.shockwave.fake

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.RepeatMode
import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import java.util.ArrayList
import java.util.Collections
import java.util.Random

/**
 * In-memory [PlayerController] that simulates playback (no audio). Main thread only.
 * A [Handler] ticker runs only while playing, so a paused fake costs nothing. Starts paused at 0:42
 * with a populated queue so the UI has something to render on first launch.
 */
class FakePlayerController : PlayerController {
    private val state = MutableLiveData<PlaybackState>()
    private val handler = Handler(Looper.getMainLooper())
    private val tickTask: Runnable = Runnable { onTick() }
    private val random = Random(42)

    private lateinit var queue: MutableList<Track>
    private var index = 0
    private var playing = false
    private var positionMs = 0L
    private var shuffleEnabled = false
    private var repeatMode = RepeatMode.OFF
    private var sleepTimer = SleepTimer.OFF
    private var playingFrom: String? = null
    private var preShuffleQueue: MutableList<Track>? = null
    private var preShuffleIndex = -1

    constructor() : this(SampleData.TRACKS.subList(0, 12), 1)

    constructor(initialQueue: List<Track>, startIndex: Int) {
        queue = ArrayList(initialQueue)
        index = if (queue.isEmpty()) -1 else startIndex
        positionMs = minOf(42_000L, duration())
        playingFrom = if (queue.isEmpty()) null else queue[0].artistName
        publish()
    }

    override fun getState(): LiveData<PlaybackState> {
        return state
    }

    private fun current(): Track? {
        return if (index >= 0 && index < queue.size) queue[index] else null
    }

    private fun duration(): Long {
        val t = current()
        return if (t == null) 0L else t.durationMs
    }

    private fun publish() {
        state.setValue(
            PlaybackState(
                current(), playing, false, positionMs, duration(), shuffleEnabled,
                repeatMode, queue, index, sleepTimer, playingFrom, preShuffleQueue != null,
            ),
        )
    }

    // --- ticker -------------------------------------------------------------------------------

    private fun startTicker() {
        handler.removeCallbacks(tickTask)
        handler.postDelayed(tickTask, TICK_MS)
    }

    private fun onTick() {
        tick()
        if (playing) handler.postDelayed(tickTask, TICK_MS)
    }

    /** One simulated time step; package-private so tests can drive time without a Looper. */
    fun tick() {
        if (!playing || current() == null) return
        if (sleepTimer.kind == SleepTimer.Kind.COUNTDOWN) {
            val left = sleepTimer.remainingMs - TICK_MS
            if (left <= 0) {
                playing = false
                sleepTimer = SleepTimer.OFF
                publish()
                return
            }
            sleepTimer = SleepTimer.countdown(left, sleepTimer.totalMs)
        }
        positionMs += TICK_MS
        if (positionMs >= duration()) onTrackEnded()
        publish()
    }

    private fun onTrackEnded() {
        if (sleepTimer.kind == SleepTimer.Kind.END_OF_TRACK) {
            playing = false
            positionMs = 0L
            sleepTimer = SleepTimer.OFF
        } else if (repeatMode == RepeatMode.ONE) {
            positionMs = 0L
        } else if (hasNext()) {
            goTo(index + 1, true)
        } else if (repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            goTo(0, true)
        } else {
            playing = false
            positionMs = 0L
        }
    }

    private fun hasNext(): Boolean {
        return index >= 0 && index < queue.size - 1
    }

    private fun goTo(newIndex: Int, keepPlaying: Boolean) {
        if (newIndex < 0 || newIndex >= queue.size) return
        index = newIndex
        positionMs = 0L
        playing = keepPlaying
    }

    // --- transport ----------------------------------------------------------------------------

    override fun play(tracks: List<Track>, startIndex: Int, playingFrom: String?) {
        if (tracks.isEmpty()) return
        preShuffleQueue = null
        queue = ArrayList(tracks)
        this.playingFrom = playingFrom
        goTo(maxOf(0, minOf(startIndex, queue.size - 1)), true)
        publish()
        startTicker()
    }

    override fun resume() {
        if (current() == null) return
        playing = true
        publish()
        startTicker()
    }

    override fun pause() {
        playing = false
        handler.removeCallbacks(tickTask)
        publish()
    }

    override fun togglePlayPause() {
        if (playing) pause() else resume()
    }

    override fun seekTo(positionMs: Long) {
        this.positionMs = maxOf(0L, minOf(positionMs, duration()))
        publish()
    }

    override fun next() {
        if (hasNext()) goTo(index + 1, playing)
        else if (repeatMode == RepeatMode.ALL && queue.isNotEmpty()) goTo(0, playing)
        else positionMs = 0L
        publish()
    }

    override fun previous() {
        if (positionMs > 3_000L || index <= 0) positionMs = 0L
        else goTo(index - 1, playing)
        publish()
    }

    override fun skipTo(index: Int) {
        goTo(index, true)
        publish()
        startTicker()
    }

    override fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        publish()
    }

    override fun cycleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
        publish()
    }

    // --- queue --------------------------------------------------------------------------------

    override fun moveQueueItem(from: Int, to: Int) {
        if (from < 0 || to < 0 || from >= queue.size || to >= queue.size || from == to) return
        queue = ArrayList(queue)
        queue.add(to, queue.removeAt(from))
        if (from == index) index = to
        else if (from < index && to >= index) index--
        else if (from > index && to <= index) index++
        publish()
    }

    override fun removeFromQueue(index: Int) {
        if (index < 0 || index >= queue.size) return
        queue = ArrayList(queue)
        queue.removeAt(index)
        if (queue.isEmpty()) {
            this.index = -1
            playing = false
            positionMs = 0L
        } else if (index < this.index) {
            this.index--
        } else if (index == this.index) {
            this.index = minOf(this.index, queue.size - 1)
            positionMs = 0L
        }
        publish()
    }

    override fun addToQueue(track: Track) {
        addToQueue(track, queue.size)
    }

    override fun addToQueue(track: Track, index: Int) {
        val pos = maxOf(0, minOf(index, queue.size))
        queue = ArrayList(queue)
        queue.add(pos, track)
        if (this.index < 0) {
            this.index = 0
            positionMs = 0L
        } else if (pos <= this.index) {
            this.index++
        }
        publish()
    }

    override fun playNext(track: Track) {
        addToQueue(track, index + 1)
    }

    override fun shuffleQueue() {
        if (queue.size < 3 || index < 0) return
        preShuffleQueue = queue
        preShuffleIndex = index
        val shuffled = ArrayList(queue.subList(0, index + 1))
        val tail = ArrayList(queue.subList(index + 1, queue.size))
        Collections.shuffle(tail, random)
        shuffled.addAll(tail)
        queue = shuffled
        publish()
    }

    override fun undoShuffleQueue() {
        val pre = preShuffleQueue ?: return
        val restored = pre.indexOf(current())
        queue = pre
        index = if (restored >= 0) restored else preShuffleIndex
        preShuffleQueue = null
        publish()
    }

    // --- sleep timer --------------------------------------------------------------------------

    override fun setSleepTimer(minutes: Int) {
        val ms = minutes * 60_000L
        sleepTimer = SleepTimer.countdown(ms, ms)
        publish()
    }

    override fun setSleepTimerEndOfTrack() {
        sleepTimer = SleepTimer.END_OF_TRACK
        publish()
    }

    override fun cancelSleepTimer() {
        sleepTimer = SleepTimer.OFF
        publish()
    }

    companion object {
        internal const val TICK_MS = 250L
    }
}

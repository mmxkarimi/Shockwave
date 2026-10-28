package ir.mmxkarimi.shockwave.playback

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.RepeatMode
import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import java.util.Random
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

/**
 * Real [PlayerController] on top of the process-wide ExoPlayer ([PlayerHolder]). Main thread only.
 *
 * The player is the single source of truth: the queue is decoded from its MediaItems ([TrackMapper]) and
 * the [PlaybackState] is rebuilt from player events. The only timers are single [Handler] callbacks:
 * the position tick (only while playing AND someone observes the state, 4 Hz), one timeout for the play-count
 * threshold and one for the sleep timer; all idle while paused.
 */
@OptIn(markerClass = [UnstableApi::class])
class ExoPlayerController(
    context: Context,
    private val playCountListener: PlayCountListener?,
    private val nextTrackListener: Consumer<Track>?,
) : PlayerController {
    private val appContext: Context
    private val player: ExoPlayer
    private val store: PlaybackStore
    private val handler = Handler(Looper.getMainLooper())
    private val state = StateData()
    private val shuffler = QueueShuffler<Track>()
    private val sleep = SleepTimerLogic()
    private val playCount = PlayCountTracker()
    private val random = Random()
    private val tickTask: Runnable = Runnable { onTick() }
    private val sleepTask: Runnable = Runnable { onSleepWake() }
    private val playCountTask: Runnable = Runnable { onPlayCountWake() }
    private var notifiedNextId: String? = null

    private var queue: List<Track> = emptyList()
    private var consecutiveErrors = 0

    init {
        this.appContext = context.getApplicationContext()
        player = PlayerHolder.get(appContext)
        store = PlayerHolder.store(appContext)
        queue = readQueue()
        startPlayCount()
        player.addListener(PlayerEvents())
        state.setValue(buildState())
    }

    override fun getState(): LiveData<PlaybackState> {
        return state
    }

    // --- transport ----------------------------------------------------------------------------

    override fun play(tracks: List<Track>, startIndex: Int, playingFrom: String?) {
        val items = ArrayList<MediaItem>(tracks.size)
        var start = 0
        for (i in tracks.indices) {
            if (!TrackMapper.isPlayable(tracks[i])) continue
            if (i < startIndex) start++
            items.add(TrackMapper.toMediaItem(tracks[i]))
        }
        if (items.isEmpty()) return
        shuffler.clear()
        store.setPlayingFrom(playingFrom)
        ensureService()
        player.setMediaItems(items, min(start, items.size - 1), 0L)
        player.prepare()
        player.play()
    }

    override fun resume() {
        if (player.getMediaItemCount() == 0) return
        ensureService()
        prepareIfIdle()
        if (player.getPlaybackState() == Player.STATE_ENDED) player.seekToDefaultPosition()
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun togglePlayPause() {
        if (isPlaying()) pause() else resume()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(max(0L, positionMs))
    }

    override fun next() {
        prepareIfIdle()
        if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekToDefaultPosition()
    }

    /** ExoPlayer's own rule matches the contract: restart when more than 3 s in, else previous item. */
    override fun previous() {
        prepareIfIdle()
        player.seekToPrevious()
    }

    override fun skipTo(index: Int) {
        if (index < 0 || index >= player.getMediaItemCount()) return
        ensureService()
        prepareIfIdle()
        player.seekToDefaultPosition(index)
        player.play()
    }

    override fun toggleShuffle() {
        player.setShuffleModeEnabled(!player.getShuffleModeEnabled())
    }

    override fun cycleRepeat() {
        when (player.getRepeatMode()) {
            Player.REPEAT_MODE_OFF -> player.setRepeatMode(Player.REPEAT_MODE_ALL)
            Player.REPEAT_MODE_ALL -> player.setRepeatMode(Player.REPEAT_MODE_ONE)
            else -> player.setRepeatMode(Player.REPEAT_MODE_OFF)
        }
    }

    // --- queue --------------------------------------------------------------------------------

    override fun moveQueueItem(from: Int, to: Int) {
        val count = player.getMediaItemCount()
        if (from < 0 || to < 0 || from >= count || to >= count || from == to) return
        shuffler.clear()
        player.moveMediaItem(from, to)
    }

    override fun removeFromQueue(index: Int) {
        if (index < 0 || index >= player.getMediaItemCount()) return
        shuffler.clear()
        player.removeMediaItem(index)
    }

    override fun addToQueue(track: Track) {
        addToQueue(track, player.getMediaItemCount())
    }

    override fun addToQueue(track: Track, index: Int) {
        if (!TrackMapper.isPlayable(track)) return
        val wasEmpty = player.getMediaItemCount() == 0
        shuffler.clear()
        player.addMediaItem(max(0, min(index, player.getMediaItemCount())), TrackMapper.toMediaItem(track))
        if (wasEmpty) player.prepare() // paused, ready to resume
    }

    override fun playNext(track: Track) {
        addToQueue(track, if (queue.isEmpty()) 0 else player.getCurrentMediaItemIndex() + 1)
    }

    override fun shuffleQueue() {
        val current = player.getCurrentMediaItemIndex()
        val shuffled = shuffler.shuffle(queue, current, random)
        if (shuffled == null) return
        applyOrder(shuffled, current)
        publish()
    }

    override fun undoShuffleQueue() {
        val previous = shuffler.undo()
        if (previous != null && previous.size == player.getMediaItemCount()) {
            val current = previous.indexOf(currentTrack())
            if (current >= 0) applyOrder(previous, current)
        }
        publish()
    }

    /**
     * Reorders the player's queue to `target` without touching the playing item, which must be
     * `target.get(targetCurrent)`: first the items after it are replaced, then the ones before it.
     */
    private fun applyOrder(target: List<Track>, targetCurrent: Int) {
        val current = player.getCurrentMediaItemIndex()
        player.replaceMediaItems(current + 1, player.getMediaItemCount(),
                toItems(target.subList(targetCurrent + 1, target.size)))
        if (current > 0 || targetCurrent > 0) {
            player.replaceMediaItems(0, current, toItems(target.subList(0, targetCurrent)))
        }
    }

    // --- sleep timer --------------------------------------------------------------------------

    override fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        player.setPauseAtEndOfMediaItems(false)
        player.setVolume(1f)
        sleep.startCountdown(minutes * 60_000L, player.isPlaying(), now())
        scheduleSleep()
        publish()
    }

    override fun setSleepTimerEndOfTrack() {
        sleep.startEndOfTrack()
        handler.removeCallbacks(sleepTask)
        player.setVolume(1f)
        player.setPauseAtEndOfMediaItems(true) // ExoPlayer pauses itself at the end of the item: no polling
        publish()
    }

    override fun cancelSleepTimer() {
        sleep.cancel()
        handler.removeCallbacks(sleepTask)
        player.setPauseAtEndOfMediaItems(false)
        player.setVolume(1f)
        publish()
    }

    private fun scheduleSleep() {
        handler.removeCallbacks(sleepTask)
        val delay = sleep.nextWakeMs(now())
        if (delay >= 0) handler.postDelayed(sleepTask, delay)
    }

    private fun onSleepWake() {
        val now = now()
        if (sleep.isExpired(now)) {
            sleep.cancel()
            player.pause()
            player.setVolume(1f)
            publish()
            return
        }
        player.setVolume(sleep.volume(now)) // fade-out during the last seconds
        scheduleSleep()
    }

    // --- play count ---------------------------------------------------------------------------

    private fun startPlayCount() {
        val t = currentTrack()
        playCount.start(if (t == null) null else t.id, if (t == null) 0L else t.durationMs)
        playCount.setPlaying(player.isPlaying(), now())
        schedulePlayCount()
    }

    private fun schedulePlayCount() {
        handler.removeCallbacks(playCountTask)
        val delay = playCount.msUntilCount(now())
        if (delay >= 0) handler.postDelayed(playCountTask, delay)
    }

    private fun onPlayCountWake() {
        val trackId = playCount.claim(now())
        val listener = playCountListener
        if (trackId != null && listener != null) listener.onTrackPlayed(trackId)
        schedulePlayCount()
    }

    // --- state --------------------------------------------------------------------------------

    private fun onTick() {
        publish()
        scheduleTick()
    }

    private fun scheduleTick() {
        handler.removeCallbacks(tickTask)
        if (state.hasActiveObservers() && player.isPlaying()) handler.postDelayed(tickTask, TICK_MS)
    }

    private fun isPlaying(): Boolean {
        return player.isPlaying()
                || (player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_BUFFERING)
    }

    private fun prepareIfIdle() {
        if (player.getPlaybackState() == Player.STATE_IDLE && player.getMediaItemCount() > 0) player.prepare()
    }

    /** Starts the service (notification, media buttons). */
    private fun ensureService() {
        try {
            appContext.startService(Intent(appContext, PlaybackService::class.java))
        } catch (e: IllegalStateException) {
            // Background-start restriction: playback still works, only the notification is missing.
        }
    }

    private fun currentTrack(): Track? {
        val index = player.getCurrentMediaItemIndex()
        return if (index >= 0 && index < queue.size) queue[index] else null
    }

    private fun readQueue(): List<Track> {
        val count = player.getMediaItemCount()
        val tracks = ArrayList<Track>(count)
        for (i in 0 until count) {
            val t = TrackMapper.fromMediaItem(player.getMediaItemAt(i))
            if (t != null) tracks.add(t)
        }
        return tracks
    }

    private fun publish() {
        state.setValue(buildState())
    }

    private fun buildState(): PlaybackState {
        val now = now()
        val current = currentTrack()
        var duration = player.getDuration()
        if (duration == C.TIME_UNSET || duration <= 0) duration = if (current == null) 0L else current.durationMs
        var position = if (current == null) 0L else max(0L, player.getCurrentPosition())
        if (duration > 0) position = min(position, duration)
        return PlaybackState(
                current,
                isPlaying(),
                player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_BUFFERING,
                position,
                duration,
                player.getShuffleModeEnabled(),
                repeatMode(),
                queue,
                if (current == null) -1 else player.getCurrentMediaItemIndex(),
                sleep.snapshot(now),
                if (queue.isEmpty()) null else store.playingFrom(),
                shuffler.canUndo())
    }

    /** Only while playing (nothing is prefetched for a paused player) and only when the next track changed. */
    private fun notifyNextTrack() {
        val listener = nextTrackListener
        if (listener == null || !player.isPlaying()) return
        val index = player.getNextMediaItemIndex()
        val next = if (index >= 0 && index < queue.size) queue[index] else null
        if (next == null || next.id == notifiedNextId) return
        notifiedNextId = next.id
        listener.accept(next)
    }

    private fun repeatMode(): RepeatMode {
        return when (player.getRepeatMode()) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
    }

    /** LiveData that ticks (and flushes persistence) only while observed. */
    private inner class StateData : MutableLiveData<PlaybackState>() {
        override fun onActive() {
            publish()
            scheduleTick()
        }

        override fun onInactive() {
            handler.removeCallbacks(tickTask)
            store.saveNow() // UI left: persist the position, the only thing ticks would have changed
        }
    }

    /** Individual callbacks run before `onEvents`, so the bookkeeping is done before the state is published. */
    private inner class PlayerEvents : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            startPlayCount()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) consecutiveErrors = 0
            val now = now()
            playCount.setPlaying(isPlaying, now)
            schedulePlayCount()
            sleep.setPlaying(isPlaying, now)
            scheduleSleep()
            scheduleTick()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM
                    && sleep.kind() == SleepTimer.Kind.END_OF_TRACK) {
                sleep.cancel() // the end-of-track pause just happened
                player.setPauseAtEndOfMediaItems(false)
            }
        }

        /** Unreadable file: skip to the next one, but never loop forever over a fully broken queue. */
        override fun onPlayerError(error: PlaybackException) {
            if (++consecutiveErrors <= player.getMediaItemCount() && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            queue = readQueue() // before onMediaItemTransition, which looks up the current track
        }

        override fun onEvents(p: Player, events: Player.Events) {
            publish()
            notifyNextTrack()
        }
    }

    companion object {
        internal const val TICK_MS = 250L

        @JvmStatic
        private fun now(): Long {
            return SystemClock.uptimeMillis()
        }

        @JvmStatic
        private fun toItems(tracks: List<Track>): List<MediaItem> {
            val items = ArrayList<MediaItem>(tracks.size)
            for (t in tracks) items.add(TrackMapper.toMediaItem(t))
            return items
        }
    }
}
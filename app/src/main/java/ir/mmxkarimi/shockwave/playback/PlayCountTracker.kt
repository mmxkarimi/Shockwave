package ir.mmxkarimi.shockwave.playback

import kotlin.math.max
import kotlin.math.min

/**
 * Decides when a track counts as "played": after min(30 s, 30 % of its length) of actual playback time
 * (seeking forward does not count). Pure logic, no Android; the caller passes a monotonic clock (`now`, ms)
 * and schedules ONE timeout from [msUntilCount], so nothing polls.
 */
internal class PlayCountTracker {
    private var trackId: String? = null
    private var thresholdMs: Long = 0L
    private var playedMs: Long = 0L
    private var playingSince: Long = -1L
    private var counted: Boolean = false

    /** A new play-through begins (item change or repeat-one loop). Starts in the not-playing state. */
    fun start(trackId: String?, durationMs: Long) {
        this.trackId = trackId
        this.thresholdMs = thresholdMs(durationMs)
        this.playedMs = 0L
        this.playingSince = -1L
        this.counted = trackId == null
    }

    fun setPlaying(playing: Boolean, now: Long) {
        if (playing && playingSince < 0) {
            playingSince = now
        } else if (!playing && playingSince >= 0) {
            playedMs += now - playingSince
            playingSince = -1L
        }
    }

    /** Milliseconds of further playback until the track counts, or -1 when no timeout is needed. */
    fun msUntilCount(now: Long): Long {
        if (counted || playingSince < 0) return -1L
        return max(0L, thresholdMs - (playedMs + now - playingSince))
    }

    /** Returns the track id exactly once when the threshold has been reached, else null. */
    fun claim(now: Long): String? {
        if (counted || playingSince < 0 || msUntilCount(now) > 0) return null
        counted = true
        return trackId
    }

    companion object {
        internal const val MAX_THRESHOLD_MS = 30_000L
        internal const val MIN_THRESHOLD_MS = 1_000L
        internal const val FRACTION = 0.3f

        /** Threshold for a track of the given length; unknown length (<= 0) uses the maximum. */
        @JvmStatic
        @JvmName("thresholdMs")
        internal fun thresholdMs(durationMs: Long): Long {
            if (durationMs <= 0) return MAX_THRESHOLD_MS
            return max(MIN_THRESHOLD_MS, min(MAX_THRESHOLD_MS, (durationMs * FRACTION).toLong()))
        }
    }
}
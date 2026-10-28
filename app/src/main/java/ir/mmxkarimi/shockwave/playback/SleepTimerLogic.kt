package ir.mmxkarimi.shockwave.playback

import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import kotlin.math.max
import kotlin.math.min

/**
 * Sleep timer state machine, pure Java. A countdown only runs while playback is running (like a real
 * "stop after N minutes of music"), so a paused player has no pending work. The caller passes a monotonic clock
 * (`now`, ms), schedules one timeout from [nextWakeMs], and applies [volume] (fade-out
 * during the last [FADE_MS]) and [isExpired].
 */
internal class SleepTimerLogic {
    private var kind: SleepTimer.Kind = SleepTimer.Kind.OFF
    private var totalMs: Long = 0L
    private var remainingMs: Long = 0L
    private var countingSince: Long = -1L

    fun kind(): SleepTimer.Kind {
        return kind
    }

    fun startCountdown(totalMs: Long, playing: Boolean, now: Long) {
        kind = SleepTimer.Kind.COUNTDOWN
        this.totalMs = max(0L, totalMs)
        remainingMs = this.totalMs
        countingSince = if (playing) now else -1L
    }

    fun startEndOfTrack() {
        kind = SleepTimer.Kind.END_OF_TRACK
        totalMs = 0L
        remainingMs = 0L
        countingSince = -1L
    }

    fun cancel() {
        kind = SleepTimer.Kind.OFF
        totalMs = 0L
        remainingMs = 0L
        countingSince = -1L
    }

    /** Playback started/stopped; freezes or resumes a running countdown. */
    fun setPlaying(playing: Boolean, now: Long) {
        if (kind != SleepTimer.Kind.COUNTDOWN) return
        if (playing && countingSince < 0) {
            countingSince = now
        } else if (!playing && countingSince >= 0) {
            remainingMs = remainingMs(now)
            countingSince = -1L
        }
    }

    fun remainingMs(now: Long): Long {
        if (kind != SleepTimer.Kind.COUNTDOWN) return 0L
        val elapsed = if (countingSince >= 0) now - countingSince else 0L
        return max(0L, remainingMs - elapsed)
    }

    fun isExpired(now: Long): Boolean {
        return kind == SleepTimer.Kind.COUNTDOWN && remainingMs(now) <= 0L
    }

    /** Player volume in [0, 1]: 1 until the last FADE_MS of a countdown, then a linear fade to 0. */
    fun volume(now: Long): Float {
        if (kind != SleepTimer.Kind.COUNTDOWN) return 1f
        val fade = min(FADE_MS, totalMs)
        if (fade <= 0) return 0f
        return min(1f, remainingMs(now) / fade.toFloat())
    }

    /** Delay until the next fade step / expiry while a countdown is running, else -1 (nothing to schedule). */
    fun nextWakeMs(now: Long): Long {
        if (kind != SleepTimer.Kind.COUNTDOWN || countingSince < 0) return -1L
        val remaining = remainingMs(now)
        val fade = min(FADE_MS, totalMs)
        if (remaining > fade) return remaining - fade
        return min(remaining, FADE_STEP_MS)
    }

    fun snapshot(now: Long): SleepTimer {
        return when (kind) {
            SleepTimer.Kind.COUNTDOWN -> SleepTimer.countdown(remainingMs(now), totalMs)
            SleepTimer.Kind.END_OF_TRACK -> SleepTimer.END_OF_TRACK
            else -> SleepTimer.OFF
        }
    }

    companion object {
        internal const val FADE_MS = 8_000L
        internal const val FADE_STEP_MS = 250L
    }
}
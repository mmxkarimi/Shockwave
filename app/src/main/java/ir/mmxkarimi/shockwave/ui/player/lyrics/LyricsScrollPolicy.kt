package ir.mmxkarimi.shockwave.ui.player.lyrics

import kotlin.math.max

/** Pure auto-scroll rule for the lyrics list. */
class LyricsScrollPolicy {
    private var userScrolledAt: Long = Long.MIN_VALUE / 2

    fun onUserScroll(nowMs: Long) {
        userScrolledAt = nowMs
    }

    fun resume() {
        userScrolledAt = Long.MIN_VALUE / 2
    }

    fun shouldAutoScroll(nowMs: Long): Boolean {
        return nowMs - userScrolledAt >= PAUSE_MS
    }

    /** Milliseconds until auto-scroll resumes (0 if active). */
    fun remainingPauseMs(nowMs: Long): Long {
        return max(0L, PAUSE_MS - (nowMs - userScrolledAt))
    }

    companion object {
        const val PAUSE_MS: Long = 3_000L

        /** Alpha for a line: active 1, upcoming .60, past .50; unsynced lyrics are all 1. */
        @JvmStatic
        fun alphaFor(line: Int, active: Int, synced: Boolean): Float {
            if (!synced) return 1f
            if (line == active) return 1f
            return if (line < active) 0.50f else 0.60f
        }
    }
}

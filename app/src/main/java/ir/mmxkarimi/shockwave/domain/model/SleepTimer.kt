package ir.mmxkarimi.shockwave.domain.model

/**
 * Sleep timer state: [Kind.OFF], [Kind.COUNTDOWN] (stops when [remainingMs] hits 0) or
 * [Kind.END_OF_TRACK] (stops when the current track ends). Immutable.
 */
data class SleepTimer(
    @get:JvmName("kind") val kind: Kind,
    @get:JvmName("remainingMs") val remainingMs: Long,
    @get:JvmName("totalMs") val totalMs: Long,
) {
    enum class Kind { OFF, COUNTDOWN, END_OF_TRACK }

    companion object {
        @JvmField val OFF = SleepTimer(Kind.OFF, 0L, 0L)
        @JvmField val END_OF_TRACK = SleepTimer(Kind.END_OF_TRACK, 0L, 0L)

        @JvmStatic fun countdown(remainingMs: Long, totalMs: Long): SleepTimer =
            SleepTimer(Kind.COUNTDOWN, remainingMs, totalMs)
    }
}

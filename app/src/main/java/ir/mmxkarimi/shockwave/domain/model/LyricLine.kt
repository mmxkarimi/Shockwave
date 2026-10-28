package ir.mmxkarimi.shockwave.domain.model

/** One lyric line. For unsynced lyrics [timeMs] is [UNSYNCED]. */
data class LyricLine(
    @get:JvmName("timeMs") val timeMs: Long,
    @get:JvmName("text") val text: String,
) {
    companion object {
        const val UNSYNCED = -1L
    }
}

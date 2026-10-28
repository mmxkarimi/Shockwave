package ir.mmxkarimi.shockwave.domain.model

import java.util.Collections

/**
 * Lyrics of one track, synced (LRC) or plain. A result with no lines means "no lyrics found" (see [none]).
 *
 * @param source free text such as "Embedded", "LRC file", "LRCLIB"; may be null
 */
data class Lyrics(
    @get:JvmName("trackId") val trackId: String,
    @get:JvmName("lines") var lines: List<LyricLine>,
    @get:JvmName("synced") val synced: Boolean,
    @get:JvmName("source") val source: String?,
) {
    init {
        lines = Collections.unmodifiableList(ArrayList(lines))
    }

    /** Whether there are no lines (nothing was found). */
    fun isEmpty(): Boolean = lines.isEmpty()

    /**
     * Index of the line active at [positionMs] (last line whose time is <= positionMs),
     * or -1 before the first line and for unsynced lyrics.
     */
    fun activeLineIndex(positionMs: Long): Int {
        if (!synced) return -1
        var lo = 0
        var hi = lines.size - 1
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return ans
    }

    companion object {
        /** The "nothing found" result. */
        @JvmStatic fun none(trackId: String): Lyrics = Lyrics(trackId, emptyList(), false, null)
    }
}

package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.domain.model.LyricLine
import ir.mmxkarimi.shockwave.domain.model.Lyrics

/**
 * Result of [LrcParser.parse].
 *
 * @param metadata LRC header tags with lower-cased keys (`ti`, `ar`, `al`, `by`, ...)
 * @param offsetMs the `[offset:]` tag; already applied to [lines]
 */
data class ParsedLrc(
    @get:JvmName("lines") val lines: List<LyricLine>,
    @get:JvmName("synced") val synced: Boolean,
    @get:JvmName("metadata") val metadata: Map<String, String>,
    @get:JvmName("offsetMs") val offsetMs: Long,
) {

    fun toLyrics(trackId: String, source: String?): Lyrics = Lyrics(trackId, lines, synced, source)
}

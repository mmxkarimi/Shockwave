package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.domain.model.Track

/** One place lyrics can come from. Called on a background thread only. */
fun interface LyricsSource {

    /** Raw lyrics text (LRC or plain) plus a display label such as "Embedded", "LRC file", "LRCLIB". */
    data class Found(
        @get:JvmName("text") val text: String,
        @get:JvmName("source") val source: String,
    )

    /** @return the lyrics, or null when this source has none for the track; IOException = could not find out */
    fun load(track: Track, cancel: CancelToken): Found?
}

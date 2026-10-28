package ir.mmxkarimi.shockwave.domain.repository

import ir.mmxkarimi.shockwave.domain.model.Track

/** Pluggable catalog provider (the local library now; a legal online provider later). Callbacks run on main. */
interface MusicSource {

    fun getId(): String

    fun getDisplayName(): String

    /** Failures are reported as an empty list. */
    fun search(query: String, limit: Int, callback: Callback<List<Track>>)

    /** Resolves a playable URI for [track]; the callback gets null if it can't be played from this source. */
    fun resolvePlayableUri(track: Track, callback: Callback<String?>)
}

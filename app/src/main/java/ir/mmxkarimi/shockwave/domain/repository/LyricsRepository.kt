package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.Track

interface LyricsRepository {

    /**
     * Emits nothing while loading, then exactly one [Lyrics]: the lyrics (synced or plain) or
     * [Lyrics.none] when nothing was found.
     */
    fun lyricsFor(track: Track): LiveData<Lyrics>
}

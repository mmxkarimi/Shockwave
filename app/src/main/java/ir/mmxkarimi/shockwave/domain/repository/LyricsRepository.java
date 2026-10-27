package ir.mmxkarimi.shockwave.domain.repository;

import androidx.lifecycle.LiveData;
import ir.mmxkarimi.shockwave.domain.model.Lyrics;
import ir.mmxkarimi.shockwave.domain.model.Track;

public interface LyricsRepository {

    /**
     * Emits nothing while loading, then exactly one {@link Lyrics}: the lyrics (synced or plain) or
     * {@link Lyrics#none(String)} when nothing was found.
     */
    LiveData<Lyrics> lyricsFor(Track track);
}

package ir.mmxkarimi.shockwave.fake;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.mmxkarimi.shockwave.domain.model.Lyrics;
import ir.mmxkarimi.shockwave.domain.model.Track;
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository;

public final class FakeLyricsRepository implements LyricsRepository {
    @Override public LiveData<Lyrics> lyricsFor(Track track) {
        return new MutableLiveData<>(SampleData.lyricsFor(track.id()));
    }
}

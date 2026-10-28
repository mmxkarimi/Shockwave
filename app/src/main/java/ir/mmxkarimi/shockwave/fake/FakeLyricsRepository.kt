package ir.mmxkarimi.shockwave.fake

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository

class FakeLyricsRepository : LyricsRepository {
    override fun lyricsFor(track: Track): LiveData<Lyrics> {
        return MutableLiveData(SampleData.lyricsFor(track.id))
    }
}

package ir.mmxkarimi.shockwave.fake

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.WaveformRepository
import java.util.Random

/** Deterministic pseudo-loudness envelope per track (smoothed random walk in [0.08, 1]). */
class FakeWaveformRepository : WaveformRepository {
    override fun waveformFor(track: Track, bars: Int): LiveData<FloatArray> {
        val random = Random(track.id.hashCode().toLong())
        val out = FloatArray(maxOf(bars, 0))
        var level = 0.5f
        for (i in out.indices) {
            level = maxOf(0.08f, minOf(1f, level * 0.6f + random.nextFloat() * 0.5f))
            out[i] = level
        }
        return MutableLiveData(out)
    }
}

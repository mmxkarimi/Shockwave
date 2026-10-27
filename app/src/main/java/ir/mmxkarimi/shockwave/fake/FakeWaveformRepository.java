package ir.mmxkarimi.shockwave.fake;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ir.mmxkarimi.shockwave.domain.model.Track;
import ir.mmxkarimi.shockwave.domain.repository.WaveformRepository;
import java.util.Random;

/** Deterministic pseudo-loudness envelope per track (smoothed random walk in [0.08, 1]). */
public final class FakeWaveformRepository implements WaveformRepository {
    @Override public LiveData<float[]> waveformFor(Track track, int bars) {
        Random random = new Random(track.id().hashCode());
        float[] out = new float[Math.max(bars, 0)];
        float level = 0.5f;
        for (int i = 0; i < out.length; i++) {
            level = Math.max(0.08f, Math.min(1f, level * 0.6f + random.nextFloat() * 0.5f));
            out[i] = level;
        }
        return new MutableLiveData<>(out);
    }
}

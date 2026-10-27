package ir.mmxkarimi.shockwave.domain.repository;

import androidx.lifecycle.LiveData;
import ir.mmxkarimi.shockwave.domain.model.Track;

public interface WaveformRepository {

    /**
     * Loudness envelope of {@code track} resampled to {@code bars} values in [0, 1] (the "Bars" / "Waveform"
     * seek bar). Emits nothing while extracting (the UI shows its flat placeholder); implementations cache results.
     */
    LiveData<float[]> waveformFor(Track track, int bars);
}

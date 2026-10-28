package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.Track

interface WaveformRepository {

    /**
     * Loudness envelope of [track] resampled to [bars] values in [0, 1] (the "Bars" / "Waveform"
     * seek bar). Emits nothing while extracting (the UI shows its flat placeholder); implementations cache results.
     */
    fun waveformFor(track: Track, bars: Int): LiveData<FloatArray>
}

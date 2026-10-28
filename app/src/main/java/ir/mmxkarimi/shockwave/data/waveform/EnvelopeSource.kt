package ir.mmxkarimi.shockwave.data.waveform

import ir.mmxkarimi.shockwave.domain.model.Track
import java.util.function.BooleanSupplier

/** Produces the master envelope of a track. Blocking; called on the extraction thread only. */
interface EnvelopeSource {
    /**
     * @param cancelled polled often; return null promptly once it turns true
     * @return [WaveformMath.MASTER_BUCKETS] values in [0, 1], or null when the track cannot be decoded or the
     *         work was cancelled
     */
    fun extract(track: Track, cancelled: BooleanSupplier): FloatArray?
}

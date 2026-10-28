package ir.mmxkarimi.shockwave.data.waveform

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Streams decoded PCM into per-bucket RMS: downmixes to mono on the fly and keeps only a sum of squares and a count
 * per bucket, so memory is O(buckets) however long the track is. Pure Java, unit-tested.
 */
class EnvelopeBuilder(private val plan: SamplingPlan) {
    private val sumSq = DoubleArray(plan.buckets())
    private val count = LongArray(plan.buckets())

    /**
     * Adds one decoded buffer (`pcm.remaining()` bytes of interleaved 16-bit or 32-bit float samples, native
     * byte order) whose first frame is at `ptsUs`. Frames outside the plan's windows are ignored.
     */
    fun add(pcm: ByteBuffer, channels: Int, floatPcm: Boolean, sampleRate: Int, ptsUs: Long) {
        val bytesPerSample = if (floatPcm) 4 else 2
        val frames = pcm.remaining() / (bytesPerSample * channels)
        val pos = pcm.position()
        val saved = pcm.order()
        pcm.order(ByteOrder.nativeOrder())
        val usPerFrame = 1_000_000.0 / sampleRate
        for (f in 0 until frames) {
            val b = plan.bucketAt(ptsUs + (f * usPerFrame).toLong())
            if (b < 0) continue
            var mono = 0.0
            for (c in 0 until channels) {
                val at = pos + (f * channels + c) * bytesPerSample
                mono += if (floatPcm) pcm.getFloat(at).toDouble() else pcm.getShort(at).toDouble() / 32768.0
            }
            mono /= channels
            sumSq[b] += mono * mono
            count[b]++
        }
        pcm.order(saved)
    }

    /** Fraction of buckets that received at least one frame. */
    fun coverage(): Float {
        var n = 0
        for (c in count) if (c > 0) n++
        return n.toFloat() / count.size
    }

    /** RMS per bucket (full scale = 1); NaN for buckets that received no frame. */
    fun rms(): FloatArray {
        val out = FloatArray(count.size)
        out.fill(Float.NaN)
        for (i in out.indices) if (count[i] > 0) out[i] = Math.sqrt(sumSq[i] / count[i]).toFloat()
        return out
    }
}

package ir.mmxkarimi.shockwave.data.waveform

/** Pure-Java envelope maths (no Android): gap filling, smoothing, perceptual normalisation, resampling, byte codec. */
class WaveformMath private constructor() {

    companion object {
        /** Length of the master envelope stored per track; any `bars` count is resampled from it. */
        const val MASTER_BUCKETS = 256

        /** Dynamic range shown, in dB below the track's loud reference level. */
        internal const val RANGE_DB = 36f

        /** Loud reference = this percentile of the bucket levels, so one clipped hit does not flatten the rest. */
        internal const val REFERENCE_PERCENTILE = 0.95f

        /** Below this RMS (full scale = 1) the track counts as digital silence. */
        internal const val SILENCE_RMS = 1e-4f

        /** Raw per-bucket RMS (NaN = no data) to the final master envelope in [0, 1]; null if nothing was measured. */
        @JvmStatic
        fun toEnvelope(rms: FloatArray): FloatArray? {
            val filled = fillGaps(rms)
            return if (filled == null) null else smooth(normalize(filled))
        }

        /** Replaces NaN buckets by linear interpolation of the nearest measured neighbours; null if all are NaN. */
        @JvmStatic
        fun fillGaps(input: FloatArray): FloatArray? {
            var first = -1
            for (i in 0 until input.size) {
                if (first >= 0) break
                if (!input[i].isNaN()) first = i
            }
            if (first < 0) return null
            val out = input.clone()
            var prev = first
            for (i in 0 until first) out[i] = input[first]
            for (i in first + 1 until input.size) {
                if (input[i].isNaN()) continue
                for (g in prev + 1 until i) out[g] = input[prev] + (input[i] - input[prev]) * (g - prev) / (i - prev)
                prev = i
            }
            for (i in prev + 1 until input.size) out[i] = input[prev]
            return out
        }

        /**
         * RMS to [0, 1] on a dB scale (perceptual loudness): the loud reference maps to 1, [RANGE_DB] below it to 0,
         * so quiet intros and loud choruses are both visible. Digital silence maps to all zeros.
         */
        @JvmStatic
        fun normalize(rms: FloatArray): FloatArray {
            val sorted = rms.clone()
            sorted.sort()
            val ref = sorted[Math.min(sorted.size - 1, (REFERENCE_PERCENTILE * sorted.size).toInt())]
            val out = FloatArray(rms.size)
            if (ref < SILENCE_RMS) return out
            for (i in rms.indices) {
                val db = 20.0 * Math.log10((Math.max(rms[i], 1e-9f) / ref).toDouble())
                out[i] = Math.max(0.0, Math.min(1.0, 1.0 + db / RANGE_DB)).toFloat()
            }
            return out
        }

        /** Light [1 2 1]/4 smoothing; edges keep their own value as neighbour. Output stays within [0, 1]. */
        @JvmStatic
        fun smooth(input: FloatArray): FloatArray {
            val out = FloatArray(input.size)
            for (i in input.indices) {
                val l = input[Math.max(0, i - 1)]
                val r = input[Math.min(input.size - 1, i + 1)]
                out[i] = 0.25f * l + 0.5f * input[i] + 0.25f * r
            }
            return out
        }

        /**
         * Resamples to exactly `n` values: area average when shrinking, linear interpolation when growing.
         * An empty source gives zeros; `n <= 0` gives an empty array.
         */
        @JvmStatic
        fun resample(src: FloatArray, n: Int): FloatArray {
            val out = FloatArray(Math.max(n, 0))
            if (src.isEmpty() || n <= 0) return out
            if (src.size == 1) {
                out.fill(src[0])
                return out
            }
            if (n >= src.size) {
                for (i in 0 until n) {
                    val pos = if (n == 1) 0f else i.toFloat() * (src.size - 1) / (n - 1)
                    val lo = pos.toInt()
                    val hi = Math.min(lo + 1, src.size - 1)
                    out[i] = src[lo] + (src[hi] - src[lo]) * (pos - lo)
                }
                return out
            }
            val step = src.size.toDouble() / n
            for (i in 0 until n) {
                val from = i * step
                val to = from + step
                var sum = 0.0
                var k = from.toInt()
                while (k < src.size && k < to) {
                    sum += src[k] * (Math.min(to, (k + 1).toDouble()) - Math.max(from, k.toDouble()))
                    k++
                }
                out[i] = (sum / step).toFloat()
            }
            return out
        }

        /** One byte per bar, `round(v * 255)`; values are clamped to [0, 1]. */
        @JvmStatic
        fun encode(env: FloatArray): ByteArray {
            val out = ByteArray(env.size)
            for (i in env.indices) out[i] = Math.round(Math.max(0f, Math.min(1f, env[i])) * 255f).toByte()
            return out
        }

        @JvmStatic
        fun decode(blob: ByteArray): FloatArray {
            val out = FloatArray(blob.size)
            for (i in blob.indices) out[i] = (blob[i].toInt() and 0xFF) / 255f
            return out
        }
    }
}

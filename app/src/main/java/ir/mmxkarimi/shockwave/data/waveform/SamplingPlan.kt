package ir.mmxkarimi.shockwave.data.waveform

/**
 * Which parts of a track are decoded. The track is cut into `buckets` equal slices; only the first
 * `windowUs` of each slice is measured (the whole slice when it is shorter), preceded by `warmupUs` of
 * input that is decoded but not measured so codecs with inter-frame state (MP3 reservoir, AAC/Vorbis/Opus overlap)
 * have recovered after a skipped gap. Short tracks therefore decode fully; a 60 min podcast decodes ~1.4%.
 * Pure Java, unit-tested.
 */
class SamplingPlan internal constructor(
    durationUs: Long,
    buckets: Int,
    maxWindowUs: Long,
    warmupUs: Long,
) {
    private val durationUs: Long
    private val buckets: Int
    private val windowUs: Long
    private val warmupUs: Long

    init {
        if (durationUs <= 0 || buckets <= 0) throw IllegalArgumentException("duration and buckets must be > 0")
        this.durationUs = durationUs
        this.buckets = buckets
        this.windowUs = Math.max(1, Math.min(maxWindowUs, durationUs / buckets))
        this.warmupUs = warmupUs
    }

    constructor(durationUs: Long, buckets: Int) : this(durationUs, buckets, MAX_WINDOW_US, WARMUP_US)

    fun buckets(): Int = buckets

    internal fun startUs(bucket: Int): Long = durationUs * bucket / buckets

    private fun windowEndUs(bucket: Int): Long = Math.min(startUs(bucket) + windowUs, durationUs)

    private fun sliceOf(tUs: Long): Int {
        var b = Math.min((buckets - 1).toLong(), Math.max(0, tUs * buckets / durationUs)).toInt()
        while (b > 0 && tUs < startUs(b)) b--
        while (b < buckets - 1 && tUs >= startUs(b + 1)) b++
        return b
    }

    /** Bucket whose measured window contains `tUs`, or -1 (warm-up, skipped part, or outside the track). */
    fun bucketAt(tUs: Long): Int {
        if (tUs < 0 || tUs >= durationUs) return -1
        val b = sliceOf(tUs)
        return if (tUs < windowEndUs(b)) b else -1
    }

    /**
     * Earliest time `>= tUs` whose input must be decoded (a window or its warm-up), or -1 when nothing is left.
     * Callers skip input before the returned time (advance, or seek for big gaps).
     */
    fun nextFeedUs(tUs: Long): Long {
        if (tUs >= durationUs) return -1
        var b = sliceOf(Math.max(tUs, 0))
        while (b < buckets) {
            if (tUs < windowEndUs(b)) return Math.max(tUs, Math.max(0, startUs(b) - warmupUs))
            b++
        }
        return -1
    }

    companion object {
        /** Measured audio per bucket. shortcut: one window per bucket, upgrade to 2 if bars look jittery on device. */
        internal const val MAX_WINDOW_US = 200_000L
        internal const val WARMUP_US = 50_000L
    }
}

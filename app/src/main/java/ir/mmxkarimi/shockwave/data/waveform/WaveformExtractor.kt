package ir.mmxkarimi.shockwave.data.waveform

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.IOException
import java.nio.ByteBuffer
import java.util.function.BooleanSupplier

/**
 * Android decode glue: MediaExtractor + MediaCodec (mp3, aac, flac, ogg, opus, wav ... whatever the device decodes)
 * feeding [EnvelopeBuilder] according to a [SamplingPlan]. Decoded buffers are consumed immediately, so
 * RAM stays at one codec buffer; input outside the plan is skipped with `advance()` (small gaps) or
 * `seekTo()` (gaps over [SEEK_GAP_US]) and never decoded.
 */
class WaveformExtractor(context: Context) : EnvelopeSource {
    private val context: Context = context.applicationContext

    override fun extract(track: Track, cancelled: BooleanSupplier): FloatArray? {
        val mediaUri = track.mediaUri ?: return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, Uri.parse(mediaUri), null)
            val index = firstAudioTrack(extractor)
            if (index < 0) return null
            val format = extractor.getTrackFormat(index)
            extractor.selectTrack(index)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) else track.durationMs * 1000
            if (durationUs <= 0) return null
            val newCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = newCodec
            newCodec.configure(format, null, null, 0)
            newCodec.start()
            val plan = SamplingPlan(durationUs, WaveformMath.MASTER_BUCKETS)
            val builder = EnvelopeBuilder(plan)
            if (!decode(extractor, newCodec, format, plan, builder, cancelled) || builder.coverage() < MIN_COVERAGE) {
                return null
            }
            return WaveformMath.toEnvelope(builder.rms())
        } catch (e: IOException) {
            Log.w(TAG, "cannot extract waveform of ${track.id}", e)
            return null
        } catch (e: RuntimeException) {
            Log.w(TAG, "cannot extract waveform of ${track.id}", e)
            return null
        } finally {
            val stopped = codec
            if (stopped != null) {
                try {
                    stopped.stop()
                } catch (ignored: RuntimeException) {
                    /* already stopped / failed */
                }
                stopped.release()
            }
            extractor.release()
        }
    }

    companion object {
        private const val TAG = "WaveformExtractor"
        private const val DEQUEUE_TIMEOUT_US = 5_000L
        private const val SEEK_GAP_US = 2_000_000L

        /** Consecutive idle dequeue rounds (~10 ms each) before a silent codec is declared stuck. */
        private const val MAX_IDLE_ROUNDS = 300
        private const val ENCODING_PCM_16BIT = 2
        private const val ENCODING_PCM_FLOAT = 4

        /** Fewer measured buckets than this (of all) means the decoder timestamps are unusable. */
        private const val MIN_COVERAGE = 0.125f

        @JvmStatic
        private fun firstAudioTrack(extractor: MediaExtractor): Int {
            for (i in 0 until extractor.getTrackCount()) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) return i
            }
            return -1
        }

        /**
         * Runs the decode loop. Input is fed in runs; at the end of a run an EOS buffer drains the codec (so the tail of
         * the window is not lost), then the extractor skips ahead and the codec is flushed. Flushing at every jump
         * matters: several decoders derive output timestamps from the first input after a flush.
         * @return false = cancelled, stuck, or unsupported PCM encoding
         */
        @JvmStatic
        private fun decode(
            extractor: MediaExtractor,
            codec: MediaCodec,
            inputFormat: MediaFormat,
            plan: SamplingPlan,
            builder: EnvelopeBuilder,
            cancelled: BooleanSupplier,
        ): Boolean {
            val info = MediaCodec.BufferInfo()
            var channels = Math.max(1, inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var floatPcm = false
            var eosQueued = false
            var lastRun = false
            var seekAllowed = true
            var idle = 0
            while (true) {
                if (cancelled.getAsBoolean() || idle > MAX_IDLE_ROUNDS) return false
                var progressed = false
                if (!eosQueued) {
                    val t = extractor.getSampleTime()
                    val next = if (t < 0) -1L else plan.nextFeedUs(t)
                    val input = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (input >= 0) {
                        progressed = true
                        val buf: ByteBuffer? = if (next == t) codec.getInputBuffer(input) else null
                        val size = if (buf == null) -1 else extractor.readSampleData(buf, 0)
                        if (size < 0) { // end of this run: drain the codec
                            codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eosQueued = true
                            lastRun = next < 0
                        } else {
                            codec.queueInputBuffer(input, 0, size, t, 0)
                            extractor.advance()
                        }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)
                if (out >= 0) {
                    progressed = true
                    val pcm: ByteBuffer? = if (info.size > 0) codec.getOutputBuffer(out) else null
                    if (pcm != null) {
                        pcm.position(info.offset).limit(info.offset + info.size)
                        builder.add(pcm, channels, floatPcm, sampleRate, info.presentationTimeUs)
                    }
                    codec.releaseOutputBuffer(out, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (lastRun) return true
                        seekAllowed = skipAhead(extractor, plan, seekAllowed)
                        codec.flush()
                        eosQueued = false
                    }
                } else if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    progressed = true
                    val f = codec.getOutputFormat()
                    channels = Math.max(1, f.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                    sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val encoding = if (f.containsKey(MediaFormat.KEY_PCM_ENCODING))
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) else ENCODING_PCM_16BIT
                    if (encoding != ENCODING_PCM_16BIT && encoding != ENCODING_PCM_FLOAT) return false
                    floatPcm = encoding == ENCODING_PCM_FLOAT
                }
                idle = if (progressed) 0 else idle + 1
            }
        }

        /**
         * Moves the extractor to the next feed time: seeks for big gaps (sync points are earlier, so it advances the
         * rest without decoding), otherwise just advances. Returns whether seeking is still worth trying; a seek that
         * did not land ahead of the old position (no seek table) is not retried.
         */
        @JvmStatic
        private fun skipAhead(extractor: MediaExtractor, plan: SamplingPlan, seekAllowed: Boolean): Boolean {
            val t = extractor.getSampleTime()
            val next = if (t < 0) -1L else plan.nextFeedUs(t)
            if (next < 0) return seekAllowed
            var allowed = seekAllowed
            if (allowed && next - t > SEEK_GAP_US) {
                extractor.seekTo(next, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                allowed = extractor.getSampleTime() > t
            }
            var now = extractor.getSampleTime()
            while (now >= 0 && now < next) {
                extractor.advance()
                now = extractor.getSampleTime()
            }
            return allowed
        }
    }
}

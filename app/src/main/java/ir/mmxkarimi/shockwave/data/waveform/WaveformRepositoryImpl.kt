package ir.mmxkarimi.shockwave.data.waveform

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.WaveformRepository
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Function

/**
 * Cache-first waveform source: memory (8 master envelopes) -> [WaveformStore] -> [EnvelopeSource].
 *
 * Always one master envelope of [WaveformMath.MASTER_BUCKETS] values per track; `bars` is resampled
 * from it, so changing the bar count never re-decodes. Work is lazy: nothing happens until a LiveData gets an active
 * observer (the track is on screen) or [prefetch] is called for the queued-next track. Concurrent requests
 * for one track share one job; a job nobody observes any more is cancelled; the single `worker` thread means
 * at most one extraction runs at a time. Failures are cached negatively (memory + store) so a bad file is tried once.
 * All mutable state is touched on the main thread only; the worker just reads [Job]'s volatile fields.
 */
class WaveformRepositoryImpl(
    private val store: WaveformStore,
    private val source: EnvelopeSource,
    private val fingerprint: Function<Track, String>,
    private val powerSave: BooleanSupplier,
    private val worker: Executor,
    private val main: Executor,
) : WaveformRepository {

    private val memory: MutableMap<String, FloatArray> = object : LinkedHashMap<String, FloatArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>): Boolean =
            size > MEMORY_ENTRIES
    }
    private val inFlight = HashMap<String, Job>()

    override fun waveformFor(track: Track, bars: Int): LiveData<FloatArray> = Result(track, bars)

    /**
     * Warms the cache for the track queued next. Does nothing in battery saver or when already cached/in flight.
     * Main thread. Replaces an earlier unobserved prefetch.
     */
    fun prefetch(track: Track) {
        val key = keyOf(track)
        if (memory.containsKey(key) || powerSave.getAsBoolean()) return
        cancelUnobservedPrefetches(key)
        var job = inFlight[key]
        if (job == null) job = start(key, track, false)
        job.prefetch = true
    }

    private fun start(key: String, track: Track, observed: Boolean): Job {
        val job = Job(key, track)
        job.hasObservers = observed
        inFlight[key] = job
        worker.execute { run(job) }
        return job
    }

    private fun cancelUnobservedPrefetches(except: String) {
        for (j in ArrayList(inFlight.values)) {
            if (j.key != except && j.prefetch && j.observers.isEmpty()) cancel(j)
        }
    }

    private fun cancel(job: Job) {
        job.cancelled = true
        inFlight.remove(job.key, job)
    }

    /** Worker thread. */
    private fun run(job: Job) {
        if (job.cancelled) return
        val track = job.track
        val fp = fingerprint.apply(track)
        val blob = store.get(track.id, fp)
        var master: FloatArray
        if (blob != null && (blob.size == 0 || blob.size == WaveformMath.MASTER_BUCKETS)) {
            master = if (blob.size == 0) FAILED else WaveformMath.decode(blob)
        } else {
            if (job.cancelled || (!job.hasObservers && powerSave.getAsBoolean())) {
                main.execute { finish(job, null) }
                return
            }
            val extracted = source.extract(track) { job.cancelled }
            if (extracted == null && job.cancelled) {
                main.execute { finish(job, null) }
                return
            }
            master = extracted ?: FAILED
            store.put(track.id, fp, WaveformMath.encode(master))
        }
        val result = master
        main.execute { finish(job, result) }
    }

    /** Main thread. `master == null` = cancelled/skipped, nothing is cached. */
    private fun finish(job: Job, master: FloatArray?) {
        inFlight.remove(job.key)
        if (master == null) return
        memory[job.key] = master
        for (r in ArrayList(job.observers)) r.deliver(master)
    }

    private class Job(internal val key: String, internal val track: Track) {
        internal val observers: MutableList<Result> = ArrayList()

        @Volatile
        internal var cancelled: Boolean = false

        @Volatile
        internal var hasObservers: Boolean = false

        internal var prefetch: Boolean = false
    }

    private inner class Result(private val track: Track, private val bars: Int) : LiveData<FloatArray>() {
        private var job: Job? = null

        override fun onActive() {
            val key = keyOf(track)
            val cached = memory[key]
            if (cached != null) {
                deliver(cached)
                return
            }
            cancelUnobservedPrefetches(key)
            val current = inFlight[key] ?: start(key, track, true)
            current.observers.add(this)
            current.hasObservers = true
            job = current
        }

        override fun onInactive() {
            val current = job ?: return
            current.observers.remove(this)
            current.hasObservers = current.observers.isNotEmpty()
            if (!current.hasObservers && !current.prefetch) cancel(current)
            job = null
        }

        internal fun deliver(master: FloatArray) {
            if (master.size > 0) setValue(WaveformMath.resample(master, bars))
        }
    }

    companion object {
        private const val MEMORY_ENTRIES = 8

        @JvmField
        internal val FAILED = FloatArray(0)

        @JvmStatic
        private fun keyOf(track: Track): String = track.id + "|" + track.durationMs
    }
}

package ir.mmxkarimi.shockwave.data.repository

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicLongArray

/**
 * Tiny in-process "something was written" bus. Writers call [notifyChanged] with the topics they touched;
 * active [DbLiveData]s reload only when one of their topics changed. Per-topic counters let a LiveData that was
 * inactive at the time notice on its next activation, so nothing listens (or wakes the CPU) while the UI is hidden.
 */
class ChangeNotifier {
    internal interface Listener {
        fun onChanged(topics: Int)
    }

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val counters = AtomicLongArray(TOPIC_COUNT)

    internal fun add(listener: Listener) {
        listeners.add(listener)
    }

    internal fun remove(listener: Listener) {
        listeners.remove(listener)
    }

    /** Changes whenever any topic in `topics` changes. */
    internal fun stamp(topics: Int): Long {
        var sum = 0L
        for (i in 0 until TOPIC_COUNT) if ((topics and (1 shl i)) != 0) sum += counters.get(i)
        return sum
    }

    /** Call after the write is committed. Any thread. */
    fun notifyChanged(topics: Int) {
        for (i in 0 until TOPIC_COUNT) if ((topics and (1 shl i)) != 0) counters.incrementAndGet(i)
        for (l in listeners) l.onChanged(topics)
    }

    companion object {
        const val TRACKS = 1
        const val LIKES = 1 shl 1
        const val PLAYLISTS = 1 shl 2
        const val HISTORY = 1 shl 3
        const val ALL = TRACKS or LIKES or PLAYLISTS or HISTORY
        private const val TOPIC_COUNT = 4
    }
}

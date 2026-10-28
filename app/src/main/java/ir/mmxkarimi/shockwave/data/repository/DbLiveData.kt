package ir.mmxkarimi.shockwave.data.repository

import androidx.lifecycle.LiveData
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

/**
 * A LiveData backed by a query that runs on the io executor. It only listens (and only loads) while it has active
 * observers: on activation it reloads if one of its topics changed meanwhile, and while active it reloads on every
 * matching [ChangeNotifier] event. Bursts of events collapse into one pending load. Results are delivered with
 * `postValue`, so observers see them on the main thread. The executor must run tasks in order (single thread).
 */
internal class DbLiveData<T>(
    private val io: Executor,
    private val notifier: ChangeNotifier,
    private val topics: Int,
    private val query: Supplier<T>,
) : LiveData<T>(), ChangeNotifier.Listener {
    private val loadPending = AtomicBoolean()

    @Volatile
    private var loadedStamp = -1L

    override fun onActive() {
        notifier.add(this)
        if (loadedStamp != notifier.stamp(topics)) load()
    }

    override fun onInactive() {
        notifier.remove(this)
    }

    override fun onChanged(changedTopics: Int) {
        if ((changedTopics and topics) != 0) load()
    }

    private fun load() {
        if (!loadPending.compareAndSet(false, true)) return
        io.execute {
            loadPending.set(false)
            val stamp = notifier.stamp(topics) // before the query: a write racing with it triggers another load
            postValue(query.get())
            loadedStamp = stamp
        }
    }
}

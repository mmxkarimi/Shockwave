package ir.mmxkarimi.shockwave.data.lyrics

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LyricsRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * Lazy [LyricsRepository]: nothing runs until the returned LiveData gets an active observer, the work happens on
 * `executor`, and it is cancelled when the last observer goes away (the lookup restarts if observed again).
 * While a track's lookup is pending, every [lyricsFor] call for it returns the same LiveData, so one track never
 * has two requests in flight. Each LiveData emits exactly one value: lyrics or [Lyrics.none].
 */
class LyricsRepositoryImpl(
    private val resolver: LyricsResolver,
    private val executor: Executor,
) : LyricsRepository {

    private val pending = ConcurrentHashMap<String, Lookup>()

    override fun lyricsFor(track: Track): LiveData<Lyrics> = pending.computeIfAbsent(track.id) { Lookup(track) }

    private inner class Lookup(private val track: Track) : LiveData<Lyrics>() {
        private var token: CancelToken? = null // main thread only

        override fun onActive() {
            if (getValue() != null || token != null) return
            pending.putIfAbsent(track.id, this) // re-register after an earlier cancel
            val mine = CancelToken()
            token = mine
            executor.execute { run(mine) }
        }

        override fun onInactive() {
            val current = token ?: return
            current.cancel()
            token = null
            if (getValue() == null) pending.remove(track.id, this) // a later call starts a fresh lookup
        }

        private fun run(mine: CancelToken) {
            val result: Lyrics? = try {
                resolver.resolve(track, mine)
            } catch (e: RuntimeException) { // e.g. SQLite failure: show "no lyrics" rather than loading forever
                Lyrics.none(track.id)
            }
            if (result == null || mine.isCancelled()) return
            pending.remove(track.id, this)
            postValue(result)
        }
    }
}

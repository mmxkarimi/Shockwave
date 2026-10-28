package ir.mmxkarimi.shockwave.playback

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ir.mmxkarimi.shockwave.domain.model.Track
import kotlin.math.max
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Survives process death: the queue (JSON), current index/position, shuffle/repeat and the "playing from" label
 * live in one small SharedPreferences file. Written on changes only, never on position ticks: the (larger) queue
 * is debounced after timeline changes; index/position/modes are saved on item transitions, pause, mode changes
 * and by [saveNow] (UI went to background / service stopping). Restore happens paused and unprepared.
 */
internal class PlaybackStore(context: Context) : Player.Listener {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val saveQueueTask: Runnable = Runnable { saveQueue() }
    private var player: ExoPlayer? = null
    private var queueDirty: Boolean = false

    fun playingFrom(): String? {
        return prefs.getString(K_FROM, null)
    }

    fun setPlayingFrom(from: String?) {
        prefs.edit().putString(K_FROM, from).apply()
    }

    /** Loads the persisted queue into `target` (paused, not prepared) and starts listening to it. */
    fun restoreAndAttach(target: ExoPlayer) {
        player = target
        val tracks = parseQueue(prefs.getString(K_QUEUE, null))
        if (tracks.isNotEmpty()) {
            val items = ArrayList<MediaItem>(tracks.size)
            for (t in tracks) items.add(TrackMapper.toMediaItem(t))
            val index = max(0, min(prefs.getInt(K_INDEX, 0), items.size - 1))
            target.setMediaItems(items, index, max(0L, prefs.getLong(K_POSITION, 0L)))
            target.setShuffleModeEnabled(prefs.getBoolean(K_SHUFFLE, false))
            target.setRepeatMode(prefs.getInt(K_REPEAT, Player.REPEAT_MODE_OFF))
        }
        queueDirty = false
        target.addListener(this)
    }

    /** Flushes everything now (cheap when nothing changed). */
    fun saveNow() {
        if (player == null) return
        handler.removeCallbacks(saveQueueTask)
        if (queueDirty) saveQueue()
        saveState()
    }

    override fun onEvents(p: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            queueDirty = true
            handler.removeCallbacks(saveQueueTask)
            handler.postDelayed(saveQueueTask, QUEUE_SAVE_DELAY_MS)
        }
        if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_REPEAT_MODE_CHANGED) || (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && !p.isPlaying())) {
            saveState()
        }
    }

    private fun saveQueue() {
        val p = player ?: return
        queueDirty = false
        val tracks = ArrayList<Track>(p.getMediaItemCount())
        for (i in 0 until p.getMediaItemCount()) {
            val t = TrackMapper.fromMediaItem(p.getMediaItemAt(i))
            if (t != null) tracks.add(t)
        }
        prefs.edit().putString(K_QUEUE, toJson(tracks)).apply()
    }

    private fun saveState() {
        val p = player ?: return
        val position = p.getCurrentPosition()
        prefs.edit()
                .putInt(K_INDEX, max(0, p.getCurrentMediaItemIndex()))
                .putLong(K_POSITION, if (position == C.TIME_UNSET) 0L else position)
                .putBoolean(K_SHUFFLE, p.getShuffleModeEnabled())
                .putInt(K_REPEAT, p.getRepeatMode())
                .apply()
    }

    companion object {
        private const val FILE = "playback_state"
        private const val K_QUEUE = "queue"
        private const val K_INDEX = "index"
        private const val K_POSITION = "position"
        private const val K_SHUFFLE = "shuffle"
        private const val K_REPEAT = "repeat"
        private const val K_FROM = "from"
        private const val QUEUE_SAVE_DELAY_MS = 1_000L

        // --- JSON codec (package-private for tests) -----------------------------------------------

        @JvmStatic
        @JvmName("toJson")
        internal fun toJson(tracks: List<Track>): String {
            val array = JSONArray()
            try {
                for (t in tracks) {
                    array.put(JSONObject()
                            .put("id", t.id).put("title", t.title)
                            .put("artistId", t.artistId).put("artist", t.artistName)
                            .put("albumId", t.albumId).put("album", t.albumTitle)
                            .put("duration", t.durationMs).put("artwork", t.artworkUri)
                            .put("seed", t.placeholderSeed).put("uri", t.mediaUri)
                            .put("explicit", t.explicit).put("liked", t.liked)
                            .put("year", t.year).put("number", t.trackNumber).put("added", t.addedAtMs))
                }
            } catch (e: JSONException) {
                throw IllegalStateException(e) // only thrown for non-finite numbers, which Tracks never hold
            }
            return array.toString()
        }

        /** Never throws: corrupt data simply restores an empty queue. */
        @JvmStatic
        @JvmName("parseQueue")
        internal fun parseQueue(json: String?): List<Track> {
            val tracks = ArrayList<Track>()
            if (json == null) return tracks
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val uri = o.optString("uri", "")
                    if (uri.isEmpty()) continue
                    tracks.add(Track(o.getString("id"), o.optString("title"), o.optString("artistId"),
                            o.optString("artist"), o.optString("albumId"), o.optString("album"),
                            o.optLong("duration"), nullable(o, "artwork"), o.optInt("seed"), uri,
                            o.optBoolean("explicit"), o.optBoolean("liked"), o.optInt("year"),
                            o.optInt("number"), o.optLong("added")))
                }
            } catch (e: JSONException) {
                tracks.clear()
            }
            return tracks
        }

        @JvmStatic
        private fun nullable(o: JSONObject, key: String): String? {
            return if (o.isNull(key)) null else o.optString(key, null)
        }
    }
}
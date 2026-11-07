package ir.mmxkarimi.shockwave.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ShockwaveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShockwaveWidget
}

/**
 * Attached to the process-wide player (see PlayerHolder), so every placed widget is refreshed on any change of the
 * player: from the app, the notification, a media button, or the widget itself. The player is the single source of
 * truth; the widget never has its own copy of the state.
 */
class ShockwaveWidgetSync(private val context: Context) : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            ShockwaveWidgetUpdater.request(context)
        }
    }
}

/**
 * Refreshes every placed widget. The snapshot is read here and published in [WidgetState]; running Glance sessions
 * collect that flow, so they redraw with the new state (a session's provideGlance runs only once, so a snapshot read
 * there would go stale). While playing, the progress bar is refreshed every few seconds.
 */
object ShockwaveWidgetUpdater {
    private const val DEBOUNCE_MS = 120L
    private const val PROGRESS_TICK_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null // main thread only

    fun request(context: Context) {
        val app = context.applicationContext
        scope.launch {
            job?.cancel()
            job = scope.launch {
                delay(DEBOUNCE_MS) // a track change fires several player events in a row
                try {
                    val ids = GlanceAppWidgetManager(app).getGlanceIds(ShockwaveWidget::class.java)
                    if (ids.isEmpty()) return@launch
                    val snapshot = WidgetSnapshotReader.read(app)
                    WidgetState.snapshot.value = snapshot
                    ShockwaveWidget.updateAll(app)
                    if (snapshot.playing) {
                        delay(PROGRESS_TICK_MS)
                        request(app)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The host is gone: nothing to refresh.
                }
            }
        }
    }
}

/** The latest widget state, shared by every running widget session. */
internal object WidgetState {
    val snapshot = MutableStateFlow<WidgetSnapshot?>(null)
}

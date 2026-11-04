package ir.mmxkarimi.shockwave.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/** Refreshes every placed widget. */
object ShockwaveWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun request(context: Context) {
        val app = context.applicationContext
        scope.launch {
            try {
                ShockwaveWidget.updateAll(app)
            } catch (e: Exception) {
                // No widget placed, or the host is gone: nothing to refresh.
            }
        }
    }
}

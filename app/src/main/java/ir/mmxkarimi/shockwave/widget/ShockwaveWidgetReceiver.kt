package ir.mmxkarimi.shockwave.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShockwaveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShockwaveWidget
}

/** Lets the playback service refresh every placed widget when the track or play state changes. */
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

package ir.mmxkarimi.shockwave.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.common.Player
import ir.mmxkarimi.shockwave.playback.PlaybackService
import ir.mmxkarimi.shockwave.playback.PlayerHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Widget buttons talk to the same in-process player as the notification and the UI. */
private suspend fun withPlayer(context: Context, block: (Player) -> Unit) {
    val app = context.applicationContext
    withContext(Dispatchers.Main) {
        val player = PlayerHolder.get(app)
        if (player.mediaItemCount == 0) return@withContext
        try {
            // Bring up the service so the notification / media buttons exist (cold process after a swipe-away).
            app.startService(Intent(app, PlaybackService::class.java))
        } catch (e: IllegalStateException) {
            // Background-start restriction: playback still works, only the notification is missing.
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        block(player)
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withPlayer(context) { player ->
            val playing = player.isPlaying ||
                (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
            if (playing) player.pause() else player.play()
        }
        ShockwaveWidget.update(context, glanceId)
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withPlayer(context) { player -> if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
        ShockwaveWidget.update(context, glanceId)
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Restarts the track when more than a few seconds in, otherwise goes to the previous item.
        withPlayer(context) { player -> player.seekToPrevious() }
        ShockwaveWidget.update(context, glanceId)
    }
}

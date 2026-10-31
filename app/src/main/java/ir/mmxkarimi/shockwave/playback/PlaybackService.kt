package ir.mmxkarimi.shockwave.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Hosts the MediaSession on the process-wide [PlayerHolder] player: media notification, lock screen,
 * headset / Bluetooth media buttons. Started by [ExoPlayerController] (or by the system for a media button),
 * it stays alive with a loaded queue — paused included — so the notification, the paused queue and the media
 * buttons keep working without reopening the app (like in other music players); it only stops itself when the
 * queue is empty, or when the task is removed while nothing is playing.
 */
@OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private val handler = Handler(Looper.getMainLooper())
    private val idleStopTask: Runnable = Runnable { stopIfIdle() }
    private val progressTick: Runnable = Runnable { tickProgress() }
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null

    private val serviceWatcher: Player.Listener = object : Player.Listener {
        override fun onEvents(p: Player, events: Player.Events) {
            handler.removeCallbacks(idleStopTask)
            if (!isActive()) {
                handler.postDelayed(idleStopTask, IDLE_STOP_DELAY_MS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
            }
            scheduleProgressTick()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        player = PlayerHolder.get(this)
        // A media button can start this service in a cold process: make the persisted queue ready to play, so
        // the button's play command finds a prepared player instead of an idle one.
        if (player.getMediaItemCount() > 0 && player.getPlaybackState() == Player.STATE_IDLE) player.prepare()
        val builder = MediaSession.Builder(this, player)
                .setBitmapLoader(ArtworkLoader(this))
                .setCallback(object : MediaSession.Callback {
                    override fun onMediaButtonEvent(
                        session: MediaSession,
                        controllerInfo: MediaSession.ControllerInfo,
                        intent: Intent,
                    ): Boolean {
                        val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        }
                        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                            if (player.getPlaybackState() == Player.STATE_IDLE && player.getMediaItemCount() > 0) {
                                player.prepare()
                            }
                            when (keyEvent.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                    player.play()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                    player.pause()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                KeyEvent.KEYCODE_HEADSETHOOK -> {
                                    if (player.isPlaying()) player.pause() else player.play()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_STOP -> {
                                    player.stop()
                                    return true
                                }
                            }
                        }
                        return super.onMediaButtonEvent(session, controllerInfo, intent)
                    }
                })
        val launch = getPackageManager().getLaunchIntentForPackage(getPackageName())
        if (launch != null) {
            builder.setSessionActivity(PendingIntent.getActivity(this, 0, launch,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        val built = builder.build()
        session = built
        addSession(built)
        // Our notification (prev / play-pause / next, progress bar; notif_icon in the status bar).
        setMediaNotificationProvider(PlaybackNotificationProvider(this))
        player.addListener(serviceWatcher)
        scheduleProgressTick()
        if (!isActive()) handler.postDelayed(idleStopTask, IDLE_STOP_DELAY_MS)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    /**
     * Closing the app from recents while music plays must not stop it: re-assert the foreground notification and
     * keep going. Paused (or empty) playback stops with the task, like the stock behaviour.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isActive()) {
            triggerNotificationUpdate()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleStopTask)
        handler.removeCallbacks(progressTick)
        player.removeListener(serviceWatcher)
        PlayerHolder.store(this).saveNow()
        val current = session
        if (current != null) {
            removeSession(current)
            current.release() // the shared player stays alive for the controller
            session = null
        }
        super.onDestroy()
    }

    private fun isActive(): Boolean {
        return player.getPlayWhenReady() && (player.getPlaybackState() == Player.STATE_READY
                || player.getPlaybackState() == Player.STATE_BUFFERING)
    }

    private fun scheduleProgressTick() {
        handler.removeCallbacks(progressTick)
        if (player.isPlaying()) handler.postDelayed(progressTick, PROGRESS_TICK_MS)
    }

    private fun tickProgress() {
        triggerNotificationUpdate()
        scheduleProgressTick()
    }

    /** Only an empty queue ends the service: a loaded (paused) queue keeps its session and notification alive. */
    private fun stopIfIdle() {
        if (isActive()) return
        if (player.getMediaItemCount() == 0) stopSelf()
    }

    companion object {
        internal const val CHANNEL_ID = "playback"
        internal const val IDLE_STOP_DELAY_MS = 60_000L

        /** The player only emits state changes, so the notification's progress bar is refreshed on this cadence. */
        internal const val PROGRESS_TICK_MS = 1_000L
    }
}
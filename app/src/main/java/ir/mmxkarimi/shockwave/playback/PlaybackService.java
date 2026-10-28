package ir.mmxkarimi.shockwave.playback;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * Hosts the MediaSession on the process-wide {@link PlayerHolder} player: media notification, lock screen,
 * headset / Bluetooth media buttons. Started by {@link ExoPlayerController} (or by the system for a media button),
 * it stays alive with a loaded queue — paused included — so the notification, the paused queue and the media
 * buttons keep working without reopening the app (like in other music players); it only stops itself when the
 * queue is empty, or when the task is removed while nothing is playing.
 */
@OptIn(markerClass = UnstableApi.class)
public class PlaybackService extends MediaSessionService {
    static final String CHANNEL_ID = "playback";
    static final long IDLE_STOP_DELAY_MS = 60_000L;
    /** The player only emits state changes, so the notification's progress bar is refreshed on this cadence. */
    static final long PROGRESS_TICK_MS = 1_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable idleStopTask = this::stopIfIdle;
    private final Runnable progressTick = this::tickProgress;
    private ExoPlayer player;
    @Nullable private MediaSession session;

    private final Player.Listener serviceWatcher = new Player.Listener() {
        @Override public void onEvents(Player p, Player.Events events) {
            handler.removeCallbacks(idleStopTask);
            if (!isActive()) handler.postDelayed(idleStopTask, IDLE_STOP_DELAY_MS);
            scheduleProgressTick();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        player = PlayerHolder.get(this);
        // A media button can start this service in a cold process: make the persisted queue ready to play, so
        // the button's play command finds a prepared player instead of an idle one.
        if (player.getMediaItemCount() > 0 && player.getPlaybackState() == Player.STATE_IDLE) player.prepare();
        MediaSession.Builder builder = new MediaSession.Builder(this, player)
                .setBitmapLoader(new ArtworkLoader(this))
                .setCallback(new MediaSession.Callback() {
                    @Override
                    public boolean onMediaButtonEvent(MediaSession session, MediaSession.ControllerInfo controllerInfo, Intent intent) {
                        if (player.getPlaybackState() == Player.STATE_IDLE && player.getMediaItemCount() > 0) {
                            player.prepare();
                        }
                        // MediaSession.Callback is an interface, so plain "super" points to Object. Return false to let
                        // Media3 handle the button as usual (this is what the interface default does).
                        return false;
                    }
                });
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch != null) {
            builder.setSessionActivity(PendingIntent.getActivity(this, 0, launch,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        session = builder.build();
        addSession(session);
        // Our notification (prev / play-pause / next, progress bar; notif_icon in the status bar).
        setMediaNotificationProvider(new PlaybackNotificationProvider(this));
        player.addListener(serviceWatcher);
        scheduleProgressTick();
        if (!isActive()) handler.postDelayed(idleStopTask, IDLE_STOP_DELAY_MS);
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    /**
     * Closing the app from recents while music plays must not stop it: re-assert the foreground notification and
     * keep going. Paused (or empty) playback stops with the task, like the stock behaviour.
     */
    @Override public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (isActive()) {
            triggerNotificationUpdate();
            return;
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(idleStopTask);
        handler.removeCallbacks(progressTick);
        player.removeListener(serviceWatcher);
        PlayerHolder.store(this).saveNow();
        if (session != null) {
            removeSession(session);
            session.release(); // the shared player stays alive for the controller
            session = null;
        }
        super.onDestroy();
    }

    private boolean isActive() {
        return player.getPlayWhenReady() && (player.getPlaybackState() == Player.STATE_READY
                || player.getPlaybackState() == Player.STATE_BUFFERING);
    }

    private void scheduleProgressTick() {
        handler.removeCallbacks(progressTick);
        if (player.isPlaying()) handler.postDelayed(progressTick, PROGRESS_TICK_MS);
    }

    private void tickProgress() {
        triggerNotificationUpdate();
        scheduleProgressTick();
    }

    /** Only an empty queue ends the service: a loaded (paused) queue keeps its session and notification alive. */
    private void stopIfIdle() {
        if (isActive()) return;
        if (player.getMediaItemCount() == 0) stopSelf();
    }
}

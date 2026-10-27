package ir.mmxkarimi.shockwave.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.BitmapLoader;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CacheBitmapLoader;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaNotification.ActionFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ir.mmxkarimi.shockwave.R;
import ir.mmxkarimi.shockwave.ui.player.PlayerFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * The app's media notification: the {@code notif_icon} mark in the status bar, artwork, title/artist, a progress
 * bar with elapsed / total time and previous · play-pause · next controls — on every Android version, including
 * Android 10 where the stock media layout showed nothing. The MediaStyle style plus the session (the service also
 * injects the platform token) hands the same controls to the system media controls on the devices that have them,
 * and the MediaStyle actions keep working as the compact row. Refreshed by the service about once per second
 * while playing, because the player only emits state changes.
 */
@OptIn(markerClass = UnstableApi.class)
public final class PlaybackNotificationProvider implements MediaNotification.Provider {
    private static final int NOTIFICATION_ID = DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID;
    /** Covers are decoded up to 512 px; the notification gets a smaller copy so its parcel stays small. */
    private static final int NOTIFICATION_ART_PX = 256;

    private final Context context;
    private final NotificationManager notificationManager;
    /** Caches decoded artwork across rebuilds, wrapping the session's loader (re-wrapped if it ever changes). */
    @Nullable private BitmapLoader artworkLoader;
    @Nullable private BitmapLoader artworkLoaderSource;
    /** Scaled copy of the last cover (identity-keyed: the cache returns the same bitmap per track). */
    @Nullable private Bitmap artworkSource;
    @Nullable private Bitmap artworkScaled;

    public PlaybackNotificationProvider(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = Objects.requireNonNull(
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public MediaNotification createNotification(MediaSession session,
            ImmutableList<CommandButton> mediaButtonPreferences, ActionFactory actionFactory,
            Callback onNotificationChanged) {
        ensureChannel();
        @Nullable Bitmap artwork = artwork(session, actionFactory, onNotificationChanged);
        return new MediaNotification(NOTIFICATION_ID, build(session, actionFactory, artwork));
    }

    @Override
    public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
        return false; // no custom actions: everything is a media command
    }

    @Override
    public NotificationChannelInfo getNotificationChannelInfo() {
        return new NotificationChannelInfo(PlaybackService.CHANNEL_ID, getString(R.string.playback_channel_name));
    }

    private void ensureChannel() {
        if (notificationManager.getNotificationChannel(PlaybackService.CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(PlaybackService.CHANNEL_ID,
                getString(R.string.playback_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    /** @return the artwork when already decoded; otherwise starts a decode and posts an update when it lands */
    @Nullable
    private Bitmap artwork(MediaSession session, ActionFactory actionFactory, Callback onNotificationChanged) {
        MediaMetadata metadata = session.getPlayer().getMediaMetadata();
        if (metadata.artworkUri == null) return null;
        BitmapLoader sessionLoader = session.getBitmapLoader();
        if (artworkLoader == null || artworkLoaderSource != sessionLoader) {
            artworkLoaderSource = sessionLoader;
            artworkLoader = new CacheBitmapLoader(sessionLoader);
        }
        ListenableFuture<Bitmap> future = artworkLoader.loadBitmapFromMetadata(metadata);
        if (future == null) return null;
        if (!future.isDone()) {
            // The player lives on the main thread; rebuild the notification there like the stock provider does.
            Futures.addCallback(future, new FutureCallback<Bitmap>() {
                @Override public void onSuccess(Bitmap result) {
                    try {
                        onNotificationChanged.onNotificationChanged(
                                new MediaNotification(NOTIFICATION_ID, build(session, actionFactory, result)));
                    } catch (RuntimeException sessionGone) {
                        // The service released the session with the decode in flight: drop the late update
                        // (the notification manager also discards updates for removed sessions).
                    }
                }

                @Override public void onFailure(Throwable t) {
                    // No cover art: the notification simply keeps its text-only form.
                }
            }, ContextCompat.getMainExecutor(context));
            return null;
        }
        try {
            return Futures.getDone(future);
        } catch (CancellationException | ExecutionException e) {
            return null;
        }
    }

    private Notification build(MediaSession session, ActionFactory actionFactory, @Nullable Bitmap artwork) {
        Player player = session.getPlayer();
        MediaMetadata metadata = player.getMediaMetadata();
        CharSequence title = metadata.title != null ? metadata.title : "";
        CharSequence artist = metadata.artist != null ? metadata.artist : "";
        long duration = player.getDuration();
        if (duration == C.TIME_UNSET || duration <= 0) {
            duration = metadata.durationMs != null ? metadata.durationMs : 0L;
        }
        long position = Math.max(0L, player.getCurrentPosition());
        if (duration > 0) position = Math.min(position, duration);
        // Same rule as ExoPlayerController: buffering with the intent to play counts as playing.
        boolean playing = player.isPlaying()
                || (player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_BUFFERING);
        @Nullable Bitmap art = artwork != null ? scaledArtwork(artwork) : null;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PlaybackService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_playback)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(session.getSessionActivity())
                .setDeleteIntent(actionFactory.createNotificationDismissalIntent(session))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOngoing(playing)
                .setGroup(DefaultMediaNotificationProvider.GROUP_KEY);
        if (art != null) builder.setLargeIcon(art);

        // Prev / play-pause / next as media actions: the system media controls read them from the session.
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, R.drawable.notif_ic_prev),
                getString(R.string.mp_cd_previous), Player.COMMAND_SEEK_TO_PREVIOUS));
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, playing ? R.drawable.notif_ic_pause : R.drawable.notif_ic_play),
                getString(playing ? R.string.mp_cd_pause : R.string.mp_cd_play), Player.COMMAND_PLAY_PAUSE));
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, R.drawable.notif_ic_next),
                getString(R.string.mp_cd_next), Player.COMMAND_SEEK_TO_NEXT));
        MediaStyle style = new MediaStyle(session);
        style.setShowActionsInCompactView(0, 1, 2);
        builder.setStyle(style);

        // The shade shows this custom view everywhere (with the progress bar); collapsed and expanded are equal.
        RemoteViews content = new RemoteViews(context.getPackageName(), R.layout.notification_media);
        fill(content, session, actionFactory, title, artist, position, duration, playing, art);
        builder.setCustomContentView(content);
        builder.setCustomBigContentView(content);
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    /**
     * The notification carries the cover twice (large icon + content view), so it gets a copy of at most
     * {@link #NOTIFICATION_ART_PX} px: keeps the parcel well under the binder limit. Identity-keyed, because the
     * artwork cache hands back the same bitmap per track (at most one scale per track change).
     */
    private Bitmap scaledArtwork(Bitmap source) {
        if (artworkSource == source && artworkScaled != null) return artworkScaled;
        int w = source.getWidth(), h = source.getHeight();
        int longSide = Math.max(w, h);
        Bitmap scaled = longSide <= NOTIFICATION_ART_PX ? source : Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(w * (float) NOTIFICATION_ART_PX / longSide)),
                Math.max(1, Math.round(h * (float) NOTIFICATION_ART_PX / longSide)), true);
        artworkSource = source;
        artworkScaled = scaled;
        return scaled;
    }

    private void fill(RemoteViews views, MediaSession session, ActionFactory actionFactory, CharSequence title,
            CharSequence artist, long positionMs, long durationMs, boolean playing, @Nullable Bitmap artwork) {
        views.setTextViewText(R.id.np_title, title);
        views.setTextViewText(R.id.np_artist, artist);
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        views.setTextViewText(R.id.np_elapsed, PlayerFormat.time(positionMs, locale));
        views.setTextViewText(R.id.np_duration, durationMs > 0 ? PlayerFormat.time(durationMs, locale) : "");
        views.setProgressBar(R.id.np_progress, 1000,
                durationMs > 0 ? (int) Math.min(1000L, positionMs * 1000L / durationMs) : 0, false);
        if (artwork != null) {
            views.setImageViewBitmap(R.id.np_art, artwork);
            views.setViewVisibility(R.id.np_art, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.np_art, View.GONE);
        }
        views.setImageViewResource(R.id.np_prev, R.drawable.notif_ic_prev);
        views.setImageViewResource(R.id.np_play, playing ? R.drawable.notif_ic_pause : R.drawable.notif_ic_play);
        views.setImageViewResource(R.id.np_next, R.drawable.notif_ic_next);
        views.setOnClickPendingIntent(R.id.np_prev,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_SEEK_TO_PREVIOUS));
        views.setOnClickPendingIntent(R.id.np_play,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_PLAY_PAUSE));
        views.setOnClickPendingIntent(R.id.np_next,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_SEEK_TO_NEXT));
    }

    private String getString(int resId) {
        return context.getString(resId);
    }
}

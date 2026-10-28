package ir.mmxkarimi.shockwave.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaNotification.ActionFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper.MediaStyle
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.player.PlayerFormat
import java.util.Locale
import java.util.Objects
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.math.max
import kotlin.math.min

/**
 * The app's media notification: the `notif_icon` mark in the status bar, artwork, title/artist, a progress
 * bar with elapsed / total time and previous · play-pause · next controls — on every Android version, including
 * Android 10 where the stock media layout showed nothing. The MediaStyle style plus the session (the service also
 * injects the platform token) hands the same controls to the system media controls on the devices that have them,
 * and the MediaStyle actions keep working as the compact row. Refreshed by the service about once per second
 * while playing, because the player only emits state changes.
 */
@OptIn(markerClass = [UnstableApi::class])
class PlaybackNotificationProvider(context: Context) : MediaNotification.Provider {
    private val context: Context = context.getApplicationContext()
    private val notificationManager: NotificationManager = Objects.requireNonNull(
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)

    /** Caches decoded artwork across rebuilds, wrapping the session's loader (re-wrapped if it ever changes). */
    private var artworkLoader: BitmapLoader? = null
    private var artworkLoaderSource: BitmapLoader? = null

    /** Scaled copy of the last cover (identity-keyed: the cache returns the same bitmap per track). */
    private var artworkSource: Bitmap? = null
    private var artworkScaled: Bitmap? = null

    override fun createNotification(
        session: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: ActionFactory,
        onNotificationChanged: MediaNotification.Provider.Callback,
    ): MediaNotification {
        ensureChannel()
        val artwork = artwork(session, actionFactory, onNotificationChanged)
        return MediaNotification(NOTIFICATION_ID, build(session, actionFactory, artwork))
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean {
        return false // no custom actions: everything is a media command
    }

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
        return MediaNotification.Provider.NotificationChannelInfo(
                PlaybackService.CHANNEL_ID, getString(R.string.playback_channel_name))
    }

    private fun ensureChannel() {
        if (notificationManager.getNotificationChannel(PlaybackService.CHANNEL_ID) != null) return
        val channel = NotificationChannel(PlaybackService.CHANNEL_ID,
                getString(R.string.playback_channel_name), NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        notificationManager.createNotificationChannel(channel)
    }

    /** @return the artwork when already decoded; otherwise starts a decode and posts an update when it lands */
    private fun artwork(
        session: MediaSession,
        actionFactory: ActionFactory,
        onNotificationChanged: MediaNotification.Provider.Callback,
    ): Bitmap? {
        val metadata = session.getPlayer().getMediaMetadata()
        if (metadata.artworkUri == null) return null
        val sessionLoader = session.getBitmapLoader()
        var loader = artworkLoader
        if (loader == null || artworkLoaderSource !== sessionLoader) {
            artworkLoaderSource = sessionLoader
            loader = CacheBitmapLoader(sessionLoader)
            artworkLoader = loader
        }
        val future = loader.loadBitmapFromMetadata(metadata) ?: return null
        if (!future.isDone()) {
            // The player lives on the main thread; rebuild the notification there like the stock provider does.
            Futures.addCallback(future, object : FutureCallback<Bitmap> {
                override fun onSuccess(result: Bitmap?) {
                    try {
                        onNotificationChanged.onNotificationChanged(
                                MediaNotification(NOTIFICATION_ID, build(session, actionFactory, result)))
                    } catch (sessionGone: RuntimeException) {
                        // The service released the session with the decode in flight: drop the late update
                        // (the notification manager also discards updates for removed sessions).
                    }
                }

                override fun onFailure(t: Throwable) {
                    // No cover art: the notification simply keeps its text-only form.
                }
            }, ContextCompat.getMainExecutor(context))
            return null
        }
        return try {
            Futures.getDone(future)
        } catch (e: CancellationException) {
            null
        } catch (e: ExecutionException) {
            null
        }
    }

    private fun build(session: MediaSession, actionFactory: ActionFactory, artwork: Bitmap?): Notification {
        val player = session.getPlayer()
        val metadata = player.getMediaMetadata()
        val title: CharSequence = metadata.title ?: ""
        val artist: CharSequence = metadata.artist ?: ""
        var duration = player.getDuration()
        if (duration == C.TIME_UNSET || duration <= 0) {
            duration = metadata.durationMs ?: 0L
        }
        var position = max(0L, player.getCurrentPosition())
        if (duration > 0) position = min(position, duration)
        // Same rule as ExoPlayerController: buffering with the intent to play counts as playing.
        val playing = player.isPlaying()
                || (player.getPlayWhenReady() && player.getPlaybackState() == Player.STATE_BUFFERING)
        val art: Bitmap? = if (artwork != null) scaledArtwork(artwork) else null

        val builder = NotificationCompat.Builder(context, PlaybackService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_playback)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(session.getSessionActivity())
                .setDeleteIntent(actionFactory.createNotificationDismissalIntent(session))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOngoing(playing)
                .setGroup(DefaultMediaNotificationProvider.GROUP_KEY)
        if (art != null) builder.setLargeIcon(art)

        // Prev / play-pause / next as media actions: the system media controls read them from the session.
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, R.drawable.notif_ic_prev),
                getString(R.string.mp_cd_previous), Player.COMMAND_SEEK_TO_PREVIOUS))
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, if (playing) R.drawable.notif_ic_pause else R.drawable.notif_ic_play),
                getString(if (playing) R.string.mp_cd_pause else R.string.mp_cd_play), Player.COMMAND_PLAY_PAUSE))
        builder.addAction(actionFactory.createMediaAction(session,
                IconCompat.createWithResource(context, R.drawable.notif_ic_next),
                getString(R.string.mp_cd_next), Player.COMMAND_SEEK_TO_NEXT))
        val style = MediaStyle(session)
        style.setShowActionsInCompactView(0, 1, 2)
        builder.setStyle(style)

        builder.setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15+ Live Updates API method:
            // Native MediaStyle with live status bar chip, dynamic seekbar, and lock screen live activity.
            // Custom RemoteViews are omitted so the OS renders the full interactive system media card.
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            val album = metadata.albumTitle
            if (!album.isNullOrEmpty()) {
                builder.setSubText(album)
            }
            // Request promoted ongoing live status bar chip on Android 15+
            builder.extras.putBoolean("android.requestPromotedOngoing", playing)
        } else {
            // Legacy devices method (< Android 15):
            // Custom RemoteViews with manual progress bar for older Android versions without system media shade.
            val content = RemoteViews(context.getPackageName(), R.layout.notification_media)
            fill(content, session, actionFactory, title, artist, position, duration, playing, art)
            builder.setCustomContentView(content)
            builder.setCustomBigContentView(content)
            if (Build.VERSION.SDK_INT >= 31) {
                builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            }
        }
        return builder.build()
    }

    /**
     * The notification carries the cover twice (large icon + content view), so it gets a copy of at most
     * [NOTIFICATION_ART_PX] px: keeps the parcel well under the binder limit. Identity-keyed, because the
     * artwork cache hands back the same bitmap per track (at most one scale per track change).
     */
    private fun scaledArtwork(source: Bitmap): Bitmap {
        val cachedSource = artworkSource
        val cachedScaled = artworkScaled
        if (cachedSource === source && cachedScaled != null) return cachedScaled
        val w = source.getWidth()
        val h = source.getHeight()
        val longSide = max(w, h)
        val scaled = if (longSide <= NOTIFICATION_ART_PX) source else Bitmap.createScaledBitmap(source,
                max(1, Math.round(w * NOTIFICATION_ART_PX.toFloat() / longSide)),
                max(1, Math.round(h * NOTIFICATION_ART_PX.toFloat() / longSide)), true)
        artworkSource = source
        artworkScaled = scaled
        return scaled
    }

    private fun fill(
        views: RemoteViews,
        session: MediaSession,
        actionFactory: ActionFactory,
        title: CharSequence,
        artist: CharSequence,
        positionMs: Long,
        durationMs: Long,
        playing: Boolean,
        artwork: Bitmap?,
    ) {
        views.setTextViewText(R.id.np_title, title)
        views.setTextViewText(R.id.np_artist, artist)
        val locale = context.getResources().getConfiguration().getLocales().get(0)
        views.setTextViewText(R.id.np_elapsed, PlayerFormat.time(positionMs, locale))
        views.setTextViewText(R.id.np_duration, if (durationMs > 0) PlayerFormat.time(durationMs, locale) else "")
        views.setProgressBar(R.id.np_progress, 1000,
                if (durationMs > 0) min(1000L, positionMs * 1000L / durationMs).toInt() else 0, false)
        if (artwork != null) {
            views.setImageViewBitmap(R.id.np_art, artwork)
            views.setViewVisibility(R.id.np_art, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.np_art, View.GONE)
        }
        views.setImageViewResource(R.id.np_prev, R.drawable.notif_ic_prev)
        views.setImageViewResource(R.id.np_play, if (playing) R.drawable.notif_ic_pause else R.drawable.notif_ic_play)
        views.setImageViewResource(R.id.np_next, R.drawable.notif_ic_next)
        views.setOnClickPendingIntent(R.id.np_prev,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_SEEK_TO_PREVIOUS))
        views.setOnClickPendingIntent(R.id.np_play,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.np_next,
                actionFactory.createMediaActionPendingIntent(session, Player.COMMAND_SEEK_TO_NEXT))
    }

    private fun getString(resId: Int): String {
        return context.getString(resId)
    }

    companion object {
        private val NOTIFICATION_ID: Int = DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID

        /** Covers are decoded up to 512 px; the notification gets a smaller copy so its parcel stays small. */
        private const val NOTIFICATION_ART_PX = 256
    }
}
package ir.mmxkarimi.shockwave.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory

/**
 * The one ExoPlayer of the process, shared by [ExoPlayerController] (UI side) and [PlaybackService]
 * (notification / media buttons). Both live in the same process, so no IPC or async MediaController handshake is
 * needed. It is created on first use (main thread), restored from [PlaybackStore] in the paused state, and
 * kept for the life of the process; the service only releases codecs with `stop()` when idle.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class PlayerHolder private constructor() {
    companion object {
        private var player: ExoPlayer? = null
        private var store: PlaybackStore? = null

        /** Main thread only. */
        @JvmStatic
        @Synchronized
        @JvmName("get")
        internal fun get(context: Context): ExoPlayer {
            var p = player
            if (p == null) {
                val app = context.getApplicationContext()
                // Local files: small, bounded buffers (a few seconds, <= 4 MB) instead of the streaming defaults.
                val loadControl: LoadControl = DefaultLoadControl.Builder()
                        .setBufferDurationsMsForLocalPlayback(2_000, 8_000, 500, 1_000)
                        .setTargetBufferBytes(4 * 1024 * 1024)
                        .setBackBuffer(0, false)
                        .build()
                // Audio only: no video/text/metadata renderers to initialise or hold memory for.
                p = ExoPlayer.Builder(app,
                        RenderersFactory { handler, video, audio, text, metadata ->
                            arrayOf<Renderer>(
                                    MediaCodecAudioRenderer(app, MediaCodecSelector.DEFAULT, handler, audio))
                        })
                        .setMediaSourceFactory(DefaultMediaSourceFactory(app,
                                DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)))
                        .setLoadControl(loadControl)
                        .setAudioAttributes(AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(), /* handleAudioFocus= */ true)
                        .setHandleAudioBecomingNoisy(true)
                        .setWakeMode(C.WAKE_MODE_LOCAL) // CPU wake lock only while actually playing local files
                        .build()
                val newStore = PlaybackStore(app)
                store = newStore
                newStore.restoreAndAttach(p)
                player = p
            }
            return p
        }

        @JvmStatic
        @Synchronized
        @JvmName("store")
        internal fun store(context: Context): PlaybackStore {
            get(context)
            return store!!
        }
    }
}
package ir.mmxkarimi.shockwave.di

import android.content.Context
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.playback.ExoPlayerController
import ir.mmxkarimi.shockwave.playback.PlayCountListener
import java.util.function.Consumer

/** Factory for the real playback stack; `AppContainer` calls it instead of constructing the fake. */
class PlaybackModule private constructor() {
    companion object {
        /**
         * @param playCountListener called once per real listen of a track, e.g. `library::recordPlay`
         * @param nextTrackListener optional: called while playing whenever the next queued track changes (null when
         *                           nothing needs the hint)
         */
        @JvmStatic fun createController(
            context: Context,
            playCountListener: PlayCountListener?,
            nextTrackListener: Consumer<Track>?,
        ): PlayerController {
            return ExoPlayerController(context, playCountListener, nextTrackListener)
        }
    }
}

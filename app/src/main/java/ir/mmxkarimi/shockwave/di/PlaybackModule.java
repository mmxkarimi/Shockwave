package ir.mmxkarimi.shockwave.di;

import android.content.Context;
import androidx.annotation.Nullable;
import ir.mmxkarimi.shockwave.domain.model.Track;
import ir.mmxkarimi.shockwave.domain.repository.PlayerController;
import ir.mmxkarimi.shockwave.playback.ExoPlayerController;
import ir.mmxkarimi.shockwave.playback.PlayCountListener;
import java.util.function.Consumer;

/** Factory for the real playback stack; {@code AppContainer} calls it instead of constructing the fake. */
public final class PlaybackModule {
    private PlaybackModule() {}

    /**
     * @param playCountListener called once per real listen of a track, e.g. {@code library::recordPlay}
     * @param nextTrackListener called while playing whenever the next queued track changes, e.g. waveform prefetch
     */
    public static PlayerController createController(Context context, @Nullable PlayCountListener playCountListener,
            @Nullable Consumer<Track> nextTrackListener) {
        return new ExoPlayerController(context, playCountListener, nextTrackListener);
    }
}

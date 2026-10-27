package ir.mmxkarimi.shockwave;

import ir.mmxkarimi.shockwave.di.AppContainer;

/** Robolectric application for {@link ScreenRenderTest}: the shell runs on the in-memory fakes (sample library). */
public class FakeRenderApp extends ShockwaveApp {
    private AppContainer fakes;

    @Override public AppContainer getContainer() {
        if (fakes == null) fakes = new AppContainer(this, true);
        return fakes;
    }
}

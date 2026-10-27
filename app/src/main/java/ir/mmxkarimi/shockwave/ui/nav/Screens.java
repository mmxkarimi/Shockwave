package ir.mmxkarimi.shockwave.ui.nav;

import android.content.Context;
import androidx.fragment.app.Fragment;
import ir.mmxkarimi.shockwave.ShockwaveApp;
import ir.mmxkarimi.shockwave.di.AppContainer;

/** Small helpers shared by the screen fragments. */
public final class Screens {
    private Screens() { }

    public static AppContainer container(Fragment fragment) {
        return ((ShockwaveApp) fragment.requireContext().getApplicationContext()).getContainer();
    }

    public static AppContainer container(Context context) {
        return ((ShockwaveApp) context.getApplicationContext()).getContainer();
    }

    public static void openTrackList(Fragment f, ScreenNavigator.TrackList kind, String id) {
        ScreenNavigator nav = ScreenNavigator.from(f.requireContext());
        if (nav != null) nav.openTrackList(kind, id);
    }

    public static void openCollection(Fragment f, ScreenNavigator.Collection kind) {
        ScreenNavigator nav = ScreenNavigator.from(f.requireContext());
        if (nav != null) nav.openCollection(kind);
    }
}

package ir.mmxkarimi.shockwave.ui.nav

import android.content.Context
import androidx.fragment.app.Fragment
import ir.mmxkarimi.shockwave.ShockwaveApp
import ir.mmxkarimi.shockwave.di.AppContainer

/** Small helpers shared by screens. */
object Screens {
    @JvmStatic
    fun container(fragment: Fragment): AppContainer {
        return (fragment.requireContext().applicationContext as ShockwaveApp).getContainer()
    }

    @JvmStatic
    fun container(context: Context): AppContainer {
        return (context.applicationContext as ShockwaveApp).getContainer()
    }

    @JvmStatic
    fun openTrackList(f: Fragment, kind: ScreenNavigator.TrackList, id: String?) {
        ScreenNavigator.from(f.requireContext())?.openTrackList(kind, id)
    }

    @JvmStatic
    fun openCollection(f: Fragment, kind: ScreenNavigator.Collection) {
        ScreenNavigator.from(f.requireContext())?.openCollection(kind)
    }
}

package ir.mmxkarimi.shockwave

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Trace
import ir.mmxkarimi.shockwave.data.settings.BatterySaverMode
import ir.mmxkarimi.shockwave.di.AppContainer
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.MotionPolicy

open class ShockwaveApp : Application() {
    private lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        Trace.beginSection("ShockwaveApp.onCreate") // visible in Perfetto; the container is lazy except the player
        container = AppContainer(this)
        Trace.endSection()
        // Cheap: one static write per change of the setting, no polling.
        container.settings().batterySaver().observeForever { mode ->
            MotionPolicy.setBatterySaverOverride(
                if (mode == BatterySaverMode.AUTO) null else mode == BatterySaverMode.ON,
            )
        }
        // Below API 33 AppCompat keeps the chosen locale only in memory: re-apply the stored choice on every start.
        if (Build.VERSION.SDK_INT < 33) ThemeApplier.applyLanguage(container.settings().language().value ?: "")
        registerActivityLifecycleCallbacks(FirstActivityStart())
    }

    open fun getContainer(): AppContainer {
        return container
    }

    /** Fires [AppContainer.onAppForeground] for the first started activity, then removes itself. */
    private inner class FirstActivityStart : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            unregisterActivityLifecycleCallbacks(this)
            container.onAppForeground()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }
        override fun onActivityResumed(activity: Activity) { }
        override fun onActivityPaused(activity: Activity) { }
        override fun onActivityStopped(activity: Activity) { }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }
        override fun onActivityDestroyed(activity: Activity) { }
    }
}

package ir.mmxkarimi.shockwave

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.databinding.ActivityMainBinding
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.MiniPlayerBar
import ir.mmxkarimi.shockwave.ui.home.HomeFragment
import ir.mmxkarimi.shockwave.ui.library.CollectionFragment
import ir.mmxkarimi.shockwave.ui.library.LibraryFragment
import ir.mmxkarimi.shockwave.ui.library.TrackListFragment
import ir.mmxkarimi.shockwave.ui.nav.AudioPermission
import ir.mmxkarimi.shockwave.ui.nav.PermissionFragment
import ir.mmxkarimi.shockwave.ui.nav.PlayerNavigator
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator.Collection
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator.Tab
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator.TrackList
import ir.mmxkarimi.shockwave.ui.nav.ShellViewModel
import ir.mmxkarimi.shockwave.ui.player.PlayerFormat
import ir.mmxkarimi.shockwave.ui.player.PlayerFragment
import ir.mmxkarimi.shockwave.ui.queue.QueueFragment
import ir.mmxkarimi.shockwave.ui.search.SearchFragment
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.settings.SettingsFragment
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme
import ir.mmxkarimi.shockwave.ui.theme.ThemeController

/**
 * Single-activity shell implementing PlayerNavigator, ScreenNavigator, and PermissionFragment.Host.
 */
class MainActivity : AppCompatActivity(), PlayerNavigator, ScreenNavigator, PermissionFragment.Host {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shell: ShellViewModel
    private var permissionFlow = false
    private var backToHome: OnBackPressedCallback? = null

    private val notificationPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (java.lang.Boolean.TRUE == granted) {
                try {
                    startService(Intent(this, ir.mmxkarimi.shockwave.playback.PlaybackService::class.java))
                } catch (ignored: Exception) {
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = PrefsSettingsStore(this).getTheme()
        ThemeApplier.applyNightMode(theme)
        setTheme(ThemeApplier.themeRes(theme))
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shell = ViewModelProvider(
            this,
            ShellViewModel.factory((application as ShockwaveApp).getContainer().player)
        )[ShellViewModel::class.java]

        setupComposeChrome()

        backToHome = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                openTab(Tab.HOME)
            }
        }
        onBackPressedDispatcher.addCallback(this, backToHome!!)
        supportFragmentManager.addOnBackStackChangedListener { updateBackToHome() }

        if (savedInstanceState == null) {
            if (AudioPermission.granted(this) || PermissionPrefs.skipped(this)) {
                if (!applyShortcut(intent)) showTab(Tab.HOME)
                askNotificationsOnce()
            } else {
                permissionFlow = true
                supportFragmentManager.beginTransaction()
                    .replace(R.id.screen_container, PermissionFragment()).commit()
            }
        } else {
            permissionFlow = savedInstanceState.getBoolean(STATE_PERMISSION)
            val tabOrd = savedInstanceState.getInt(STATE_TAB, 0)
            shell.selectTab(Tab.values()[tabOrd])
        }
        updateChromeVisibility()
    }

    private fun setupComposeChrome() {
        binding.chrome.visibility = View.VISIBLE
        binding.chrome.removeAllViews()
        val composeView = ComposeView(this).apply {
            setContent {
                val playback by shell.getPlayback().observeAsState()
                val accent by ThemeController.get(this@MainActivity).accent().observeAsState()

                val track = playback?.currentTrack
                val progressFraction = PlayerFormat.fraction(playback?.positionMs ?: 0L, playback?.durationMs ?: 0L)

                ShockwaveTheme(accentScheme = accent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (track != null) {
                            MiniPlayerBar(
                                track = track,
                                isPlaying = playback?.playing == true,
                                progressFraction = progressFraction,
                                onClick = { openFullPlayer() },
                                onPlayPause = { shell.togglePlayPause() },
                                onNext = { shell.next() }
                            )
                        }
                    }
                }
            }
        }
        binding.chrome.addView(composeView)
    }

    private fun updateChromeVisibility() {
        binding.chrome.visibility = if (permissionFlow) View.GONE else View.VISIBLE
        binding.bottomScrim.visibility = if (permissionFlow) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putBoolean(STATE_PERMISSION, permissionFlow)
        out.putInt(STATE_TAB, (shell.getTab().value ?: Tab.HOME).ordinal)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyShortcut(intent)
    }

    private fun applyShortcut(intent: Intent?): Boolean {
        if (intent == null || permissionFlow) return false
        val action = intent.action ?: return false
        when (action) {
            ACTION_SHORTCUT_RECENT -> {
                intent.action = null
                openTab(Tab.LIBRARY)
                openTrackList(TrackList.RECENT, null)
                return true
            }
            ACTION_SHORTCUT_FAVORITES -> {
                intent.action = null
                openTab(Tab.LIBRARY)
                openTrackList(TrackList.LIKED, null)
                return true
            }
            ACTION_SHORTCUT_PLAY -> {
                intent.action = null
                openTab(Tab.HOME)
                playFromShortcut()
                return true
            }
        }
        return false
    }

    private fun playFromShortcut() {
        val c = (application as ShockwaveApp).getContainer()
        val state = c.player.getState().value
        if (state != null && state.currentTrack != null) {
            c.player.resume()
            return
        }
        val recent: LiveData<List<Track>> = c.library.getRecentlySaved()
        recent.observe(this, object : Observer<List<Track>> {
            override fun onChanged(tracks: List<Track>) {
                if (tracks.isEmpty()) return
                recent.removeObserver(this)
                c.player.play(tracks, 0, getString(R.string.library_recently_saved))
            }
        })
    }

    private fun updateBackToHome() {
        backToHome?.isEnabled = !permissionFlow && shell.getTab().value != Tab.HOME &&
                supportFragmentManager.backStackEntryCount == 0
    }

    private fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (PermissionPrefs.notificationsAsked(this) && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            return
        }
        PermissionPrefs.markNotificationsAsked(this)
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun openTab(tab: Tab) {
        val fm = supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        if (shell.getTab().value == tab && fm.findFragmentById(R.id.screen_container) != null) return
        showTab(tab)
    }

    private fun showTab(tab: Tab) {
        val f: Fragment = when (tab) {
            Tab.HOME -> HomeFragment()
            Tab.SEARCH -> SearchFragment()
            Tab.LIBRARY -> LibraryFragment()
            Tab.SETTINGS -> SettingsFragment()
        }
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            .replace(R.id.screen_container, f)
            .commit()
        shell.selectTab(tab)
    }

    override fun openTrackList(kind: TrackList, id: String?) {
        push(TrackListFragment.newInstance(kind, id))
    }

    override fun openCollection(kind: Collection) {
        push(CollectionFragment.newInstance(kind))
    }

    private fun push(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.animator.fade_in, android.R.animator.fade_out,
                android.R.animator.fade_in, android.R.animator.fade_out
            )
            .replace(R.id.screen_container, f)
            .addToBackStack(null)
            .commit()
    }

    override fun openFullPlayer() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PLAYER_TAG) != null) return
        val f = PlayerFragment.newInstance()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.animator.fade_in, android.R.animator.fade_out,
                android.R.animator.fade_in, android.R.animator.fade_out
            )
            .add(R.id.player_container, f, PLAYER_TAG)
            .addToBackStack(PLAYER_TAG)
            .commit()
    }

    override fun openQueue() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(QUEUE_TAG) != null) return
        val f = QueueFragment.newInstance()
        f.show(fm, QUEUE_TAG)
    }

    override fun closeFullPlayer() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PLAYER_TAG) != null) {
            fm.popBackStack(PLAYER_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    override fun onPermissionFlowDone() {
        if (!permissionFlow) return
        permissionFlow = false
        PermissionPrefs.markSkipped(this)
        if (AudioPermission.granted(this)) {
            (application as ShockwaveApp).getContainer().onAudioPermissionGranted()
        }
        updateChromeVisibility()
        showTab(Tab.HOME)
        askNotificationsOnce()
    }

    private object PermissionPrefs {
        fun skipped(c: Context): Boolean {
            return c.getSharedPreferences("nav", MODE_PRIVATE).getBoolean("perm_done", false)
        }

        fun notificationsAsked(c: Context): Boolean {
            return c.getSharedPreferences("nav", MODE_PRIVATE).getBoolean("notif_asked", false)
        }

        fun markNotificationsAsked(c: Context) {
            c.getSharedPreferences("nav", MODE_PRIVATE).edit().putBoolean("notif_asked", true).apply()
        }

        fun markSkipped(c: Context) {
            c.getSharedPreferences("nav", MODE_PRIVATE).edit().putBoolean("perm_done", true).apply()
        }
    }

    companion object {
        private const val ACTION_SHORTCUT_PLAY = "ir.mmxkarimi.shockwave.SHORTCUT_PLAY"
        private const val ACTION_SHORTCUT_RECENT = "ir.mmxkarimi.shockwave.SHORTCUT_RECENT"
        private const val ACTION_SHORTCUT_FAVORITES = "ir.mmxkarimi.shockwave.SHORTCUT_FAVORITES"

        private const val PLAYER_TAG = "player"
        private const val QUEUE_TAG = "queue"
        private const val STATE_PERMISSION = "permission_flow"
        private const val STATE_TAB = "tab"
    }
}

package ir.mmxkarimi.shockwave

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.databinding.ActivityMainBinding
import ir.mmxkarimi.shockwave.di.AppContainer
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
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
import ir.mmxkarimi.shockwave.ui.settings.SettingsStore
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.AccentScheme
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
        binding.nav.visibility = View.GONE
        binding.mini.visibility = View.GONE

        binding.chrome.removeAllViews()
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                val playback by shell.getPlayback().observeAsState()
                val currentTab by shell.getTab().observeAsState(Tab.HOME)
                val accent by ThemeController.get(this@MainActivity).accent().observeAsState()

                val track = playback?.currentTrack
                val progressFraction = PlayerFormat.fraction(playback?.positionMs ?: 0L, playback?.durationMs ?: 0L)

                ShockwaveTheme(accentScheme = accent) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (track != null) {
                            MiniPlayerBar(
                                track = track,
                                isPlaying = playback?.isPlaying == true,
                                progressFraction = progressFraction,
                                onClick = { openFullPlayer() },
                                onPlayPause = { shell.togglePlayPause() },
                                onNext = { shell.next() }
                            )
                        }

                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            NavigationBarItem(
                                selected = currentTab == Tab.HOME,
                                onClick = { openTab(Tab.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.SEARCH,
                                onClick = { openTab(Tab.SEARCH) },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                label = { Text("Search") }
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.LIBRARY,
                                onClick = { openTab(Tab.LIBRARY) },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                label = { Text("Library") }
                            )
                            NavigationBarItem(
                                selected = currentTab == Tab.SETTINGS,
                                onClick = { openTab(Tab.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
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

    override fun onNewIntent(intent: Intent?) {
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
        val state = c.player.state.value
        if (state != null && state.currentTrack != null) {
            c.player.resume()
            return
        }
        val recent: LiveData<List<Track>> = c.library.recentlySaved
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

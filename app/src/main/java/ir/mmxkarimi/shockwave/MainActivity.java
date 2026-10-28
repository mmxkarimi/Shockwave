package ir.mmxkarimi.shockwave;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import ir.mmxkarimi.shockwave.databinding.ActivityMainBinding;
import ir.mmxkarimi.shockwave.di.AppContainer;
import ir.mmxkarimi.shockwave.domain.model.PlaybackState;
import ir.mmxkarimi.shockwave.domain.model.Track;
import ir.mmxkarimi.shockwave.ui.home.HomeFragment;
import ir.mmxkarimi.shockwave.ui.library.CollectionFragment;
import ir.mmxkarimi.shockwave.ui.library.LibraryFragment;
import ir.mmxkarimi.shockwave.ui.library.TrackListFragment;
import ir.mmxkarimi.shockwave.ui.nav.AudioPermission;
import ir.mmxkarimi.shockwave.ui.nav.PermissionFragment;
import ir.mmxkarimi.shockwave.ui.nav.PlayerNavigator;
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator;
import ir.mmxkarimi.shockwave.ui.nav.ShellViewModel;
import ir.mmxkarimi.shockwave.ui.search.SearchFragment;
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore;
import ir.mmxkarimi.shockwave.ui.settings.SettingsFragment;
import ir.mmxkarimi.shockwave.ui.settings.SettingsStore;
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier;
import ir.mmxkarimi.shockwave.ui.theme.ThemeController;
import ir.mmxkarimi.shockwave.ui.widget.FloatingNavView;
import ir.mmxkarimi.shockwave.ui.widget.MiniPlayerView;
import java.util.List;

/**
 * Single-activity shell: top-level tabs in {@code screen_container}, detail screens pushed on the back stack,
 * the mini-player and floating nav at the bottom, and the full player (T4) layered on top in {@code player_container}.
 */
public class MainActivity extends AppCompatActivity
        implements PlayerNavigator, ScreenNavigator, PermissionFragment.Host {

    /** Launcher shortcut actions (res/xml/shortcuts.xml); each opens the app at the right place. */
    private static final String ACTION_SHORTCUT_PLAY = "ir.mmxkarimi.shockwave.SHORTCUT_PLAY";
    private static final String ACTION_SHORTCUT_RECENT = "ir.mmxkarimi.shockwave.SHORTCUT_RECENT";
    private static final String ACTION_SHORTCUT_FAVORITES = "ir.mmxkarimi.shockwave.SHORTCUT_FAVORITES";

    /** T4's screens, resolved by name so the shell compiles before (and independently of) them. */
    private static final String PLAYER_FRAGMENT = "ir.mmxkarimi.shockwave.ui.player.PlayerFragment";
    private static final String QUEUE_FRAGMENT = "ir.mmxkarimi.shockwave.ui.queue.QueueFragment";
    private static final String PLAYER_TAG = "player";
    private static final String QUEUE_TAG = "queue";
    private static final String STATE_PERMISSION = "permission_flow";
    private static final String STATE_TAB = "tab";

    private ActivityMainBinding binding;
    private ShellViewModel shell;
    private boolean permissionFlow;
    private OnBackPressedCallback backToHome;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SettingsStore.Theme theme = new PrefsSettingsStore(this).getTheme();
        ThemeApplier.applyNightMode(theme);
        setTheme(ThemeApplier.themeRes(theme));
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets();

        shell = new ViewModelProvider(this,
                ShellViewModel.factory(((ShockwaveApp) getApplication()).getContainer().player))
                .get(ShellViewModel.class);

        bindNav();
        bindMiniPlayer();

        backToHome = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                openTab(Tab.HOME);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToHome);
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateBackToHome);

        if (savedInstanceState == null) {
            if (AudioPermission.granted(this) || PermissionPrefs.skipped(this)) {
                if (!applyShortcut(getIntent())) showTab(Tab.HOME);
                askNotificationsOnce();
            } else {
                permissionFlow = true;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.screen_container, new PermissionFragment()).commit();
            }
        } else {
            permissionFlow = savedInstanceState.getBoolean(STATE_PERMISSION);
            // The restored fragment survives process death, the ViewModel's tab does not.
            shell.selectTab(Tab.values()[savedInstanceState.getInt(STATE_TAB, 0)]);
        }
        binding.chrome.setVisibility(permissionFlow ? View.GONE : View.VISIBLE);
        binding.bottomScrim.setVisibility(permissionFlow ? View.GONE : View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(STATE_PERMISSION, permissionFlow);
        out.putInt(STATE_TAB, shell.getTab().getValue().ordinal());
    }

    @Override protected void onNewIntent(@Nullable Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyShortcut(intent);
    }

    /**
     * One of the launcher shortcuts (res/xml/shortcuts.xml): open Recently added / Favorites, or start playback.
     * The action is consumed, so a configuration change or process restore never replays it.
     *
     * @return true when the intent was a shortcut and has been handled
     */
    private boolean applyShortcut(@Nullable Intent intent) {
        if (intent == null || permissionFlow) return false;
        String action = intent.getAction();
        if (action == null) return false;
        switch (action) {
            case ACTION_SHORTCUT_RECENT -> {
                intent.setAction(null);
                openTab(Tab.LIBRARY);
                openTrackList(TrackList.RECENT, null);
                return true;
            }
            case ACTION_SHORTCUT_FAVORITES -> {
                intent.setAction(null);
                openTab(Tab.LIBRARY);
                openTrackList(TrackList.LIKED, null);
                return true;
            }
            case ACTION_SHORTCUT_PLAY -> {
                intent.setAction(null);
                openTab(Tab.HOME);
                playFromShortcut();
                return true;
            }
            default -> { }
        }
        return false;
    }

    /** Resume the current queue; with nothing queued, start from the most recently added songs. */
    private void playFromShortcut() {
        AppContainer c = ((ShockwaveApp) getApplication()).getContainer();
        PlaybackState state = c.player.getState().getValue();
        if (state != null && state.currentTrack() != null) {
            c.player.resume();
            return;
        }
        LiveData<List<Track>> recent = c.library.getRecentlySaved();
        recent.observe(this, new Observer<List<Track>>() {
            @Override public void onChanged(List<Track> tracks) {
                if (tracks == null || tracks.isEmpty()) return; // the launch scan may still be running
                recent.removeObserver(this);
                c.player.play(tracks, 0, getString(R.string.library_recently_saved));
            }
        });
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            binding.screenContainer.setPadding(bars.left, bars.top, bars.right, 0);
            // Keep the mini-player + nav pill clearly above the screen edge, even with no nav inset reported.
            int gap = Math.round(24 * getResources().getDisplayMetrics().density);
            binding.chrome.setPadding(bars.left, 0, bars.right, bars.bottom + gap);
            return insets;
        });
    }

    // ---- floating nav + mini-player (T2 widgets) ----

    private static final int[] NAV_IDS = {R.id.mp_nav_home, R.id.mp_nav_search, R.id.mp_nav_library, R.id.mp_nav_settings};

    private void bindNav() {
        binding.nav.addDefaultItems();
        binding.nav.setOnItemSelectedListener(new FloatingNavView.OnItemSelectedListener() {
            @Override public void onItemSelected(int id) {
                for (int i = 0; i < NAV_IDS.length; i++) if (NAV_IDS[i] == id) openTab(Tab.values()[i]);
            }

            @Override public void onItemReselected(int id) {
                openTab(shell.getTab().getValue());
            }
        });
        shell.getTab().observe(this, selected -> {
            int id = NAV_IDS[selected.ordinal()];
            if (binding.nav.getSelectedItem() != id) binding.nav.setSelectedItem(id, true);
            updateBackToHome();
        });
    }

    private void updateBackToHome() {
        if (backToHome == null) return;
        backToHome.setEnabled(!permissionFlow && shell.getTab().getValue() != Tab.HOME
                && getSupportFragmentManager().getBackStackEntryCount() == 0);
    }

    private void bindMiniPlayer() {
        binding.mini.setListener(new MiniPlayerView.Listener() {
            @Override public void onOpen() { openFullPlayer(); }

            @Override public void onPlayPause() { shell.togglePlayPause(); }

            @Override public void onNext() { shell.next(); }

            @Override public void onPrevious() { shell.previous(); }

            @Override public void onDismiss() { shell.pause(); }
        });
        ThemeController.get(this).accent().observe(this, binding.mini::setAccent);
        shell.getPlayback().observe(this, this::renderMini);
    }

    @Nullable private String miniTrackId;

    private void renderMini(PlaybackState s) {
        Track t = s == null ? null : s.currentTrack();
        binding.mini.setVisibility(t == null ? View.GONE : View.VISIBLE);
        if (t == null) return;
        if (!t.id().equals(miniTrackId)) {
            miniTrackId = t.id();
            binding.mini.bind(t);
        }
        if (s.playing()) askNotificationsOnce();
        binding.mini.setPlaying(s.playing());
        binding.mini.setBuffering(s.buffering());
        binding.mini.setProgress(s.positionMs(), s.durationMs());
    }

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    try {
                        startService(new Intent(this, ir.mmxkarimi.shockwave.playback.PlaybackService.class));
                    } catch (Exception ignored) { }
                }
            });

    /** Android 13+: the media notification needs it. Asked once, on the first play; playback works if denied. */
    private void askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (PermissionPrefs.notificationsAsked(this) && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            return;
        }
        PermissionPrefs.markNotificationsAsked(this);
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    // ---- ScreenNavigator ----

    @Override public void openTab(Tab tab) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (shell.getTab().getValue() == tab && fm.findFragmentById(R.id.screen_container) != null) return;
        showTab(tab);
    }

    private void showTab(Tab tab) {
        Fragment f = switch (tab) {
            case HOME -> new HomeFragment();
            case SEARCH -> new SearchFragment();
            case LIBRARY -> new LibraryFragment();
            case SETTINGS -> new SettingsFragment();
        };
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.screen_container, f)
                .commit();
        shell.selectTab(tab);
    }

    @Override public void openTrackList(TrackList kind, @Nullable String id) {
        push(TrackListFragment.newInstance(kind, id));
    }

    @Override public void openCollection(Collection kind) {
        push(CollectionFragment.newInstance(kind));
    }

    private void push(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out)
                .replace(R.id.screen_container, f)
                .addToBackStack(null)
                .commit();
    }

    // ---- PlayerNavigator ----

    @Override public void openFullPlayer() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(PLAYER_TAG) != null) return;
        Fragment f = instantiate(PLAYER_FRAGMENT);
        if (f == null) {
            Toast.makeText(this, R.string.nav_player_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out,
                        android.R.animator.fade_in, android.R.animator.fade_out)
                .add(R.id.player_container, f, PLAYER_TAG)
                .addToBackStack(PLAYER_TAG)
                .commit();
    }

    @Override public void openQueue() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(QUEUE_TAG) != null) return;
        Fragment f = instantiate(QUEUE_FRAGMENT);
        if (f == null) return;
        if (f instanceof DialogFragment) {
            ((DialogFragment) f).show(fm, QUEUE_TAG);
        } else {
            fm.beginTransaction().setReorderingAllowed(true)
                    .add(R.id.player_container, f, QUEUE_TAG).addToBackStack(QUEUE_TAG).commit();
        }
    }

    @Override public void closeFullPlayer() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(PLAYER_TAG) != null) {
            fm.popBackStack(PLAYER_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @Nullable private Fragment instantiate(String className) {
        try {
            return getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), className);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- PermissionFragment.Host ----

    @Override public void onPermissionFlowDone() {
        if (!permissionFlow) return;
        permissionFlow = false;
        PermissionPrefs.markSkipped(this);
        // "Choose folders" also ends the flow without the grant: only scan when access really exists.
        if (AudioPermission.granted(this)) ((ShockwaveApp) getApplication()).getContainer().onAudioPermissionGranted();
        binding.chrome.setVisibility(View.VISIBLE);
        binding.bottomScrim.setVisibility(View.VISIBLE);
        showTab(Tab.HOME);
        askNotificationsOnce();
    }

    /** Remembers that the first-run screen was passed, so it is not shown on every launch. */
    private static final class PermissionPrefs {
        static boolean skipped(android.content.Context c) {
            return c.getSharedPreferences("nav", MODE_PRIVATE).getBoolean("perm_done", false);
        }

        static boolean notificationsAsked(android.content.Context c) {
            return c.getSharedPreferences("nav", MODE_PRIVATE).getBoolean("notif_asked", false);
        }

        static void markNotificationsAsked(android.content.Context c) {
            c.getSharedPreferences("nav", MODE_PRIVATE).edit().putBoolean("notif_asked", true).apply();
        }

        static void markSkipped(android.content.Context c) {
            c.getSharedPreferences("nav", MODE_PRIVATE).edit().putBoolean("perm_done", true).apply();
        }
    }
}

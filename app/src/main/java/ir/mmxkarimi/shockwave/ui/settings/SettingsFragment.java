package ir.mmxkarimi.shockwave.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import ir.mmxkarimi.shockwave.R;
import ir.mmxkarimi.shockwave.databinding.SettingsFragmentBinding;
import ir.mmxkarimi.shockwave.di.AppContainer;
import ir.mmxkarimi.shockwave.ui.nav.AudioPermission;
import ir.mmxkarimi.shockwave.ui.nav.Screens;
import ir.mmxkarimi.shockwave.ui.settings.SettingsStore.Theme;
import ir.mmxkarimi.shockwave.ui.theme.ThemeController;
import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Settings (DESIGN.md 09). Applying theme/language is done here (View layer); the ViewModel only stores. */
public final class SettingsFragment extends Fragment {
    private SettingsFragmentBinding b;
    private SettingsViewModel vm;
    private TextView[] sleepChips;
    /** Feedback of the last scan ("Added …"), shown until the next one starts. */
    @Nullable private CharSequence scanResult;
    @Nullable private AlertDialog foldersDialog;

    private final ActivityResultLauncher<Intent> folderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri uri = result.getData() == null ? null : result.getData().getData();
                if (result.getResultCode() != Activity.RESULT_OK || uri == null) return;
                String path = FolderPath.fromTreeUri(uri);
                if (path == null) {
                    Toast.makeText(requireContext(), R.string.settings_folder_add_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Set<String> folders = currentFolders();
                if (folders.add(path)) applyFolders(folders);
            });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle state) {
        b = SettingsFragmentBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        vm = new ViewModelProvider(this, SettingsViewModel.factory(new PrefsSettingsStore(requireContext())))
                .get(SettingsViewModel.class);

        TextView[] themes = {b.themeSystem, b.themeLight, b.themeDark, b.themeBlack};
        for (int i = 0; i < themes.length; i++) {
            Theme t = Theme.values()[i];
            themes[i].setOnClickListener(v -> {
                Theme before = Objects.requireNonNull(vm.getState().getValue()).theme();
                if (before == t) return;
                vm.setTheme(t);
                ThemeApplier.applyNightMode(t);
                // Black is a separate theme resource (not just night mode), so switching to/from it recreates.
                if (before == Theme.BLACK || t == Theme.BLACK) requireActivity().recreate();
            });
        }
        TextView[] langs = {b.langSystem, b.langEn, b.langFa};
        String[] tags = {SettingsStore.LANG_SYSTEM, "en", "fa"};
        for (int i = 0; i < langs.length; i++) {
            String tag = tags[i];
            langs[i].setOnClickListener(v -> {
                if (tag.equals(Objects.requireNonNull(vm.getState().getValue()).language())) return;
                vm.setLanguage(tag);
                ThemeApplier.applyLanguage(tag);
            });
        }

        NumberFormat nf = NumberFormat.getIntegerInstance(getResources().getConfiguration().getLocales().get(0));
        int[] presets = SettingsViewModel.SLEEP_PRESETS;
        sleepChips = new TextView[presets.length];
        for (int i = 0; i < presets.length; i++) {
            int m = presets[i];
            TextView chip = new TextView(new ContextThemeWrapper(requireContext(), R.style.Search_Chip), null, 0);
            chip.setText(m == 0 ? getString(R.string.settings_sleep_end) : getString(R.string.settings_sleep_minutes_f, nf.format(m)));
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    Math.round(48 * getResources().getDisplayMetrics().density));
            lp.setMarginEnd(Math.round(8 * getResources().getDisplayMetrics().density));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> vm.setSleepDefault(m));
            b.sleepRow.addView(chip);
            sleepChips[i] = chip;
        }

        b.artworkColors.setOnCheckedChangeListener((v, on) -> {
            vm.setArtworkColors(on);
            ThemeController.get(requireContext()).setArtworkColorsEnabled(on);
        });
        b.onlineLyrics.setOnCheckedChangeListener((v, on) -> vm.setOnlineLyrics(on));
        b.batterySaver.setOnCheckedChangeListener((v, on) -> vm.setBatterySaver(on));
        b.version.setText(getString(R.string.settings_version, versionName()));

        AppContainer c = Screens.container(this);
        b.folders.setOnClickListener(v -> showFoldersDialog());
        b.rescan.setOnClickListener(v -> rescan());
        c.settings().includedFolders().observe(getViewLifecycleOwner(), folders ->
                b.foldersSub.setText(folders == null || folders.isEmpty()
                        ? getString(R.string.settings_folders_all)
                        : getResources().getQuantityString(R.plurals.settings_folders_count,
                                folders.size(), folders.size())));
        c.libraryAdmin().isScanning().observe(getViewLifecycleOwner(),
                scanning -> renderRescanSub(Boolean.TRUE.equals(scanning)));

        vm.getState().observe(getViewLifecycleOwner(), s -> {
            for (int i = 0; i < themes.length; i++) themes[i].setSelected(Theme.values()[i] == s.theme());
            for (int i = 0; i < langs.length; i++) langs[i].setSelected(tags[i].equals(s.language()));
            for (int i = 0; i < presets.length; i++) sleepChips[i].setSelected(presets[i] == s.sleepDefaultMinutes());
            if (b.artworkColors.isChecked() != s.artworkColors()) b.artworkColors.setChecked(s.artworkColors());
            if (b.onlineLyrics.isChecked() != s.onlineLyrics()) b.onlineLyrics.setChecked(s.onlineLyrics());
            if (b.batterySaver.isChecked() != s.batterySaver()) b.batterySaver.setChecked(s.batterySaver());
        });
    }

    /** Manual rescan from the row: reports the outcome underneath the row. */
    private void rescan() {
        if (!AudioPermission.granted(requireContext())) {
            Toast.makeText(requireContext(), R.string.nav_perm_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        runRescan();
    }

    /** Runs a scan (also after a folder change) and shows its outcome under the rescan row. */
    private void runRescan() {
        scanResult = null;
        renderRescanSub(true);
        Screens.container(this).libraryAdmin().rescan(r -> {
            if (!isAdded()) return; // left the screen meanwhile; the row resets when it is opened again
            scanResult = r.ok()
                    ? r.changed()
                            ? getString(R.string.settings_scan_done_f, r.added(), r.updated(), r.removed())
                            : getString(R.string.settings_scan_unchanged)
                    : getString(R.string.settings_scan_failed);
            renderRescanSub(false);
        });
    }

    private void renderRescanSub(boolean scanning) {
        if (b == null) return;
        b.rescanSub.setText(scanning ? getString(R.string.settings_scanning)
                : scanResult != null ? scanResult : getString(R.string.settings_rescan_sub));
    }

    /** Lists the scanned folders: tap one to remove it, or add a new one with the system folder picker. */
    private void showFoldersDialog() {
        Set<String> folders = currentFolders();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_folders)
                .setPositiveButton(R.string.settings_folder_add, (dialog, which) -> pickFolder())
                .setNegativeButton(android.R.string.cancel, null);
        if (folders.isEmpty()) {
            builder.setMessage(R.string.settings_folders_all);
        } else {
            String[] names = folders.toArray(new String[0]);
            builder.setMessage(R.string.settings_folder_remove_hint)
                    .setItems(names, (dialog, which) -> {
                        folders.remove(names[which]);
                        applyFolders(folders);
                    });
        }
        foldersDialog = builder.show();
    }

    private void pickFolder() {
        folderPicker.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }

    private Set<String> currentFolders() {
        Set<String> stored = Screens.container(this).settings().includedFolders().getValue();
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    /** Saves the include filter and rescans, so the library matches the new folder set right away. */
    private void applyFolders(Set<String> folders) {
        Screens.container(this).settings().setIncludedFolders(folders);
        if (AudioPermission.granted(requireContext())) runRescan();
    }

    private String versionName() {
        try {
            PackageInfo info = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    @Override public void onDestroyView() {
        if (foldersDialog != null) {
            foldersDialog.dismiss();
            foldersDialog = null;
        }
        b = null;
        super.onDestroyView();
    }
}

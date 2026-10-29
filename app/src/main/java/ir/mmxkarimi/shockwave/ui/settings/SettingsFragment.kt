package ir.mmxkarimi.shockwave.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.components.ExpressiveGroupCard
import ir.mmxkarimi.shockwave.ui.components.ExpressiveGroupItemRow
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ThemeMode
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by vm.getState().observeAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val libraryAdmin = remember { Screens.container(context).libraryAdmin() }
    val isScanning by libraryAdmin.isScanning().observeAsState(false)
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
    ) {
        // Expressive Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(42.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        state?.let { s ->
            val onSelectTheme: (SettingsStore.Theme) -> Unit = { target ->
                val before = s.theme
                vm.setTheme(target)
                if ((before == SettingsStore.Theme.BLACK) != (target == SettingsStore.Theme.BLACK)) {
                    (context as? android.app.Activity)?.recreate()
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Grouped Island 1: Appearance & Theme
            ExpressiveGroupCard(title = stringResource(R.string.settings_appearance)) {
                // 2x2 Visual Theme Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeVisualCard(
                            theme = SettingsStore.Theme.SYSTEM,
                            selected = s.theme == SettingsStore.Theme.SYSTEM,
                            label = stringResource(R.string.settings_theme_system),
                            icon = Icons.Default.BrightnessAuto,
                            previewBg = Color(0xFF252620),
                            previewAccent = Color(0xFFC6F135),
                            onClick = { onSelectTheme(SettingsStore.Theme.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeVisualCard(
                            theme = SettingsStore.Theme.LIGHT,
                            selected = s.theme == SettingsStore.Theme.LIGHT,
                            label = stringResource(R.string.settings_theme_light),
                            icon = Icons.Default.LightMode,
                            previewBg = Color(0xFFFDF9EE),
                            previewAccent = Color(0xFF516600),
                            onClick = { onSelectTheme(SettingsStore.Theme.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeVisualCard(
                            theme = SettingsStore.Theme.DARK,
                            selected = s.theme == SettingsStore.Theme.DARK,
                            label = stringResource(R.string.settings_theme_dark),
                            icon = Icons.Default.DarkMode,
                            previewBg = Color(0xFF13140F),
                            previewAccent = Color(0xFFC6F135),
                            onClick = { onSelectTheme(SettingsStore.Theme.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeVisualCard(
                            theme = SettingsStore.Theme.BLACK,
                            selected = s.theme == SettingsStore.Theme.BLACK,
                            label = stringResource(R.string.settings_theme_black),
                            icon = Icons.Default.Contrast,
                            previewBg = Color(0xFF000000),
                            previewAccent = Color(0xFFC6F135),
                            onClick = { onSelectTheme(SettingsStore.Theme.BLACK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ExpressiveGroupItemRow(
                    title = stringResource(R.string.settings_artwork_colors),
                    subtitle = stringResource(R.string.settings_artwork_colors_sub),
                    icon = Icons.Default.Palette,
                    trailingContent = {
                        Switch(
                            checked = s.artworkColors,
                            onCheckedChange = { vm.setArtworkColors(it) }
                        )
                    }
                )

                ExpressiveGroupItemRow(
                    title = stringResource(R.string.settings_battery_saver),
                    subtitle = stringResource(R.string.settings_battery_saver_sub),
                    icon = Icons.Default.BatteryChargingFull,
                    trailingContent = {
                        Switch(
                            checked = s.batterySaver,
                            onCheckedChange = { vm.setBatterySaver(it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grouped Island 2: Playback & Sleep Timer
            ExpressiveGroupCard(title = stringResource(R.string.settings_playback)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_sleep_default),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically stops playback after chosen duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val sleepPresets = listOf(
                        15 to "15m",
                        30 to "30m",
                        45 to "45m",
                        60 to "60m",
                        0 to stringResource(R.string.settings_sleep_end)
                    )

                    val presetScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(presetScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sleepPresets.forEach { (minutes, label) ->
                            val isSelected = s.sleepDefaultMinutes == minutes
                            Surface(
                                onClick = { vm.setSleepDefault(minutes) },
                                shape = RoundedCornerShape(50.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                ExpressiveGroupItemRow(
                    title = stringResource(R.string.settings_lyrics_online),
                    subtitle = stringResource(R.string.settings_lyrics_online_sub),
                    icon = Icons.Default.Lyrics,
                    trailingContent = {
                        Switch(
                            checked = s.onlineLyrics,
                            onCheckedChange = { vm.setOnlineLyrics(it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grouped Island 3: Library Maintenance
            ExpressiveGroupCard(title = stringResource(R.string.settings_library)) {
                ExpressiveGroupItemRow(
                    title = stringResource(R.string.settings_rescan),
                    subtitle = scanStatusMessage
                        ?: (if (isScanning) stringResource(R.string.settings_scanning) else stringResource(R.string.settings_rescan_sub)),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        if (!isScanning) {
                            scanStatusMessage = context.getString(R.string.settings_scanning)
                            libraryAdmin.rescan { result ->
                                scanStatusMessage = if (!result.ok) {
                                    context.getString(R.string.settings_scan_failed)
                                } else if (!result.changed()) {
                                    context.getString(R.string.settings_scan_unchanged)
                                } else {
                                    context.getString(
                                        R.string.settings_scan_done_f,
                                        result.added,
                                        result.updated,
                                        result.removed
                                    )
                                }
                            }
                        }
                    },
                    trailingContent = {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                )

                ExpressiveGroupItemRow(
                    title = stringResource(R.string.settings_folders),
                    subtitle = stringResource(R.string.settings_folders_all),
                    icon = Icons.Default.Folder
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grouped Island 4: About
            ExpressiveGroupCard(title = stringResource(R.string.settings_about)) {
                ExpressiveGroupItemRow(
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.settings_version, "0.5.0 Expressive"),
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

@Composable
fun ThemeVisualCard(
    theme: SettingsStore.Theme,
    selected: Boolean,
    label: String,
    icon: ImageVector,
    previewBg: Color,
    previewAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(previewBg)
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(previewAccent)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (previewBg == Color(0xFFFDF9EE)) Color(0xFF1C1C16) else Color(0xFFE4E3DA),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopStart)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val store = PrefsSettingsStore(requireContext())
        val vm = ViewModelProvider(this, SettingsViewModel.factory(store))[SettingsViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                val state by vm.getState().observeAsState()
                val currentMode = state?.theme?.let { ThemeApplier.toMode(it) } ?: ThemeMode.SYSTEM
                ShockwaveTheme(mode = currentMode) {
                    SettingsScreen(
                        vm = vm,
                        onBack = {
                            val nav = (activity as? ScreenNavigator)
                            if (nav != null) {
                                nav.openTab(ScreenNavigator.Tab.HOME)
                            } else {
                                parentFragmentManager.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}

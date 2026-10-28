package ir.mmxkarimi.shockwave.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun SettingsScreen(
    vm: SettingsViewModel
) {
    val state by vm.getState().observeAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        state?.let { s ->
            SectionHeader(title = stringResource(R.string.settings_theme_header))

            ThemeRadioOption(
                label = stringResource(R.string.settings_theme_system),
                selected = s.theme == SettingsStore.Theme.SYSTEM,
                onClick = { vm.setTheme(SettingsStore.Theme.SYSTEM) }
            )
            ThemeRadioOption(
                label = stringResource(R.string.settings_theme_light),
                selected = s.theme == SettingsStore.Theme.LIGHT,
                onClick = { vm.setTheme(SettingsStore.Theme.LIGHT) }
            )
            ThemeRadioOption(
                label = stringResource(R.string.settings_theme_dark),
                selected = s.theme == SettingsStore.Theme.DARK,
                onClick = { vm.setTheme(SettingsStore.Theme.DARK) }
            )
            ThemeRadioOption(
                label = stringResource(R.string.settings_theme_black),
                selected = s.theme == SettingsStore.Theme.BLACK,
                onClick = { vm.setTheme(SettingsStore.Theme.BLACK) }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader(title = stringResource(R.string.settings_appearance_header))

            SettingsSwitchRow(
                title = stringResource(R.string.settings_artwork_colors),
                checked = s.artworkColors,
                onCheckedChange = { vm.setArtworkColors(it) }
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_battery_saver),
                checked = s.batterySaver,
                onCheckedChange = { vm.setBatterySaver(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader(title = stringResource(R.string.settings_playback_header))

            SettingsSwitchRow(
                title = stringResource(R.string.settings_online_lyrics),
                checked = s.onlineLyrics,
                onCheckedChange = { vm.setOnlineLyrics(it) }
            )
        }
    }
}

@Composable
fun ThemeRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
                ShockwaveTheme {
                    SettingsScreen(vm = vm)
                }
            }
        }
    }
}

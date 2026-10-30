package ir.mmxkarimi.shockwave.ui.nav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

/** First run permission screen written with Jetpack Compose. */
@Composable
fun PermissionContent(
    onDone: () -> Unit,
    shouldShowRationale: () -> Boolean
) {
    val context = LocalContext.current
    var askedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDone()
        } else {
            askedOnce = true
        }
    }

    val blocked = askedOnce && !shouldShowRationale()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = stringResource(R.string.nav_perm_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.nav_perm_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (blocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.nav_perm_denied),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (blocked) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                } else {
                    launcher.launch(AudioPermission.name())
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = stringResource(
                    if (blocked) R.string.nav_perm_open_settings else R.string.nav_perm_allow
                )
            )
        }
    }
}
}

class PermissionFragment : Fragment() {

    interface Host {
        fun onPermissionFlowDone()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val theme = remember { PrefsSettingsStore(requireContext()).getTheme() }
                val currentMode = ThemeApplier.toMode(theme)

                ShockwaveTheme(mode = currentMode) {
                    PermissionContent(
                        onDone = { done() },
                        shouldShowRationale = {
                            shouldShowRequestPermissionRationale(AudioPermission.name())
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AudioPermission.granted(requireContext())) {
            done()
        }
    }

    private fun done() {
        (activity as? Host)?.onPermissionFlowDone()
    }
}

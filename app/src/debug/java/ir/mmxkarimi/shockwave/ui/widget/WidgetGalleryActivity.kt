package ir.mmxkarimi.shockwave.ui.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mmxkarimi.shockwave.fake.SampleData
import ir.mmxkarimi.shockwave.ui.components.MiniPlayerBar
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

/** Debug-only gallery with fake data written in Jetpack Compose. */
class WidgetGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sampleTrack = SampleData.TRACKS.firstOrNull()

        setContent {
            ShockwaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Compose Component Gallery",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        SectionHeader(title = "Sample Section Header", actionLabel = "Action", onActionClick = {})

                        if (sampleTrack != null) {
                            Text(text = "Track Row Component:", style = MaterialTheme.typography.titleMedium)
                            TrackRow(
                                track = sampleTrack,
                                onClick = {}
                            )

                            Text(text = "Mini Player Bar Component:", style = MaterialTheme.typography.titleMedium)
                            MiniPlayerBar(
                                track = sampleTrack,
                                isPlaying = true,
                                progressFraction = 0.45f,
                                onClick = {},
                                onPlayPause = {},
                                onNext = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

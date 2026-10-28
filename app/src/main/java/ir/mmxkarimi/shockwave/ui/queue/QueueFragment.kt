package ir.mmxkarimi.shockwave.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun QueueSheetContent(
    vm: QueueViewModel,
    onDismiss: () -> Unit
) {
    val ui by vm.getUi().observeAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.player_queue),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (!ui?.playingFrom.isNullOrBlank()) {
                    Text(
                        text = "From ${ui?.playingFrom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ui?.canUndoShuffle == true) {
                IconButton(onClick = { vm.undoShuffle() }) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo Shuffle")
                }
            }

            IconButton(onClick = { vm.clearQueue() }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Queue")
            }

            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sleep timer row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            QueueViewModel.SLEEP_PRESETS_MIN.forEach { minutes ->
                val selected = ui?.sleepKind == SleepTimer.Kind.COUNTDOWN && ui?.sleepMinutes == minutes
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) vm.cancelSleepTimer() else vm.setSleepCountdown(minutes)
                    },
                    label = { Text("${minutes}m") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current track
        ui?.current?.let { currentTrack ->
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
            TrackRow(
                track = currentTrack,
                isPlaying = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Up next header
        Text(
            text = "Up Next (${ui?.upNext?.size ?: 0})",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Up next items
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            val list = ui?.upNext ?: emptyList()
            itemsIndexed(list, key = { _, t -> t.id }) { index, track ->
                TrackRow(
                    track = track,
                    onClick = {
                        val qIndex = vm.toQueueIndex(index)
                        vm.playAt(qIndex)
                    },
                    onMoreClick = {
                        val qIndex = vm.toQueueIndex(index)
                        vm.remove(qIndex)
                    }
                )
            }
        }
    }
}

class QueueFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val player = Screens.container(this).player
        val vm = ViewModelProvider(this, QueueViewModel.factory(player))[QueueViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                ShockwaveTheme {
                    Surface {
                        QueueSheetContent(
                            vm = vm,
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "queue"

        @JvmStatic
        fun newInstance(): QueueFragment = QueueFragment()
    }
}

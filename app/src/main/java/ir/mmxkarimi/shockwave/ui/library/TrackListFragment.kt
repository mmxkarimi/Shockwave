package ir.mmxkarimi.shockwave.ui.library

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.TrackDetailsDialog
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.components.shareTrack
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.playlist.AddToPlaylistSheet
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

private const val ARG_KIND = "kind"
private const val ARG_ID = "id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    vm: TrackListViewModel,
    kind: ScreenNavigator.TrackList,
    onBack: () -> Unit
) {
    val tracks by vm.getTracks().observeAsState(emptyList())
    val header by vm.getHeader().observeAsState()
    var trackActionTarget by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    val context = LocalContext.current
    val libraryAdmin = remember { Screens.container(context).libraryAdmin() }
    val player = remember { Screens.container(context).player }

    val screenTitle = when (kind) {
        ScreenNavigator.TrackList.LIKED -> stringResource(R.string.library_liked)
        ScreenNavigator.TrackList.HISTORY -> stringResource(R.string.library_history)
        ScreenNavigator.TrackList.RECENT -> stringResource(R.string.library_recently_saved)
        else -> header?.title ?: ""
    }

    if (trackToDelete != null) {
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text(stringResource(R.string.library_delete_track_title)) },
            text = { Text(stringResource(R.string.library_delete_track_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val t = trackToDelete
                    trackToDelete = null
                    if (t != null) {
                        libraryAdmin.deleteTrack(t.id)
                    }
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (trackForDetails != null) {
        TrackDetailsDialog(track = trackForDetails!!, onDismiss = { trackForDetails = null })
    }

    if (trackToAddToPlaylist != null) {
        AddToPlaylistSheet(
            track = trackToAddToPlaylist!!,
            onDismiss = { trackToAddToPlaylist = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header item (Artwork + Title + Play/Shuffle controls)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (header != null) {
                        AsyncArtworkImage(
                            artworkUri = null,
                            seed = header?.seed ?: 0,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(if (header?.circle == true) CircleShape else RoundedCornerShape(16.dp)),
                            circle = header?.circle == true,
                            cornerDp = 16.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (!header?.subtitle.isNullOrBlank()) {
                        Text(
                            text = header?.subtitle ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "${tracks.size} tracks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    vm.play(tracks, 0, screenTitle)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        FilledTonalButton(
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    vm.shuffle(tracks, screenTitle)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Shuffle")
                        }
                    }
                }
            }

            // Track list items
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                Box {
                    TrackRow(
                        track = track,
                        onClick = {
                            vm.play(tracks, index, screenTitle)
                        },
                        onMoreClick = {
                            trackActionTarget = track
                        }
                    )

                    if (trackActionTarget == track) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { trackActionTarget = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_menu_play_next)) },
                                leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                                onClick = {
                                    trackActionTarget = null
                                    player.playNext(track)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_menu_add_queue)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                onClick = {
                                    trackActionTarget = null
                                    player.addToQueue(track)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_menu_add_playlist)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                onClick = {
                                    trackActionTarget = null
                                    trackToAddToPlaylist = track
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.track_menu_share)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    trackActionTarget = null
                                    shareTrack(context, track)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.track_details_title)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    trackActionTarget = null
                                    trackForDetails = track
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_menu_delete_track), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    trackActionTarget = null
                                    trackToDelete = track
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class TrackListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val kindName = arguments?.getString(ARG_KIND) ?: ScreenNavigator.TrackList.LIKED.name
        val kind = ScreenNavigator.TrackList.valueOf(kindName)
        val id = arguments?.getString(ARG_ID)

        val c = Screens.container(this)
        val vm = ViewModelProvider(
            this,
            TrackListViewModel.factory(c.library, c.player, kind, id)
        )[TrackListViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                ShockwaveTheme {
                    TrackListScreen(
                        vm = vm,
                        kind = kind,
                        onBack = { parentFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(kind: ScreenNavigator.TrackList, id: String?): TrackListFragment {
            return TrackListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_KIND, kind.name)
                    putString(ARG_ID, id)
                }
            }
        }
    }
}

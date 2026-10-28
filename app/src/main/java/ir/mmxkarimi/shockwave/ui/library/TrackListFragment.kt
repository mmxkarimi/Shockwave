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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
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

    val screenTitle = when (kind) {
        ScreenNavigator.TrackList.LIKED -> "Liked Songs"
        ScreenNavigator.TrackList.HISTORY -> "History"
        ScreenNavigator.TrackList.RECENT -> "Recently Added"
        else -> header?.title ?: ""
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
                TrackRow(
                    track = track,
                    onClick = {
                        vm.play(tracks, index, screenTitle)
                    }
                )
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

package ir.mmxkarimi.shockwave.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

private const val ARG_KIND = "kind"

data class CollectionItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val seed: Int,
    val circle: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    kind: ScreenNavigator.Collection,
    items: List<CollectionItem>,
    onItemClick: (CollectionItem) -> Unit,
    onBack: () -> Unit
) {
    val title = when (kind) {
        ScreenNavigator.Collection.ALBUMS -> stringResource(R.string.library_albums)
        ScreenNavigator.Collection.ARTISTS -> stringResource(R.string.library_artists)
        ScreenNavigator.Collection.PLAYLISTS -> stringResource(R.string.library_playlists)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onItemClick(item) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncArtworkImage(
                            artworkUri = item.artworkUri,
                            seed = item.seed,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(if (item.circle) CircleShape else RoundedCornerShape(12.dp)),
                            circle = item.circle,
                            cornerDp = 12.dp,
                            monogram = if (item.circle) item.title else null
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

class CollectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val kindName = arguments?.getString(ARG_KIND) ?: ScreenNavigator.Collection.ALBUMS.name
        val kind = ScreenNavigator.Collection.valueOf(kindName)

        val c = Screens.container(this)
        val vm = ViewModelProvider(this, LibraryViewModel.factory(c.library, c.player))[LibraryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                val albums by vm.getAlbums().observeAsState(emptyList())
                val artists by vm.getArtists().observeAsState(emptyList())
                val playlists by vm.getPlaylists().observeAsState(emptyList())

                val items = when (kind) {
                    ScreenNavigator.Collection.ALBUMS -> albums.map {
                        CollectionItem(it.id, it.title, it.artistName, it.artworkUri, it.placeholderSeed, false)
                    }
                    ScreenNavigator.Collection.ARTISTS -> artists.map {
                        CollectionItem(it.id, it.name, "${it.trackCount} tracks", null, it.placeholderSeed, true)
                    }
                    ScreenNavigator.Collection.PLAYLISTS -> playlists.map {
                        CollectionItem(it.id, it.name, "${it.trackCount} tracks", null, it.placeholderSeed, false)
                    }
                }

                ShockwaveTheme {
                    CollectionScreen(
                        kind = kind,
                        items = items,
                        onItemClick = { item ->
                            val trackListKind = when (kind) {
                                ScreenNavigator.Collection.ALBUMS -> ScreenNavigator.TrackList.ALBUM
                                ScreenNavigator.Collection.ARTISTS -> ScreenNavigator.TrackList.ARTIST
                                ScreenNavigator.Collection.PLAYLISTS -> ScreenNavigator.TrackList.PLAYLIST
                            }
                            Screens.openTrackList(this@CollectionFragment, trackListKind, item.id)
                        },
                        onBack = { parentFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(kind: ScreenNavigator.Collection): CollectionFragment {
            return CollectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_KIND, kind.name)
                }
            }
        }
    }
}

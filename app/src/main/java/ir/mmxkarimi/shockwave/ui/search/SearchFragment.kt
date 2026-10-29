package ir.mmxkarimi.shockwave.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
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
import ir.mmxkarimi.shockwave.domain.model.SearchFilter
import ir.mmxkarimi.shockwave.domain.model.SearchResult
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.library.AsyncArtworkImage
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun SearchScreen(
    vm: SearchViewModel,
    onNavigateTrackList: (ScreenNavigator.TrackList, String) -> Unit
) {
    val query by vm.getQuery().observeAsState("")
    val filter by vm.getFilter().observeAsState(SearchFilter.ALL)
    val results by vm.getResults().observeAsState(emptyList())
    val mode by vm.getMode().observeAsState(SearchViewModel.Mode.PROMPT)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(text = stringResource(R.string.search_hint)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        // Filter chips (All, Tracks, Albums, Artists, Playlists)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                SearchFilter.ALL to R.string.search_filter_all,
                SearchFilter.SONGS to R.string.search_filter_songs,
                SearchFilter.ALBUMS to R.string.search_filter_albums,
                SearchFilter.ARTISTS to R.string.search_filter_artists,
                SearchFilter.PLAYLISTS to R.string.search_filter_playlists
            )
            items(filters) { (f, labelRes) ->
                FilterChip(
                    selected = filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text(text = stringResource(labelRes)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Body area
        when (mode) {
            SearchViewModel.Mode.PROMPT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            SearchViewModel.Mode.NO_RESULTS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            SearchViewModel.Mode.RESULTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(results, key = { "${it.kind}_${it.track?.id ?: it.album?.id ?: it.artist?.id ?: it.playlist?.id ?: it.hashCode()}" }) { item ->
                        when (item.kind) {
                            SearchResult.Kind.TRACK -> {
                                item.track?.let { track ->
                                    TrackRow(
                                        track = track,
                                        onClick = { vm.playTrack(track) }
                                    )
                                }
                            }
                            SearchResult.Kind.ALBUM -> {
                                item.album?.let { album ->
                                    SearchResultRow(
                                        title = album.title,
                                        subtitle = album.artistName,
                                        artworkUri = album.artworkUri,
                                        seed = album.placeholderSeed,
                                        circle = false,
                                        onClick = { onNavigateTrackList(ScreenNavigator.TrackList.ALBUM, album.id) }
                                    )
                                }
                            }
                            SearchResult.Kind.ARTIST -> {
                                item.artist?.let { artist ->
                                    SearchResultRow(
                                        title = artist.name,
                                        subtitle = "${artist.trackCount} tracks",
                                        artworkUri = null,
                                        seed = artist.placeholderSeed,
                                        circle = true,
                                        onClick = { onNavigateTrackList(ScreenNavigator.TrackList.ARTIST, artist.id) }
                                    )
                                }
                            }
                            SearchResult.Kind.PLAYLIST -> {
                                item.playlist?.let { pl ->
                                    SearchResultRow(
                                        title = pl.name,
                                        subtitle = "${pl.trackCount} tracks",
                                        artworkUri = null,
                                        seed = pl.placeholderSeed,
                                        circle = false,
                                        onClick = { onNavigateTrackList(ScreenNavigator.TrackList.PLAYLIST, pl.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun SearchResultRow(
    title: String,
    subtitle: String?,
    artworkUri: String?,
    seed: Int,
    circle: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncArtworkImage(
            artworkUri = artworkUri,
            seed = seed,
            modifier = Modifier
                .size(48.dp)
                .clip(if (circle) CircleShape else RoundedCornerShape(10.dp)),
            circle = circle,
            cornerDp = 10.dp,
            monogram = if (circle) title else null
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val c = Screens.container(this)
        val vm = ViewModelProvider(this, SearchViewModel.factory(c.library, c.player))[SearchViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                val theme = remember { PrefsSettingsStore(requireContext()).getTheme() }
                val currentMode = ThemeApplier.toMode(theme)

                                ShockwaveTheme(mode = currentMode) {
                    SearchScreen(
                        vm = vm,
                        onNavigateTrackList = { kind, id ->
                            Screens.openTrackList(this@SearchFragment, kind, id)
                        }
                    )
                }
            }
        }
    }
}

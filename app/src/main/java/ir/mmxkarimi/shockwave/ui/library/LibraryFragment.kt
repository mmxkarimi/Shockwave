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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit,
    onNavigateCollection: (ScreenNavigator.Collection) -> Unit
) {
    val liked by vm.getLiked().observeAsState(emptyList())
    val albums by vm.getAlbums().observeAsState(emptyList())
    val artists by vm.getArtists().observeAsState(emptyList())
    val playlists by vm.getPlaylists().observeAsState(emptyList())
    val recent by vm.getRecentlySaved().observeAsState(emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top section: Liked Songs Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateTrackList(ScreenNavigator.TrackList.LIKED, null) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.library_liked),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${liked.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Navigation tiles: Albums, Artists, Playlists, History
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CollectionTile(
                    title = stringResource(R.string.library_albums),
                    icon = Icons.Default.Album,
                    count = albums.size,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateCollection(ScreenNavigator.Collection.ALBUMS) }
                )
                CollectionTile(
                    title = stringResource(R.string.library_artists),
                    icon = Icons.Default.Person,
                    count = artists.size,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateCollection(ScreenNavigator.Collection.ARTISTS) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CollectionTile(
                    title = stringResource(R.string.library_playlists),
                    icon = Icons.Default.PlaylistPlay,
                    count = playlists.size,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateCollection(ScreenNavigator.Collection.PLAYLISTS) }
                )
                CollectionTile(
                    title = stringResource(R.string.library_history),
                    icon = Icons.Default.History,
                    count = 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTrackList(ScreenNavigator.TrackList.HISTORY, null) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recently Saved Tracks
        if (recent.isNotEmpty()) {
            item {
                SectionHeader(
                    title = stringResource(R.string.library_recently_saved),
                    actionLabel = "See all",
                    onActionClick = { onNavigateTrackList(ScreenNavigator.TrackList.RECENT, null) }
                )
            }

            items(recent.take(10), key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onClick = {
                        vm.play(recent, recent.indexOf(track), "Recently Saved")
                    }
                )
            }
        }
    }
}

@Composable
fun CollectionTile(
    title: String,
    icon: ImageVector,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (count > 0) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

class LibraryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val c = Screens.container(this)
        val vm = ViewModelProvider(this, LibraryViewModel.factory(c.library, c.player))[LibraryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                ShockwaveTheme {
                    LibraryScreen(
                        vm = vm,
                        onNavigateTrackList = { kind, id ->
                            Screens.openTrackList(this@LibraryFragment, kind, id)
                        },
                        onNavigateCollection = { kind ->
                            Screens.openCollection(this@LibraryFragment, kind)
                        }
                    )
                }
            }
        }
    }
}

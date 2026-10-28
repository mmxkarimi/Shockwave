package ir.mmxkarimi.shockwave.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.HomeItem
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.ExpressiveSubheaderRow
import ir.mmxkarimi.shockwave.ui.components.ExpressiveTopAppBar
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.library.AsyncArtworkImage
import ir.mmxkarimi.shockwave.ui.library.LibraryViewModel
import ir.mmxkarimi.shockwave.ui.nav.AudioPermission
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.playlist.AddToPlaylistSheet
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun UnifiedHomeScreen(
    homeVm: HomeViewModel,
    libVm: LibraryViewModel,
    isScanning: Boolean,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 6 }
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(
        "For You",
        "Tracks",
        "Favorites",
        "Playlists",
        "Albums",
        "Artists"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Expressive Top App Bar with Title, Search and Settings buttons
        ExpressiveTopAppBar(
            title = "Shockwave",
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick
        )

        // Expressive Scrollable Tab Row
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Pager with Swipable Pages housed within 28dp elevated sheets
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ForYouPage(
                    homeVm = homeVm,
                    libVm = libVm,
                    isScanning = isScanning,
                    onNavigateTrackList = onNavigateTrackList,
                    onAddToPlaylist = onAddToPlaylist
                )
                1 -> TracksPage(
                    libVm = libVm,
                    onAddToPlaylist = onAddToPlaylist
                )
                2 -> FavoritesPage(
                    libVm = libVm,
                    onAddToPlaylist = onAddToPlaylist
                )
                3 -> PlaylistsPage(
                    libVm = libVm,
                    onNavigateTrackList = onNavigateTrackList
                )
                4 -> AlbumsPage(
                    libVm = libVm,
                    onNavigateTrackList = onNavigateTrackList
                )
                5 -> ArtistsPage(
                    libVm = libVm,
                    onNavigateTrackList = onNavigateTrackList
                )
            }
        }
    }
}

@Composable
fun ForYouPage(
    homeVm: HomeViewModel,
    libVm: LibraryViewModel,
    isScanning: Boolean,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val sections by homeVm.getSections().observeAsState()
    val recentTracks by libVm.getRecentlySaved().observeAsState(emptyList())
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Screens.container(context).onAudioPermissionGranted()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        if (sections == null || (sections?.isEmpty() == true && isScanning)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.home_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (sections?.isEmpty() == true) {
            val granted = AudioPermission.granted(context)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (granted) {
                                Screens.container(context).libraryAdmin().rescan(null)
                            } else {
                                permissionLauncher.launch(AudioPermission.name())
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(
                                if (granted) R.string.home_empty_rescan else R.string.nav_perm_allow
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                // Dynamic Greeting Banner
                item {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when (hour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        in 17..21 -> "Good evening"
                        else -> "Good night"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$greeting, Shockwave listener",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your music, beautifully crafted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hero Carousel for fresh releases with asymmetric 28dp corners
                if (recentTracks.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Fresh Releases",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(recentTracks.take(6), key = { "hero_${it.id}" }) { track ->
                                    Surface(
                                        onClick = {
                                            libVm.play(recentTracks, recentTracks.indexOf(track), "Fresh Releases")
                                        },
                                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 28.dp, bottomStart = 8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .width(260.dp)
                                            .height(150.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncArtworkImage(
                                                artworkUri = track.artworkUri,
                                                seed = track.placeholderSeed,
                                                modifier = Modifier.fillMaxSize(),
                                                cornerDp = 0.dp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.85f)
                                                            ),
                                                            startY = 60f
                                                        )
                                                    )
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artistName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Feed Sections
                val nonNullSections = sections ?: emptyList()
                items(nonNullSections, key = { it.id }) { section ->
                    HomeSectionRow(
                        section = section,
                        onItemClick = { item ->
                            when (item.kind) {
                                HomeItem.Kind.TRACK -> {
                                    item.track?.let { track ->
                                        homeVm.playFromSection(section, track)
                                    }
                                }
                                HomeItem.Kind.ALBUM -> {
                                    item.album?.let { album ->
                                        onNavigateTrackList(ScreenNavigator.TrackList.ALBUM, album.id)
                                    }
                                }
                                HomeItem.Kind.PLAYLIST -> {
                                    item.playlist?.let { pl ->
                                        onNavigateTrackList(ScreenNavigator.TrackList.PLAYLIST, pl.id)
                                    }
                                }
                            }
                        },
                        onSeeAll = {
                            onNavigateTrackList(ScreenNavigator.TrackList.RECENT, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TracksPage(
    libVm: LibraryViewModel,
    onAddToPlaylist: (Track) -> Unit
) {
    val tracks by libVm.getAllTracks().observeAsState(emptyList())
    var sortMode by remember { mutableIntStateOf(0) } // 0: Title, 1: Artist, 2: Duration

    val sortedTracks = remember(tracks, sortMode) {
        when (sortMode) {
            0 -> tracks.sortedBy { it.title.lowercase() }
            1 -> tracks.sortedBy { it.artistName.lowercase() }
            2 -> tracks.sortedByDescending { it.durationMs }
            else -> tracks
        }
    }

    val sortLabel = when (sortMode) {
        0 -> "Title"
        1 -> "Artist"
        2 -> "Duration"
        else -> "Title"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {
            item {
                ExpressiveSubheaderRow(
                    itemCount = sortedTracks.size,
                    sortLabel = sortLabel,
                    onSortClick = {
                        sortMode = (sortMode + 1) % 3
                    },
                    onShuffleClick = {
                        libVm.shuffle(sortedTracks, "All Tracks")
                    },
                    onPlayAllClick = {
                        if (sortedTracks.isNotEmpty()) {
                            libVm.play(sortedTracks, 0, "All Tracks")
                        }
                    }
                )
            }

            items(sortedTracks, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    onClick = {
                        libVm.play(sortedTracks, sortedTracks.indexOf(track), "All Tracks")
                    },
                    onMoreClick = {
                        onAddToPlaylist(track)
                    }
                )
            }
        }
    }
}

@Composable
fun FavoritesPage(
    libVm: LibraryViewModel,
    onAddToPlaylist: (Track) -> Unit
) {
    val liked by libVm.getLiked().observeAsState(emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        if (liked.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.library_liked_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
            ) {
                item {
                    ExpressiveSubheaderRow(
                        itemCount = liked.size,
                        sortLabel = "Favorites",
                        onSortClick = {},
                        onShuffleClick = {
                            libVm.shuffle(liked, "Favorites")
                        },
                        onPlayAllClick = {
                            libVm.playLiked("Favorites")
                        }
                    )
                }

                items(liked, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onClick = {
                            libVm.play(liked, liked.indexOf(track), "Favorites")
                        },
                        onMoreClick = {
                            onAddToPlaylist(track)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistsPage(
    libVm: LibraryViewModel,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit
) {
    val playlists by libVm.getPlaylists().observeAsState(emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val library = remember { Screens.container(context).library }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = stringResource(R.string.library_new_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotEmpty()) {
                            library.createPlaylist(name, null)
                            Toast.makeText(context, "Playlist created: $name", Toast.LENGTH_SHORT).show()
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${playlists.size} Playlists",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "New Playlist",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                Surface(
                    onClick = {
                        onNavigateTrackList(ScreenNavigator.TrackList.PLAYLIST, playlist.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${playlist.trackCount} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsPage(
    libVm: LibraryViewModel,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit
) {
    val albums by libVm.getAlbums().observeAsState(emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                Surface(
                    onClick = {
                        onNavigateTrackList(ScreenNavigator.TrackList.ALBUM, album.id)
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncArtworkImage(
                            artworkUri = album.artworkUri,
                            seed = album.placeholderSeed,
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            cornerDp = 14.dp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        if (!album.artistName.isNullOrBlank()) {
                            Text(
                                text = album.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
fun ArtistsPage(
    libVm: LibraryViewModel,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit
) {
    val artists by libVm.getArtists().observeAsState(emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { it.id }) { artist ->
                Surface(
                    onClick = {
                        onNavigateTrackList(ScreenNavigator.TrackList.ARTIST, artist.id)
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncArtworkImage(
                            artworkUri = null,
                            seed = artist.placeholderSeed,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            circle = true,
                            cornerDp = 0.dp,
                            monogram = artist.name
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "${artist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
fun HomeSectionRow(
    section: HomeSection,
    onItemClick: (HomeItem) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = section.title,
            actionLabel = if (section.items.size > 5) "See all" else null,
            onActionClick = onSeeAll
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.items, key = { "${it.kind}_${it.track?.id ?: it.album?.id ?: it.playlist?.id ?: it.hashCode()}" }) { item ->
                HomeCardItem(item = item, onClick = { onItemClick(item) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HomeCardItem(
    item: HomeItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            val (artUri, seed, title, subtitle) = when (item.kind) {
                HomeItem.Kind.TRACK -> listOf(
                    item.track?.artworkUri,
                    item.track?.placeholderSeed ?: 0,
                    item.track?.title ?: "",
                    item.track?.artistName ?: ""
                )
                HomeItem.Kind.ALBUM -> listOf(
                    item.album?.artworkUri,
                    item.album?.placeholderSeed ?: 0,
                    item.album?.title ?: "",
                    item.album?.artistName ?: ""
                )
                HomeItem.Kind.PLAYLIST -> listOf(
                    null,
                    item.playlist?.placeholderSeed ?: 0,
                    item.playlist?.name ?: "",
                    "${item.playlist?.trackCount ?: 0} tracks"
                )
            }

            AsyncArtworkImage(
                artworkUri = artUri as? String,
                seed = (seed as? Int) ?: 0,
                modifier = Modifier
                    .size(124.dp)
                    .clip(RoundedCornerShape(14.dp)),
                cornerDp = 14.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title as? String ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle as? String ?: "",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val c = Screens.container(this)
        val homeVm = ViewModelProvider(this, HomeViewModel.factory(c.homeFeed, c.player))[HomeViewModel::class.java]
        val libVm = ViewModelProvider(this, LibraryViewModel.factory(c.library, c.player))[LibraryViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                val isScanning by c.libraryAdmin().isScanning().observeAsState(false)
                ShockwaveTheme {
                    UnifiedHomeScreen(
                        homeVm = homeVm,
                        libVm = libVm,
                        isScanning = isScanning,
                        onSearchClick = {
                            (activity as? ScreenNavigator)?.openTab(ScreenNavigator.Tab.SEARCH)
                        },
                        onSettingsClick = {
                            (activity as? ScreenNavigator)?.openTab(ScreenNavigator.Tab.SETTINGS)
                        },
                        onNavigateTrackList = { kind, id ->
                            Screens.openTrackList(this@HomeFragment, kind, id)
                        },
                        onAddToPlaylist = { track ->
                            AddToPlaylistSheet.show(parentFragmentManager, track)
                        }
                    )
                }
            }
        }
    }
}

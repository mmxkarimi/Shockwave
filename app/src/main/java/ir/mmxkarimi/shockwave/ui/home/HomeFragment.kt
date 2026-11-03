package ir.mmxkarimi.shockwave.ui.home

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import java.text.NumberFormat
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.TrackDeleteHelper
import ir.mmxkarimi.shockwave.ui.components.TrackDetailsDialog
import ir.mmxkarimi.shockwave.ui.library.AsyncArtworkImage
import ir.mmxkarimi.shockwave.ui.library.LibraryViewModel
import ir.mmxkarimi.shockwave.ui.components.shareTrack
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.playlist.AddToPlaylistSheet
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme
import kotlinx.coroutines.launch

@Composable
fun UnifiedHomeScreen(
    homeVm: HomeViewModel,
    libVm: LibraryViewModel,
    isScanning: Boolean,
    initialPage: Int = 0,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 5 }
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.home_tab_tracks),
        stringResource(R.string.home_tab_favorites),
        stringResource(R.string.home_tab_playlists),
        stringResource(R.string.home_tab_albums),
        stringResource(R.string.home_tab_artists)
    )

    val tracksListState = rememberLazyListState()
    val favListState = rememberLazyListState()
    val playlistListState = rememberLazyListState()

    // Title collapse is driven by nested scroll, so every page (lists AND grids) controls it.
    // Scroll down -> title gets smaller. Scroll back to the top -> title gets bigger.
    val maxCollapsePx = with(LocalDensity.current) { 100.dp.toPx() }
    var collapsePx by remember { mutableFloatStateOf(0f) }
    val collapseConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Finger moves up (content scrolls down): shrink the title first.
                if (available.y < 0f && collapsePx < maxCollapsePx) {
                    collapsePx = (collapsePx - available.y).coerceIn(0f, maxCollapsePx)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Leftover downward scroll means the list is at the top: grow the title back.
                if (available.y > 0f && collapsePx > 0f) {
                    collapsePx = (collapsePx - available.y).coerceIn(0f, maxCollapsePx)
                }
                return Offset.Zero
            }
        }
    }
    val collapseFraction = collapsePx / maxCollapsePx

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .nestedScroll(collapseConnection)
    ) {
        // Collapsible Large Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 12.dp,
                    top = (8 + (6 * (1f - collapseFraction))).dp,
                    bottom = (4 + (6 * (1f - collapseFraction))).dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Shockwave",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = (22 + (10 * (1f - collapseFraction))).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

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

        // Horizontal Pager with Swipable Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> TracksPage(
                    libVm = libVm,
                    listState = tracksListState,
                    onAddToPlaylist = onAddToPlaylist
                )
                1 -> FavoritesPage(
                    libVm = libVm,
                    listState = favListState,
                    onAddToPlaylist = onAddToPlaylist
                )
                2 -> PlaylistsPage(
                    libVm = libVm,
                    listState = playlistListState,
                    onNavigateTrackList = onNavigateTrackList
                )
                3 -> AlbumsPage(
                    libVm = libVm,
                    onNavigateTrackList = onNavigateTrackList
                )
                4 -> ArtistsPage(
                    libVm = libVm,
                    onNavigateTrackList = onNavigateTrackList
                )
            }
        }
    }
}

@Composable
fun TracksPage(
    libVm: LibraryViewModel,
    listState: LazyListState,
    onAddToPlaylist: (Track) -> Unit
) {
    val tracks by libVm.getAllTracks().observeAsState(emptyList())
    val context = LocalContext.current
    val settingsStore = remember { PrefsSettingsStore(context) }
    var sortMode by remember { mutableIntStateOf(settingsStore.getTracksSortMode()) } // 0: Title, 1: Artist, 2: Duration, 3: Date added
    var sortMenuOpen by remember { mutableStateOf(false) }
    var trackActionTarget by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var pendingStorageTrack by remember { mutableStateOf<Track?>(null) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }
    val libraryAdmin = remember { Screens.container(context).libraryAdmin() }
    val player = remember { Screens.container(context).player }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val t = pendingStorageTrack
            if (t != null) {
                libraryAdmin.deleteTrack(t.id)
                pendingStorageTrack = null
            }
        }
    }

    // Id only: position ticks otherwise recompose the whole list every second.
    val currentTrackId by remember(player) {
        player.getState().map { it.currentTrack?.id }.distinctUntilChanged()
    }.observeAsState()

    val sortedTracks = remember(tracks, sortMode) {
        when (sortMode) {
            0 -> tracks.sortedBy { it.title.lowercase() }
            1 -> tracks.sortedBy { it.artistName.lowercase() }
            2 -> tracks.sortedByDescending { it.durationMs }
            3 -> tracks.sortedByDescending { it.addedAtMs }
            else -> tracks
        }
    }

    val titleSortLabel = stringResource(R.string.sort_title)
    val artistSortLabel = stringResource(R.string.sort_artist)
    val durationSortLabel = stringResource(R.string.sort_duration)
    val dateAddedSortLabel = stringResource(R.string.sort_date_added)
    val allTracksLabel = stringResource(R.string.all_tracks)

    val sortLabel = when (sortMode) {
        0 -> titleSortLabel
        1 -> artistSortLabel
        2 -> durationSortLabel
        3 -> dateAddedSortLabel
        else -> titleSortLabel
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
                        pendingStorageTrack = t
                        TrackDeleteHelper.deleteTrack(context, t, deleteLauncher) {
                            libraryAdmin.deleteTrack(t.id)
                            pendingStorageTrack = null
                        }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
            ) {
                item {
                    Box {
                        TracksHeader(
                            itemCount = sortedTracks.size,
                            sortLabel = sortLabel,
                            onSortClick = { sortMenuOpen = true },
                            onShuffleClick = {
                                libVm.shuffle(sortedTracks, allTracksLabel)
                            },
                            onPlayAllClick = {
                                if (sortedTracks.isNotEmpty()) {
                                    libVm.play(sortedTracks, 0, allTracksLabel)
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            val sortOptions = listOf(
                                0 to titleSortLabel,
                                1 to artistSortLabel,
                                2 to durationSortLabel,
                                3 to dateAddedSortLabel
                            )
                            sortOptions.forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        sortMode = mode
                                        settingsStore.setTracksSortMode(mode)
                                        sortMenuOpen = false
                                    },
                                    leadingIcon = if (sortMode == mode) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                itemsIndexed(sortedTracks, key = { _, t -> t.id }) { index, track ->
                    Box {
                        BorderlessTrackRow(
                            track = track,
                            isFirst = index == 0,
                            isLast = index == sortedTracks.lastIndex,
                            isPlaying = track.id == currentTrackId,
                            onClick = {
                                libVm.play(sortedTracks, sortedTracks.indexOf(track), "All Tracks")
                            },
                            onMoreClick = {
                                trackActionTarget = track
                            },
 menuContent = {
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
                                        onAddToPlaylist(track)
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
                        )
                    }
                }
            }

            // Theme-adaptive bottom scroll fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun FavoritesPage(
    libVm: LibraryViewModel,
    listState: LazyListState,
    onAddToPlaylist: (Track) -> Unit
) {
    val liked by libVm.getLiked().observeAsState(emptyList())
    var trackActionTarget by remember { mutableStateOf<Track?>(null) }
    var trackToDelete by remember { mutableStateOf<Track?>(null) }
    var pendingStorageTrack by remember { mutableStateOf<Track?>(null) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }
    val context = LocalContext.current
    val libraryAdmin = remember { Screens.container(context).libraryAdmin() }
    val player = remember { Screens.container(context).player }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val t = pendingStorageTrack
            if (t != null) {
                libraryAdmin.deleteTrack(t.id)
                pendingStorageTrack = null
            }
        }
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
                        pendingStorageTrack = t
                        TrackDeleteHelper.deleteTrack(context, t, deleteLauncher) {
                            libraryAdmin.deleteTrack(t.id)
                            pendingStorageTrack = null
                        }
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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
                ) {
                    item {
                        TracksHeader(
                            itemCount = liked.size,
                            sortLabel = null,
                            onSortClick = null,
                            onShuffleClick = {
                                libVm.shuffle(liked, "Favorites")
                            },
                            onPlayAllClick = {
                                libVm.playLiked("Favorites")
                            }
                        )
                    }

                    itemsIndexed(liked, key = { _, t -> t.id }) { index, track ->
                        Box {
                            BorderlessTrackRow(
                                track = track,
                                isFirst = index == 0,
                                isLast = index == liked.lastIndex,
                                onClick = {
                                    libVm.play(liked, liked.indexOf(track), "Favorites")
                                },
                                onMoreClick = {
                                    trackActionTarget = track
                                },
 menuContent = {
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
                                            onAddToPlaylist(track)
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
                            )
                        }
                    }
                }

                // Theme-adaptive bottom scroll fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PlaylistsPage(
    libVm: LibraryViewModel,
    listState: LazyListState,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit
) {
    val playlists by libVm.getPlaylists().observeAsState(emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    val context = LocalContext.current
    val library = remember { Screens.container(context).library }
    val libraryAdmin = remember { Screens.container(context).libraryAdmin() }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text(stringResource(R.string.library_delete_playlist_title)) },
            text = { Text(stringResource(R.string.library_delete_playlist_message, playlistToDelete?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    val p = playlistToDelete
                    playlistToDelete = null
                    if (p != null) {
                        libraryAdmin.deletePlaylist(p.id)
                    }
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = stringResource(R.string.library_new_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
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
                            Toast.makeText(context, context.getString(R.string.playlist_created_toast, name), Toast.LENGTH_SHORT).show()
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.playlist_create_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
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
                                    text = stringResource(R.string.library_new_playlist),
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
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
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
                                    text = formatTrackCount(playlist.trackCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { playlistToDelete = playlist },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Theme-adaptive bottom scroll fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            )
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
        Box(modifier = Modifier.fillMaxSize()) {
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

            // Theme-adaptive bottom scroll fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            )
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncArtworkImage(
                                artworkUri = artist.imageUri,
                                seed = artist.placeholderSeed,
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                cornerDp = 14.dp,
                                monogram = artist.name
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = formatTrackCount(artist.trackCount),
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

            // Theme-adaptive bottom scroll fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun formatTrackCount(count: Int): String {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val formattedCount = NumberFormat.getIntegerInstance(locale).format(count)
    return context.resources.getQuantityString(R.plurals.tracks_count, count, formattedCount)
}

/**
 * Play + Shuffle as a touching pair of buttons. The leading one (filled) keeps square right corners, the
 * trailing one (tonal) square left corners, so they read as one segmented control; outer corners stay rounded.
 */
@Composable
fun TracksHeader(
    itemCount: Int,
    sortLabel: String?,
    onSortClick: (() -> Unit)?,
    onShuffleClick: () -> Unit,
    onPlayAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTrackCount(itemCount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Compact play / shuffle group, placed next to the sort menu.
        val compactPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        Row {
            Button(
                onClick = onPlayAllClick,
                shape = ConnectedButtonShapes.leading,
                contentPadding = compactPadding,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
            }
            FilledTonalButton(
                onClick = onShuffleClick,
                shape = ConnectedButtonShapes.trailing,
                contentPadding = compactPadding,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", modifier = Modifier.size(20.dp))
            }
        }

        if (sortLabel != null && onSortClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = onSortClick) {
                Text(sortLabel, maxLines = 1)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Outer corners rounded, inner corners square, for two buttons that touch edge to edge. */
private object ConnectedButtonShapes {
    private val outer: Dp = 26.dp

    val leading: RoundedCornerShape = RoundedCornerShape(topStart = outer, bottomStart = outer)
    val trailing: RoundedCornerShape = RoundedCornerShape(topEnd = outer, bottomEnd = outer)
}

/**
 * Track row styled as a grouped-list item: no border, small gaps between rows,
 * large rounded corners only on the first and last row of the group.
 */
@Composable
fun BorderlessTrackRow(
    track: Track,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    menuContent: @Composable () -> Unit = {},
    isPlaying: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val outer = 24.dp
    val inner = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outer else inner,
        topEnd = if (isFirst) outer else inner,
        bottomStart = if (isLast) outer else inner,
        bottomEnd = if (isLast) outer else inner
    )
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp)
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncArtworkImage(
            artworkUri = track.artworkUri,
            seed = track.placeholderSeed,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp)),
            cornerDp = 12.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatTrackDuration(track.durationMs.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // The menu lives in the same Box as the button so it opens anchored to the three dots.
        Box {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            menuContent()
        }
    }
    }
}

private fun formatTrackDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
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
        val initialPage = arguments?.getInt(ARG_PAGE, 0) ?: 0

        return ComposeView(requireContext()).apply {
            setContent {
                val isScanning by c.libraryAdmin().isScanning().observeAsState(false)
                val theme = remember { PrefsSettingsStore(requireContext()).getTheme() }
                val currentMode = ThemeApplier.toMode(theme)

                ShockwaveTheme(mode = currentMode) {
                    UnifiedHomeScreen(
                        homeVm = homeVm,
                        libVm = libVm,
                        isScanning = isScanning,
                        initialPage = initialPage,
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

    companion object {
        private const val ARG_PAGE = "initial_page"

        @JvmStatic
        fun newInstance(initialPage: Int = 0): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply { putInt(ARG_PAGE, initialPage) }
            }
        }
    }
}

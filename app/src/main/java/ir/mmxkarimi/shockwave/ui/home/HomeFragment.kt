package ir.mmxkarimi.shockwave.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.HomeItem
import ir.mmxkarimi.shockwave.domain.model.HomeSection
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.components.SectionHeader
import ir.mmxkarimi.shockwave.ui.components.TrackRow
import ir.mmxkarimi.shockwave.ui.library.AsyncArtworkImage
import ir.mmxkarimi.shockwave.ui.library.TrackMenu
import ir.mmxkarimi.shockwave.ui.nav.AudioPermission
import ir.mmxkarimi.shockwave.ui.nav.ScreenNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    isScanning: Boolean,
    onNavigateTrackList: (ScreenNavigator.TrackList, String?) -> Unit
) {
    val sections by vm.getSections().observeAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Screens.container(context).onAudioPermissionGranted()
        }
    }

    if (sections == null || (sections?.isEmpty() == true && isScanning)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
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
                    style = MaterialTheme.typography.titleLarge,
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
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            val nonNullSections = sections ?: emptyList()
            items(nonNullSections, key = { it.id }) { section ->
                HomeSectionRow(
                    section = section,
                    onItemClick = { item ->
                        when (item.kind) {
                            HomeItem.Kind.TRACK -> {
                                item.track?.let { track ->
                                    vm.playFromSection(section, track)
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
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.items, key = { it.id }) { item ->
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
            .width(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
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
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cornerDp = 12.dp
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
        val vm = ViewModelProvider(this, HomeViewModel.factory(c.homeFeed, c.player))[HomeViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                val isScanning by c.libraryAdmin().isScanning().observeAsState(false)
                ShockwaveTheme {
                    HomeScreen(
                        vm = vm,
                        isScanning = isScanning,
                        onNavigateTrackList = { kind, id ->
                            Screens.openTrackList(this@HomeFragment, kind, id)
                        }
                    )
                }
            }
        }
    }
}

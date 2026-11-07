package ir.mmxkarimi.shockwave.ui.player

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.theme.ColorExtractor
import ir.mmxkarimi.shockwave.ui.widget.ArtworkLoader
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The full player's artwork palette (same extraction as PlayerFragment) for other surfaces such as the mini player.
 * Palettes are cached per track, so going back and forth between screens shows the colour at once.
 */
object ArtworkPalette {
    private const val SIZE_PX = 256 // the size the track lists already load, usually a cache hit
    private val cache = LruCache<String, PlayerPalette>(64)

    fun cached(trackId: String): PlayerPalette? = cache.get(trackId)

    /** Colours of [cover], exactly as the full player derives them. */
    fun fromCover(cover: Bitmap): PlayerPalette? {
        val tiny = PlayerBackdropView.prepare(cover) ?: return null
        val px = IntArray(tiny.width * tiny.height)
        tiny.getPixels(px, 0, tiny.width, 0, 0, tiny.width, tiny.height)
        return PlayerPalette.fromColor(ColorExtractor.extractSeed(px, px.size))
    }

    internal suspend fun load(context: android.content.Context, track: Track): PlayerPalette {
        cache.get(track.id)?.let { return it }
        val uri = track.artworkUri
        val cover = if (uri == null) null else withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Bitmap?> { cont ->
                ArtworkLoader.get(context).load(uri, SIZE_PX) { _, bmp -> if (cont.isActive) cont.resume(bmp) }
            }
        }
        val palette = cover?.let { withContext(Dispatchers.Default) { fromCover(it) } }
            ?: PlayerPalette.forSeed(track.placeholderSeed) // no cover: same fallback as the full player
        cache.put(track.id, palette)
        return palette
    }
}

/**
 * The artwork palette of [track], or null when [enabled] is false (the "artwork colours" setting is off) or there is
 * no track: callers then keep their theme colours.
 */
@Composable
fun rememberArtworkPalette(track: Track?, enabled: Boolean): PlayerPalette? {
    val context = LocalContext.current
    var palette by remember(track?.id) { mutableStateOf(track?.let { ArtworkPalette.cached(it.id) }) }
    LaunchedEffect(track?.id, track?.artworkUri, enabled) {
        if (track != null && enabled) palette = ArtworkPalette.load(context, track)
    }
    return if (enabled && track != null) palette else null
}

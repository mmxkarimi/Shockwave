package ir.mmxkarimi.shockwave.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mmxkarimi.shockwave.ui.widget.ArtworkLoader
import ir.mmxkarimi.shockwave.ui.widget.ArtworkView
import ir.mmxkarimi.shockwave.ui.widget.PlaceholderArt

/**
 * Art helper and Compose Artwork component.
 */
object Art {
    @JvmStatic
    fun bind(view: ArtworkView, artworkUri: String?, seed: Int, cornerDp: Float, circle: Boolean) {
        view.setZoom(1f)
        view.setCircle(circle)
        if (!circle) view.setCornerRadiusDp(cornerDp)
        view.setArtwork(artworkUri, seed)
    }

    @JvmStatic
    fun artist(view: ArtworkView, imageUri: String?, name: String?, seed: Int) {
        view.setCircle(true)
        if (imageUri == null) {
            view.setMonogram(name, seed)
        } else {
            view.setZoom(1.8f)
            view.setArtwork(imageUri, seed)
        }
    }
}

@Composable
fun AsyncArtworkImage(
    artworkUri: String?,
    seed: Int,
    modifier: Modifier = Modifier,
    cornerDp: Dp = 12.dp,
    circle: Boolean = false,
    monogram: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember(artworkUri) { mutableStateOf(if (artworkUri != null) ArtworkLoader.get(context).cached(artworkUri, 256) else null) }

    LaunchedEffect(artworkUri) {
        if (artworkUri != null) {
            val cached = ArtworkLoader.get(context).cached(artworkUri, 256)
            if (cached != null) {
                bitmap = cached
            } else {
                ArtworkLoader.get(context).load(artworkUri, 256) { _, bmp ->
                    bitmap = bmp
                }
            }
        } else {
            bitmap = null
        }
    }

    val shape = if (circle) CircleShape else RoundedCornerShape(cornerDp)
    val colors = remember(seed) { PlaceholderArt.colors(seed) }
    val gradientBrush = remember(colors) {
        Brush.linearGradient(
            colors = listOf(Color(colors[0]), Color(colors[1]))
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!monogram.isNullOrBlank()) {
            val letter = monogram.trim().take(1).uppercase()
            Text(
                text = letter,
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}

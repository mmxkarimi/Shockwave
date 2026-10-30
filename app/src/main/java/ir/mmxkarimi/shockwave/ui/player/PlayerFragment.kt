package ir.mmxkarimi.shockwave.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.LyricLine
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.model.RepeatMode
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.nav.PlayerNavigator
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.player.lyrics.LyricsScrollPolicy
import ir.mmxkarimi.shockwave.ui.playlist.AddToPlaylistSheet
import ir.mmxkarimi.shockwave.ui.queue.QueueFragment
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.settings.ThemeApplier
import ir.mmxkarimi.shockwave.ui.theme.ColorExtractor
import ir.mmxkarimi.shockwave.ui.theme.ShockwaveTheme
import ir.mmxkarimi.shockwave.ui.theme.Tokens
import ir.mmxkarimi.shockwave.ui.widget.ArtworkView
import ir.mmxkarimi.shockwave.ui.widget.MorphPlayPauseView
import ir.mmxkarimi.shockwave.ui.widget.WaveMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private fun Track.id(): String = id
private fun Track.title(): String = title
private fun Track.artistName(): String = artistName
private fun Track.placeholderSeed(): Int = placeholderSeed
private fun PlaybackState.durationMs(): Long = durationMs
private fun PlaybackState.positionMs(): Long = positionMs
private fun PlaybackState.playing(): Boolean = playing
private fun PlaybackState.buffering(): Boolean = buffering
private fun PlaybackState.shuffleEnabled(): Boolean = shuffleEnabled
private fun PlaybackState.repeatMode(): RepeatMode = repeatMode
private fun PlaybackState.currentTrack(): Track? = currentTrack
private fun Lyrics.lines(): List<LyricLine> = lines
private fun LyricLine.text(): String = text

// ------------------------------------------------------------------------------------------------
// Colors shared by every piece of the screen
// ------------------------------------------------------------------------------------------------

@Immutable
private class PlayerColors(
    val light: Boolean,
    val text: Color,
    val textSecondary: Color,   // text @ 0xB8
    val textTime: Color,        // text @ 0xC7
    val primary: Color,
    val onPrimary: Color,
    val accent: Color           // like button, seek bar, lyrics highlight
)

// ------------------------------------------------------------------------------------------------
// Screen
// ------------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    vm: PlayerViewModel,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    animateEntry: Boolean = false
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val light = !isSystemInDarkTheme()
    val motion = remember { PlayerPalette.motionAllowed(ctx) }
    val locale = LocalConfiguration.current.locales[0]

    val state by vm.getState().observeAsState()
    val track by vm.getCurrentTrack().observeAsState()
    val lyrics by vm.getLyrics().observeAsState()
    val showLyrics by vm.getShowLyrics().observeAsState(false)
    val liked by vm.getLiked().observeAsState(false)
    val activeLine by vm.getActiveLyricLine().observeAsState(-1)

    val s = state ?: PlaybackState.empty()
    val currentTrack = track
    val duration = s.durationMs()

    // ---- palette (colors from artwork) ----
    val artworkColors = remember { PrefsSettingsStore(ctx).isArtworkColors() }
    val paletteCache = remember { mutableMapOf<String, PlayerPalette>() }
    val artCache = remember { mutableMapOf<String, Bitmap>() }
    var palette by remember { mutableStateOf(PlayerPalette.forSeed(0)) }
    var tinyArt by remember { mutableStateOf<Bitmap?>(null) }
    val slide = remember { Animatable(1f) }
    var lastTrackId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTrack?.id()) {
        val id = currentTrack?.id()
        if (id != null && paletteCache.containsKey(id)) {
            palette = paletteCache[id]!!
            tinyArt = artCache[id]
        } else {
            palette = if (artworkColors) PlayerPalette.forSeed(currentTrack?.placeholderSeed() ?: 0)
            else PlayerPalette.fromColor(0xFFC6F135.toInt())
            tinyArt = null
        }
        if (lastTrackId != null && id != null && id != lastTrackId && motion) {
            slide.snapTo(0f)
            slide.animateTo(1f, tween(320))
        }
        lastTrackId = id
    }

    val onArtwork: (Bitmap?) -> Unit = onArtwork@{ cover ->
        if (cover == null || !artworkColors) return@onArtwork
        val tiny = PlayerBackdropView.prepare(cover) ?: return@onArtwork
        val px = IntArray(tiny.width * tiny.height)
        tiny.getPixels(px, 0, tiny.width, 0, 0, tiny.width, tiny.height)
        val extracted = PlayerPalette.fromColor(ColorExtractor.extractSeed(px, px.size))
        val id = currentTrack?.id()
        if (id != null) {
            paletteCache[id] = extracted
            artCache[id] = tiny
        }
        palette = extracted
        tinyArt = tiny
    }

    val textInt = if (light) PlayerBackdropView.TEXT_ON_LIGHT else PlayerBackdropView.TEXT_ON_DARK
    val text = Color(textInt)
    val colors = PlayerColors(
        light = light,
        text = text,
        textSecondary = text.copy(alpha = 0xB8 / 255f),
        textTime = text.copy(alpha = 0xC7 / 255f),
        primary = Color(palette.primary),
        onPrimary = Color(palette.onPrimary),
        accent = Color(if (light) palette.glow else palette.primary)
    )

    // ---- seek scrubbing ----
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableLongStateOf(0L) }
    val shownPos = if (scrubbing) scrubPos else s.positionMs()

    // ---- swipe down to close + enter animation ----
    var dragY by remember { mutableFloatStateOf(0f) }
    var rootHeight by remember { mutableFloatStateOf(1f) }
    val enter = remember { Animatable(if (animateEntry && motion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) enter.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootHeight = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationY = dragY
                alpha = 1f - 0.35f * min(1f, dragY / rootHeight)
            }
            .background(if (light) Color(0xFFF7F9FF) else Color(0xFF0F1216))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragY = (dragY + delta).coerceAtLeast(0f) },
                onDragStopped = { velocity ->
                    val close = dragY > rootHeight * 0.22f || velocity > 1200f
                    if (close) {
                        animate(dragY, rootHeight, animationSpec = tween(if (motion) 200 else 0)) { v, _ -> dragY = v }
                        onClose()
                    } else {
                        animate(dragY, 0f, animationSpec = tween(220)) { v, _ -> dragY = v }
                    }
                }
            )
            // android:clickable="true": do not let touches fall through to the screen below
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // player_backdrop
        AndroidView(
            factory = { c -> PlayerBackdropView(c) },
            update = { v ->
                v.setLight(light)
                v.setContent(palette, tinyArt, true)
            },
            modifier = Modifier.fillMaxSize()
        )

        // player_content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = enter.value
                    translationY = (1f - enter.value) * 72.dp.toPx()
                }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            // player_handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.text.copy(alpha = 0.32f))
            )

            // ---- player_top: lyrics toggle · pill group · heart ----
            val likeScale = remember { Animatable(1f) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerIconButton(
                    onClick = { vm.toggleLyrics() },
                    modifier = Modifier.graphicsLayer { alpha = if (showLyrics) 1f else 0.82f }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.player_ic_lyrics),
                        contentDescription = stringResource(
                            if (showLyrics) R.string.player_show_cover else R.string.player_show_lyrics
                        ),
                        tint = if (showLyrics) colors.accent else colors.text
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.text.copy(alpha = 0.10f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillToggle(on = s.shuffleEnabled(), colors = colors, onClick = { vm.toggleShuffle() }) { tint ->
                        Icon(
                            painter = painterResource(R.drawable.player_ic_shuffle),
                            contentDescription = stringResource(
                                if (s.shuffleEnabled()) R.string.player_shuffle_on else R.string.player_shuffle_off
                            ),
                            tint = tint
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                    PillToggle(on = s.repeatMode() != RepeatMode.OFF, colors = colors, onClick = { vm.cycleRepeat() }) { tint ->
                        Icon(
                            painter = painterResource(
                                if (s.repeatMode() == RepeatMode.ONE) R.drawable.player_ic_repeat_one
                                else R.drawable.player_ic_repeat
                            ),
                            contentDescription = stringResource(
                                when (s.repeatMode()) {
                                    RepeatMode.ONE -> R.string.player_repeat_one
                                    RepeatMode.ALL -> R.string.player_repeat_all
                                    else -> R.string.player_repeat_off
                                }
                            ),
                            tint = tint
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                    PillToggle(on = false, colors = colors, onClick = onOpenQueue) { tint ->
                        Icon(
                            painter = painterResource(R.drawable.player_ic_queue),
                            contentDescription = stringResource(R.string.player_queue),
                            tint = tint
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                PlayerIconButton(
                    onClick = {
                        view.performHapticFeedback(
                            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                            else HapticFeedbackConstants.VIRTUAL_KEY
                        )
                        scope.launch {
                            likeScale.animateTo(1.18f, tween(110))
                            likeScale.animateTo(1f, tween(180))
                        }
                        vm.toggleLike()
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = likeScale.value
                        scaleY = likeScale.value
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (liked) R.drawable.player_ic_favorite else R.drawable.player_ic_favorite_border
                        ),
                        contentDescription = stringResource(if (liked) R.string.player_unlike else R.string.player_like),
                        tint = colors.accent
                    )
                }
            }

            // ---- player_stage: cover OR lyrics ----
            var stageSwipeOffset by remember { mutableFloatStateOf(0f) }
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { stageSwipeOffset = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                stageSwipeOffset += dragAmount
                            },
                            onDragEnd = {
                                val threshold = 100f
                                if (stageSwipeOffset < -threshold) {
                                    vm.next()
                                } else if (stageSwipeOffset > threshold) {
                                    vm.previous()
                                }
                            }
                        )
                    }
            ) {
                val side = (min(maxWidth.value, maxHeight.value).dp - 16.dp).coerceAtLeast(0.dp)
                val artworkUpdated by rememberUpdatedState(onArtwork)

                AndroidView(
                    factory = { c ->
                        ArtworkView(c).apply {
                            setCornerRadiusDp(32)
                            setListener { bmp -> artworkUpdated(bmp) }
                        }
                    },
                    update = { v ->
                        val t = currentTrack
                        if (t != null && v.tag != t.id()) {
                            v.tag = t.id()
                            v.bind(t)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(side)
                        .graphicsLayer {
                            alpha = if (showLyrics) 0f else (0.4f + 0.6f * slide.value)
                            translationX = (1f - slide.value) * 24.dp.toPx()
                        }
                        .shadow(if (showLyrics) 0.dp else 18.dp, RoundedCornerShape(32.dp))
                )

                if (showLyrics) {
                    LyricsStage(
                        lyrics = lyrics,
                        activeLine = activeLine,
                        colors = colors,
                        motion = motion,
                        onLineClick = { vm.seekToLine(it) },
                        onRetry = { vm.retryLyrics() }
                    )
                }
            }

            // ---- Title row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title() ?: stringResource(R.string.player_nothing_playing),
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = 1),
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Content
                        ),
                        color = colors.text,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = currentTrack?.artistName() ?: "",
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = 17.sp, textDirection = TextDirection.Content),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    PlayerIconButton(onClick = { menuOpen = true }) {
                        Icon(
                            painter = painterResource(R.drawable.player_ic_more),
                            contentDescription = stringResource(R.string.player_more),
                            tint = colors.text,
                            modifier = Modifier.rotate(90f)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (showLyrics) R.string.player_show_cover else R.string.player_show_lyrics))
                            },
                            onClick = { menuOpen = false; vm.toggleLyrics() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.player_queue)) },
                            onClick = { menuOpen = false; onOpenQueue() }
                        )
                        // the sleep timer lives in the queue sheet
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.player_sleep_timer)) },
                            onClick = { menuOpen = false; onOpenQueue() }
                        )
                        if (currentTrack != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.player_add_to_playlist)) },
                                onClick = { menuOpen = false; onAddToPlaylist(currentTrack) }
                            )
                        }
                    }
                }
            }

            // ---- seek + times + transport are always LTR (tape direction) ----
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WavySeekBar(
                        position = shownPos,
                        duration = duration,
                        playing = s.playing(),
                        activeColor = colors.accent,
                        inactiveColor = if (light) Color(0x3D181C21) else Color(0x47FFFFFF),
                        motion = motion,
                        description = stringResource(R.string.player_seek),
                        onScrubStart = { scrubbing = true; scrubPos = it },
                        onScrub = { scrubPos = it },
                        onScrubEnd = {
                            scrubbing = false
                            vm.seekTo(it)
                        },
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = PlayerFormat.time(shownPos, locale),
                            modifier = Modifier.weight(1f),
                            style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                            color = colors.textTime
                        )
                        Text(
                            text = PlayerFormat.remaining(shownPos, duration, locale),
                            style = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                            color = colors.textTime
                        )
                    }

                    // Transport: rounded prev · cookie play/pause · rounded next
                    val playDesc = stringResource(if (s.playing()) R.string.player_pause else R.string.player_play)
                    val nextEnabled = s.hasNext() || s.repeatMode() == RepeatMode.ALL
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .height(104.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SideButton(
                            enabled = true,
                            colors = colors,
                            onClick = { vm.previous() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.player_ic_prev),
                                contentDescription = stringResource(R.string.player_previous),
                                tint = colors.text
                            )
                        }

                        AndroidView(
                            factory = { c ->
                                MorphPlayPauseView(c).apply {
                                    setOnClickListener { v ->
                                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        vm.togglePlayPause()
                                    }
                                }
                            },
                            update = { v ->
                                v.setColors(
                                    if (light) palette.glow else palette.primary,
                                    if (light) 0xFFFFFFFF.toInt() else palette.onPrimary
                                )
                                v.setPlaying(s.playing(), true)
                                v.setBuffering(s.buffering())
                                v.contentDescription = playDesc
                                v.isEnabled = s.currentTrack() != null
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(104.dp)
                        )

                        SideButton(
                            enabled = nextEnabled,
                            colors = colors,
                            onClick = { vm.next() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.player_ic_next),
                                contentDescription = stringResource(R.string.player_next),
                                tint = colors.text
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Wavy seek bar (Compose port of WavySeekView + WaveMath)
// ------------------------------------------------------------------------------------------------

/**
 * The played part is a sine wave that travels one wavelength per [Tokens.WAVE_PERIOD_MS] while playing and flattens
 * in [Tokens.WAVE_SETTLE_MS] on pause. The rest is a straight line. Reduced motion: a static wave.
 * Frames are only requested (about 30 fps) while playing or while the wave is settling.
 */
@Composable
private fun WavySeekBar(
    position: Long,
    duration: Long,
    playing: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    motion: Boolean,
    description: String,
    onScrubStart: (Long) -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val amplitude = Tokens.WAVE_AMPLITUDE * density
    val wavelength = Tokens.WAVE_LENGTH * density
    val period = Tokens.WAVE_PERIOD_MS.toLong()
    val settle = Tokens.WAVE_SETTLE_MS.toLong()

    var amp by remember { mutableFloatStateOf(0f) }    // 0 flat .. 1 full
    var phase by remember { mutableFloatStateOf(0f) }
    val origin = remember { SystemClock.uptimeMillis() }

    LaunchedEffect(playing, motion) {
        if (!motion) {
            amp = if (playing) 1f else 0f
            phase = 0f
            return@LaunchedEffect
        }
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = now - last
            if (dt < 33) continue
            last = now
            amp = WaveMath.approach(amp, if (playing) 1f else 0f, dt, settle)
            phase = if (playing) WaveMath.phaseAt(SystemClock.uptimeMillis() - origin, period) else 0f
            if (!playing && amp <= 0f) break
        }
    }

    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val safeDuration = duration.coerceAtLeast(1L)
    val fraction = dragFraction ?: (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    val path = remember { Path() }

    fun posAt(x: Float, width: Float): Long =
        ((x / width).coerceIn(0f, 1f) * safeDuration).toLong()

    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .pointerInput(safeDuration) {
                detectTapGestures { o ->
                    val p = posAt(o.x, size.width.toFloat())
                    onScrubStart(p); onScrub(p); onScrubEnd(p)
                }
            }
            .pointerInput(safeDuration) {
                var last = 0L
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragFraction = (o.x / size.width).coerceIn(0f, 1f)
                        last = posAt(o.x, size.width.toFloat())
                        onScrubStart(last)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        last = posAt(change.position.x, size.width.toFloat())
                        onScrub(last)
                    },
                    onDragEnd = { dragFraction = null; onScrubEnd(last) },
                    onDragCancel = { dragFraction = null; onScrubEnd(last) }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val gap = 6f * density
            val left = stroke / 2f
            val right = size.width - stroke / 2f
            val cy = size.height / 2f
            val thumbW = 4.dp.toPx()
            val thumbH = 28.dp.toPx()
            val thumbX = (left + fraction * (right - left)).coerceIn(left, right)

            // played part
            val end = thumbX - gap
            if (end > left) {
                val a = amplitude * amp
                if (a < 0.25f) {
                    drawLine(activeColor, Offset(left, cy), Offset(end, cy), stroke, StrokeCap.Round)
                } else {
                    path.rewind()
                    val step = 2f * density
                    path.moveTo(left, cy + WaveMath.y(0f, wavelength, a, phase))
                    var x = left + step
                    while (x < end) {
                        path.lineTo(x, cy + WaveMath.y(x - left, wavelength, a, phase))
                        x += step
                    }
                    path.lineTo(end, cy + WaveMath.y(end - left, wavelength, a, phase))
                    drawPath(path, activeColor, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // rest of the track
            val start = thumbX + gap
            if (start < right) {
                drawLine(inactiveColor, Offset(start, cy), Offset(right, cy), stroke, StrokeCap.Round)
            }

            // thumb
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(thumbX - thumbW / 2f, cy - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f)
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Lyrics
// ------------------------------------------------------------------------------------------------

@Composable
private fun LyricsStage(
    lyrics: Lyrics?,
    activeLine: Int,
    colors: PlayerColors,
    motion: Boolean,
    onLineClick: (Int) -> Unit,
    onRetry: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val policy = remember { LyricsScrollPolicy() }
    var showBack by remember { mutableStateOf(false) }
    var resumeToken by remember { mutableIntStateOf(0) }
    val active by rememberUpdatedState(activeLine)
    val lines: List<LyricLine> = lyrics?.lines ?: emptyList()

    suspend fun goTo(i: Int) {
        if (i < 0 || i >= lines.size) return
        if (motion) listState.animateScrollToItem(i) else listState.scrollToItem(i)
    }

    // follow the active line unless the user is browsing
    LaunchedEffect(activeLine, lines.size) {
        if (policy.shouldAutoScroll(SystemClock.uptimeMillis())) goTo(activeLine)
    }
    // user drag -> pause following, show the "back to current" chip
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) {
                policy.onUserScroll(SystemClock.uptimeMillis())
                showBack = true
                resumeToken++
            }
        }
    }
    LaunchedEffect(resumeToken) {
        if (resumeToken > 0) {
            delay(LyricsScrollPolicy.PAUSE_MS.toLong())
            policy.resume()
            showBack = false
            goTo(active)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight

        if (lines.isEmpty()) {
            val online = remember(lyrics) { isOnline(ctx) }
            val offline = PlayerViewModel.showOfflineRetry(lyrics, online)
            var retrying by remember(lyrics) { mutableStateOf(false) }
            val loading = lyrics == null || retrying

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        when {
                            loading -> R.string.lyrics_loading
                            offline -> R.string.lyrics_offline
                            else -> R.string.lyrics_none
                        }
                    ),
                    color = colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (!loading) {
                    Text(
                        text = stringResource(if (offline) R.string.lyrics_offline_hint else R.string.lyrics_none_hint),
                        modifier = Modifier.padding(top = 8.dp),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                if (offline && !loading) {
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .height(48.dp)
                            .defaultMinSize(minWidth = 96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.text.copy(alpha = 0.14f))
                            .clickable { retrying = true; onRetry() }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.lyrics_retry), color = colors.text, fontSize = 15.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalFadingEdge(56.dp),
                // same padding as the RecyclerView: active line sits at 40% of the height
                contentPadding = PaddingValues(top = h * 0.4f, bottom = h * 0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val isActive = index == activeLine
                    val lineText = line.text
                    val isRtl = lineText.firstOrNull { !it.isWhitespace() }?.let {
                        Character.getDirectionality(it) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                        Character.getDirectionality(it) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                    } == true
                    Text(
                        text = lineText,
                        style = TextStyle(
                            fontSize = if (isActive) 22.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            textDirection = TextDirection.Content
                        ),
                        color = if (isActive) colors.accent else colors.text.copy(alpha = 0.55f),
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                policy.resume()
                                showBack = false
                                onLineClick(index)
                            }
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBack,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.text.copy(alpha = 0.18f))
                        .clickable {
                            policy.resume()
                            showBack = false
                            scope.launch { goTo(active) }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.lyrics_back_to_current), color = colors.text, fontSize = 13.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Small building blocks
// ------------------------------------------------------------------------------------------------

/** Player.IconButton: 48 dp, no background. */
@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Player.ToggleButton inside the pill group: filled with the palette color when on (setToggle in the Java code). */
@Composable
private fun PillToggle(
    on: Boolean,
    colors: PlayerColors,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (on) colors.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { icon(if (on) colors.onPrimary else colors.text) }
}

/** Player.SideButton: rounded prev / next. Dimmed at 38% when disabled. */
@Composable
private fun SideButton(
    enabled: Boolean,
    colors: PlayerColors,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .clip(RoundedCornerShape(28.dp))
            .background(colors.text.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Same idea as android:requiresFadingEdge="vertical" + fadingEdgeLength. */
private fun Modifier.verticalFadingEdge(length: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = (length.toPx() / size.height).coerceIn(0f, 0.49f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                f to Color.Black,
                1f - f to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

private fun isOnline(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    return try {
        cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (e: SecurityException) {
        true
    }
}

// ------------------------------------------------------------------------------------------------
// Fragment
// ------------------------------------------------------------------------------------------------

class PlayerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        val c = Screens.container(this)
        val vm = ViewModelProvider(
            this,
            PlayerViewModel.factory(c.player, c.library, c.lyrics, c.waveform)
        )[PlayerViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val theme = remember { PrefsSettingsStore(requireContext()).getTheme() }
                val currentMode = ThemeApplier.toMode(theme)
                ShockwaveTheme(mode = currentMode) {
                    FullPlayerScreen(
                        vm = vm,
                        onClose = { collapse() },
                        onOpenQueue = {
                            val nav = PlayerNavigator.from(requireContext())
                            if (nav != null) nav.openQueue() else QueueFragment.newInstance().show(parentFragmentManager, QueueFragment.TAG)
                        },
                        onAddToPlaylist = { track ->
                            AddToPlaylistSheet.show(parentFragmentManager, track)
                        },
                        animateEntry = state == null
                    )
                }
            }
        }
    }

    private fun collapse() {
        val nav = PlayerNavigator.from(requireContext())
        if (nav != null) nav.closeFullPlayer()
        else requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    companion object {
        const val TAG = "player"

        @JvmStatic
        fun newInstance(): PlayerFragment = PlayerFragment()
    }
}

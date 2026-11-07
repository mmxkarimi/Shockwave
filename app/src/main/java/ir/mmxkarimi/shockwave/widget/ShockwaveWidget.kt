package ir.mmxkarimi.shockwave.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ir.mmxkarimi.shockwave.MainActivity
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.ui.settings.SettingsStore
import ir.mmxkarimi.shockwave.ui.settings.PrefsSettingsStore
import ir.mmxkarimi.shockwave.ui.theme.LimeBlackColorScheme
import ir.mmxkarimi.shockwave.ui.theme.LimeDarkColorScheme
import ir.mmxkarimi.shockwave.ui.theme.LimeLightColorScheme

/**
 * The home-screen widget: one widget that reshapes itself with its size.
 *  - compact (about 4x1): small cover, title / artist, transport controls in a row;
 *  - wide (about 4x2): big cover, title, artist, progress bar and controls;
 *  - square (about 2x2): the cover fills the widget with a translucent control pill on its bottom edge.
 * Colours follow the app: Material You when enabled (Android 12+), the app's lime scheme otherwise.
 *
 * Shapes (surface, buttons, pill, placeholder) are tinted drawables from res/drawable/widget_shape_*.xml. Rounded
 * corners are not made with Glance's cornerRadius or appWidgetBackground: cornerRadius is only applied on Android 12+
 * (controls were square on Android 10), and mixing the system widget background with a colour is the likely source of
 * the corners changing between renders on 12.
 */
object ShockwaveWidget : GlanceAppWidget() {
    private val COMPACT = DpSize(180.dp, 48.dp)
    private val SQUARE = DpSize(110.dp, 100.dp)
    /** A 4x1 on launchers with tall cells (Samsung: about 116 dp): still one row, with a bigger cover. */
    private val COMPACT_TALL = DpSize(250.dp, 100.dp)
    private val WIDE = DpSize(250.dp, WIDE_MIN_HEIGHT_DP.dp)

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, SQUARE, COMPACT_TALL, WIDE))

    private val fallbackColors by lazy { ColorProviders(light = LimeLightColorScheme, dark = LimeDarkColorScheme) }
    private val blackColors by lazy { ColorProviders(light = LimeLightColorScheme, dark = LimeBlackColorScheme) }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fresh state for a new session; later changes arrive through WidgetState (see ShockwaveWidgetUpdater).
        val initial = WidgetSnapshotReader.read(context)
        WidgetState.snapshot.value = initial
        val settings = PrefsSettingsStore(context)
        val black = settings.getTheme() == SettingsStore.Theme.BLACK
        val dynamic = settings.isDynamicColor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !black
        provideContent {
            val snapshot = WidgetState.snapshot.collectAsState().value ?: initial
            val colors = when {
                dynamic -> GlanceTheme.colors
                black -> blackColors
                else -> fallbackColors
            }
            GlanceTheme(colors = colors) { WidgetContent(snapshot) }
        }
    }
}

/** Below this height the wide layout (cover, texts, progress, controls stacked) does not fit and gets squeezed. */
internal const val WIDE_MIN_HEIGHT_DP = 140f

internal enum class WidgetLayout { COMPACT, SQUARE, WIDE }

/**
 * Picks the layout for a widget of [widthDp] x [heightDp]. A 4x1 widget on launchers with tall cells is about
 * 116 dp high: that must stay a single row (compact), because the wide layout needs about 135 dp.
 */
internal fun widgetLayoutFor(widthDp: Float, heightDp: Float): WidgetLayout = when {
    widthDp >= 250f && heightDp >= WIDE_MIN_HEIGHT_DP -> WidgetLayout.WIDE
    widthDp < 250f && heightDp >= 100f -> WidgetLayout.SQUARE
    else -> WidgetLayout.COMPACT
}

/** Progress bitmap size in pixels; the image is stretched to the bar's width and height. */
private const val PROGRESS_PX_W = 240
private const val PROGRESS_PX_H = 8

@Composable
private fun openApp(): Action =
    actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))

@Composable
private fun WidgetContent(s: WidgetSnapshot) {
    val size = LocalSize.current
    Box(modifier = GlanceModifier.fillMaxSize()) {
        // Widget surface: a tinted rounded rectangle that fills the whole widget.
        Image(
            provider = ImageProvider(R.drawable.widget_shape_surface),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.surface)
        )
        when (widgetLayoutFor(size.width.value, size.height.value)) {
            WidgetLayout.WIDE -> WideLayout(s)
            WidgetLayout.SQUARE -> SquareLayout(s)
            WidgetLayout.COMPACT -> CompactLayout(s, tall = size.height >= 100.dp)
        }
    }
}

// ---------------------------------------------------------------- layouts

/** One row: cover, texts, controls. [tall] (a 4x1 with tall cells) gets a bigger cover, text and buttons. */
@Composable
private fun CompactLayout(s: WidgetSnapshot, tall: Boolean) {
    val cover = if (tall) 72.dp else 44.dp
    val skip = if (tall) 40.dp else 36.dp
    val play = if (tall) 48.dp else 40.dp
    Row(
        GlanceModifier.fillMaxSize().padding(if (tall) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(s, GlanceModifier.size(cover).clickable(openApp()), cover)
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackTexts(s, titleSp = if (tall) 16 else 15, artistSp = if (tall) 13 else 12)
        }
        SkipButton(R.drawable.widget_ic_prev, R.string.mp_cd_previous, skip, actionRunCallback<PreviousAction>())
        PlayButton(s.playing, play)
        SkipButton(R.drawable.widget_ic_next, R.string.mp_cd_next, skip, actionRunCallback<NextAction>())
    }
}

@Composable
private fun WideLayout(s: WidgetSnapshot) {
    val context = LocalContext.current
    Row(GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(s, GlanceModifier.size(96.dp).clickable(openApp()), 96.dp)
        Column(modifier = GlanceModifier.defaultWeight().padding(start = 14.dp)) {
            TrackTexts(s, titleSp = 17, artistSp = 13)
            Spacer(GlanceModifier.height(10.dp))
            Image(
                provider = ImageProvider(
                    progressBar(
                        progress = s.progress.coerceIn(0f, 1f),
                        track = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
                        fill = GlanceTheme.colors.primary.getColor(context).toArgb()
                    )
                ),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                contentScale = ContentScale.FillBounds
            )
            Spacer(GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkipButton(R.drawable.widget_ic_prev, R.string.mp_cd_previous, 40.dp, actionRunCallback<PreviousAction>())
                Spacer(GlanceModifier.width(8.dp))
                PlayButton(s.playing, 48.dp)
                Spacer(GlanceModifier.width(8.dp))
                SkipButton(R.drawable.widget_ic_next, R.string.mp_cd_next, 40.dp, actionRunCallback<NextAction>())
            }
        }
    }
}

@Composable
private fun SquareLayout(s: WidgetSnapshot) {
    val context = LocalContext.current
    val surface = GlanceTheme.colors.surface.getColor(context)
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp).clickable(openApp()),
            contentAlignment = Alignment.Center
        ) {
            if (s.artwork != null) {
                Image(
                    provider = ImageProvider(s.artwork),
                    contentDescription = context.getString(R.string.widget_cd_open),
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                NotePlaceholder(44.dp)
            }
        }
        // Translucent pill on the cover's bottom edge.
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(bottom = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = GlanceModifier.width(124.dp).height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_shape_pill),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(ColorProvider(surface.copy(alpha = 0.85f)))
                )
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SkipButton(R.drawable.widget_ic_prev, R.string.mp_cd_previous, 36.dp, actionRunCallback<PreviousAction>())
                    PlayButton(s.playing, 44.dp)
                    SkipButton(R.drawable.widget_ic_next, R.string.mp_cd_next, 36.dp, actionRunCallback<NextAction>())
                }
            }
        }
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun TrackTexts(s: WidgetSnapshot, titleSp: Int, artistSp: Int) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val title = if (s.hasTrack) s.title else context.getString(R.string.widget_idle_title)
    val artist = if (s.hasTrack) s.artist else context.getString(R.string.widget_idle_subtitle)
    Text(
        text = title,
        style = TextStyle(color = colors.onSurface, fontSize = titleSp.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
        modifier = GlanceModifier.fillMaxWidth()
    )
    Text(
        text = artist,
        style = TextStyle(color = colors.onSurfaceVariant, fontSize = artistSp.sp),
        maxLines = 1,
        modifier = GlanceModifier.fillMaxWidth()
    )
}

/** Square cover (pre-rounded bitmap) or the tonal note placeholder. */
@Composable
private fun Cover(s: WidgetSnapshot, modifier: GlanceModifier, size: Dp) {
    val context = LocalContext.current
    Box(modifier, contentAlignment = Alignment.Center) {
        if (s.artwork != null) {
            Image(
                provider = ImageProvider(s.artwork),
                contentDescription = context.getString(R.string.widget_cd_open),
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            NotePlaceholder(size * 0.45f)
        }
    }
}

/** Tonal rounded tile with the note glyph; fills the box it is placed in. */
@Composable
private fun NotePlaceholder(iconSize: Dp) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(R.drawable.widget_shape_cover),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
        )
        NoteIcon(iconSize)
    }
}

@Composable
private fun NoteIcon(size: Dp) {
    Image(
        provider = ImageProvider(R.drawable.widget_ic_note),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
    )
}

@Composable
private fun SkipButton(icon: Int, description: Int, size: Dp, action: Action) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = LocalContext.current.getString(description),
        modifier = GlanceModifier.size(size).padding(size * 0.2f).clickable(action),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
    )
}

@Composable
private fun PlayButton(playing: Boolean, size: Dp) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(size)
            .clickable(actionRunCallback<PlayPauseAction>()),
        contentAlignment = Alignment.Center
    ) {
        // Round button: a tinted oval, so it is round on every Android version.
        Image(
            provider = ImageProvider(R.drawable.widget_shape_oval),
            contentDescription = null,
            // An explicit square, never fillMaxSize: a squeezed parent must not stretch the circle into an oval.
            modifier = GlanceModifier.size(size),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
        )
        Image(
            provider = ImageProvider(if (playing) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
            contentDescription = context.getString(if (playing) R.string.mp_cd_pause else R.string.mp_cd_play),
            modifier = GlanceModifier.size(size * 0.55f),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}

/**
 * Playback progress as a small bitmap: [track] with the played part in [fill]. Drawn by us because the platform
 * progress bar takes its tint from the system accent (the purple) whenever it cannot be recoloured.
 */
private fun progressBar(progress: Float, track: Int, fill: Int): Bitmap {
    val out = Bitmap.createBitmap(PROGRESS_PX_W, PROGRESS_PX_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint()
    paint.color = track
    canvas.drawRect(0f, 0f, PROGRESS_PX_W.toFloat(), PROGRESS_PX_H.toFloat(), paint)
    paint.color = fill
    canvas.drawRect(0f, 0f, PROGRESS_PX_W * progress.coerceIn(0f, 1f), PROGRESS_PX_H.toFloat(), paint)
    return out
}

package ir.mmxkarimi.shockwave.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import ir.mmxkarimi.shockwave.data.settings.SettingsStore
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
 */
object ShockwaveWidget : GlanceAppWidget() {
    private val COMPACT = DpSize(180.dp, 48.dp)
    private val SQUARE = DpSize(110.dp, 100.dp)
    private val WIDE = DpSize(250.dp, 100.dp)

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, SQUARE, WIDE))

    private val fallbackColors by lazy { ColorProviders(light = LimeLightColorScheme, dark = LimeDarkColorScheme) }
    private val blackColors by lazy { ColorProviders(light = LimeLightColorScheme, dark = LimeBlackColorScheme) }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotReader.read(context)
        val settings = PrefsSettingsStore(context)
        val black = settings.getTheme() == SettingsStore.Theme.BLACK
        val dynamic = settings.isDynamicColor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !black
        provideContent {
            val colors = when {
                dynamic -> GlanceTheme.colors
                black -> blackColors
                else -> fallbackColors
            }
            GlanceTheme(colors = colors) { WidgetContent(snapshot) }
        }
    }
}

private val OpenApp: Action get() = actionStartActivity<MainActivity>()

@Composable
private fun WidgetContent(s: WidgetSnapshot) {
    val size = LocalSize.current
    val root = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.surface)
        .cornerRadius(28.dp)
    when {
        size.height >= 100.dp && size.width >= 250.dp -> WideLayout(s, root)
        size.height >= 100.dp -> SquareLayout(s, root)
        else -> CompactLayout(s, root)
    }
}

// ---------------------------------------------------------------- layouts

@Composable
private fun CompactLayout(s: WidgetSnapshot, root: GlanceModifier) {
    Row(root.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(s, GlanceModifier.size(44.dp).clickable(OpenApp), 44.dp)
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackTexts(s, titleSp = 15, artistSp = 12)
        }
        SkipButton(R.drawable.widget_ic_prev, R.string.mp_cd_previous, 36.dp, actionRunCallback<PreviousAction>())
        PlayButton(s.playing, 40.dp)
        SkipButton(R.drawable.widget_ic_next, R.string.mp_cd_next, 36.dp, actionRunCallback<NextAction>())
    }
}

@Composable
private fun WideLayout(s: WidgetSnapshot, root: GlanceModifier) {
    Row(root.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(s, GlanceModifier.size(96.dp).clickable(OpenApp), 96.dp)
        Column(modifier = GlanceModifier.defaultWeight().padding(start = 14.dp)) {
            TrackTexts(s, titleSp = 17, artistSp = 13)
            Spacer(GlanceModifier.height(10.dp))
            LinearProgressIndicator(
                progress = s.progress.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth(),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.surfaceVariant
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
private fun SquareLayout(s: WidgetSnapshot, root: GlanceModifier) {
    val context = LocalContext.current
    val surface = GlanceTheme.colors.surface.getColor(context)
    Box(root, contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp).clickable(OpenApp),
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
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NoteIcon(44.dp)
                }
            }
        }
        // Translucent pill on the cover's bottom edge.
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(bottom = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = GlanceModifier
                    .width(124.dp)
                    .height(52.dp)
                    .background(ColorProvider(surface.copy(alpha = 0.85f)))
                    .cornerRadius(26.dp),
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
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(size * 0.22f),
                contentAlignment = Alignment.Center
            ) {
                NoteIcon(size * 0.45f)
            }
        }
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
            .background(GlanceTheme.colors.primary)
            .cornerRadius(size / 2)
            .clickable(actionRunCallback<PlayPauseAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(if (playing) R.drawable.widget_ic_pause else R.drawable.widget_ic_play),
            contentDescription = context.getString(if (playing) R.string.mp_cd_pause else R.string.mp_cd_play),
            modifier = GlanceModifier.size(size * 0.55f),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}

package ir.mmxkarimi.shockwave.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material 3 Expressive Fallback Seed: #C6F135
val LimePrimaryDark = Color(0xFFC6F135)
val LimeOnPrimaryDark = Color(0xFF283500)
val LimePrimaryContainerDark = Color(0xFF3C4D00)
val LimeOnPrimaryContainerDark = Color(0xFFE2FFA3)

val LimePrimaryLight = Color(0xFF516600)
val LimeOnPrimaryLight = Color(0xFFFFFFFF)
val LimePrimaryContainerLight = Color(0xFFDDF870)
val LimeOnPrimaryContainerLight = Color(0xFF161E00)

val LimeDarkColorScheme = darkColorScheme(
    primary = LimePrimaryDark,
    onPrimary = LimeOnPrimaryDark,
    primaryContainer = LimePrimaryContainerDark,
    onPrimaryContainer = LimeOnPrimaryContainerDark,
    secondary = Color(0xFFC5CAAC),
    onSecondary = Color(0xFF2E331E),
    secondaryContainer = Color(0xFF454A33),
    onSecondaryContainer = Color(0xFFE1E6C7),
    tertiary = Color(0xFFA3D2BE),
    onTertiary = Color(0xFF06372A),
    tertiaryContainer = Color(0xFF224E40),
    onTertiaryContainer = Color(0xFFBEEED9),
    background = Color(0xFF13140F),
    onBackground = Color(0xFFE4E3DA),
    surface = Color(0xFF13140F),
    onSurface = Color(0xFFE4E3DA),
    surfaceVariant = Color(0xFF46483C),
    onSurfaceVariant = Color(0xFFC7C8B8),
    outline = Color(0xFF909283),
    outlineVariant = Color(0xFF46483C),
    surfaceContainerLowest = Color(0xFF0E0F0A),
    surfaceContainerLow = Color(0xFF1B1C17),
    surfaceContainer = Color(0xFF1F201B),
    surfaceContainerHigh = Color(0xFF2A2B25),
    surfaceContainerHighest = Color(0xFF353630)
)

val LimeLightColorScheme = lightColorScheme(
    primary = LimePrimaryLight,
    onPrimary = LimeOnPrimaryLight,
    primaryContainer = LimePrimaryContainerLight,
    onPrimaryContainer = LimeOnPrimaryContainerLight,
    secondary = Color(0xFF5C6146),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E6C7),
    onSecondaryContainer = Color(0xFF1A1E08),
    tertiary = Color(0xFF3B6657),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEED9),
    onTertiaryContainer = Color(0xFF002018),
    background = Color(0xFFFDF9EE),
    onBackground = Color(0xFF1C1C16),
    surface = Color(0xFFFDF9EE),
    onSurface = Color(0xFF1C1C16),
    surfaceVariant = Color(0xFFE3E3D4),
    onSurfaceVariant = Color(0xFF46483C),
    outline = Color(0xFF77786B),
    outlineVariant = Color(0xFFC7C8B8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4E9),
    surfaceContainer = Color(0xFFF1EEE3),
    surfaceContainerHigh = Color(0xFFEBE8DE),
    surfaceContainerHighest = Color(0xFFE5E2D8)
)

val LimeBlackColorScheme = darkColorScheme(
    primary = LimePrimaryDark,
    onPrimary = LimeOnPrimaryDark,
    primaryContainer = LimePrimaryContainerDark,
    onPrimaryContainer = LimeOnPrimaryContainerDark,
    secondary = Color(0xFFC5CAAC),
    onSecondary = Color(0xFF2E331E),
    secondaryContainer = Color(0xFF454A33),
    onSecondaryContainer = Color(0xFFE1E6C7),
    tertiary = Color(0xFFA3D2BE),
    onTertiary = Color(0xFF06372A),
    tertiaryContainer = Color(0xFF224E40),
    onTertiaryContainer = Color(0xFFBEEED9),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE4E3DA),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE4E3DA),
    surfaceVariant = Color(0xFF1C1D18),
    onSurfaceVariant = Color(0xFFC7C8B8),
    outline = Color(0xFF909283),
    outlineVariant = Color(0xFF2B2D25),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424)
)

@Composable
fun ShockwaveTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accentScheme: AccentScheme? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mode != ThemeMode.BLACK

    val baseScheme = when {
        mode == ThemeMode.BLACK -> LimeBlackColorScheme
        useDynamic -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> LimeDarkColorScheme
        else -> LimeLightColorScheme
    }

    val finalColorScheme = if (accentScheme != null) {
        baseScheme.copy(
            primary = Color(accentScheme.primary),
            onPrimary = Color(accentScheme.onPrimary),
            primaryContainer = Color(accentScheme.container),
            onPrimaryContainer = Color(accentScheme.onContainer)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        content = content
    )
}

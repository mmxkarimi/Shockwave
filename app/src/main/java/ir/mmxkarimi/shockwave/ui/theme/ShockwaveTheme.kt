package ir.mmxkarimi.shockwave.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LapisPrimaryDark = Color(0xFFA6C8FF)
val LapisOnPrimaryDark = Color(0xFF00315F)
val LapisContainerDark = Color(0xFF1E4878)
val LapisOnContainerDark = Color(0xFFD4E3FF)

val LapisPrimaryLight = Color(0xFF2F5F95)
val LapisOnPrimaryLight = Color(0xFFFFFFFF)
val LapisContainerLight = Color(0xFFD4E3FF)
val LapisOnContainerLight = Color(0xFF001C3A)

private val DarkColorScheme = darkColorScheme(
    primary = LapisPrimaryDark,
    onPrimary = LapisOnPrimaryDark,
    primaryContainer = LapisContainerDark,
    onPrimaryContainer = LapisOnContainerDark,
    background = Color(0xFF121316),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F)
)

private val BlackColorScheme = darkColorScheme(
    primary = LapisPrimaryDark,
    onPrimary = LapisOnPrimaryDark,
    primaryContainer = LapisContainerDark,
    onPrimaryContainer = LapisOnContainerDark,
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121212),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF2B2B2B)
)

private val LightColorScheme = lightColorScheme(
    primary = LapisPrimaryLight,
    onPrimary = LapisOnPrimaryLight,
    primaryContainer = LapisContainerLight,
    onPrimaryContainer = LapisOnContainerLight,
    background = Color(0xFFF9F9FC),
    surface = Color(0xFFF9F9FC),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0)
)

@Composable
fun ShockwaveTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accentScheme: AccentScheme? = null,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseScheme = when {
        mode == ThemeMode.BLACK -> BlackColorScheme
        dark -> DarkColorScheme
        else -> LightColorScheme
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

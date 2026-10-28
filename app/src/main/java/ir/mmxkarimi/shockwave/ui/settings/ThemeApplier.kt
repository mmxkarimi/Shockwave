package ir.mmxkarimi.shockwave.ui.settings

import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ir.mmxkarimi.shockwave.ui.theme.ThemeController
import ir.mmxkarimi.shockwave.ui.theme.ThemeMode

/** Applies theme mode and app language (View layer; ViewModels only store the choice). Delegates to ThemeController. */
object ThemeApplier {
    @JvmStatic
    fun toMode(theme: SettingsStore.Theme): ThemeMode {
        return ThemeMode.valueOf(theme.name)
    }

    @JvmStatic
    fun applyNightMode(theme: SettingsStore.Theme) {
        val mode = toMode(theme)
        val before = AppCompatDelegate.getDefaultNightMode()
        if (theme == SettingsStore.Theme.SYSTEM && before == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) return
        ThemeController.applyMode(mode)
    }

    @JvmStatic
    @StyleRes
    fun themeRes(theme: SettingsStore.Theme): Int {
        return ThemeController.themeResFor(toMode(theme))
    }

    @JvmStatic
    fun applyLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }
}

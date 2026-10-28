package ir.mmxkarimi.shockwave.ui.settings

import android.content.Context
import ir.mmxkarimi.shockwave.data.settings.BatterySaverMode
import ir.mmxkarimi.shockwave.data.settings.ThemeMode
import ir.mmxkarimi.shockwave.ui.nav.Screens

/**
 * The UI's [SettingsStore] view of the app's single settings store (`AppContainer.settings()`).
 */
class PrefsSettingsStore(
    private val data: ir.mmxkarimi.shockwave.data.settings.SettingsStore
) : SettingsStore {

    constructor(context: Context) : this(Screens.container(context).settings())

    override fun getTheme(): SettingsStore.Theme {
        return SettingsStore.Theme.valueOf(data.theme().value.name)
    }

    override fun setTheme(theme: SettingsStore.Theme) {
        data.setTheme(ThemeMode.valueOf(theme.name))
    }

    override fun isArtworkColors(): Boolean {
        return data.artworkColors().value
    }

    override fun setArtworkColors(on: Boolean) {
        data.setArtworkColors(on)
    }

    override fun getLanguage(): String {
        return data.language().value
    }

    override fun setLanguage(tag: String) {
        data.setLanguage(tag)
    }

    override fun isOnlineLyrics(): Boolean {
        return data.lyricsOnline().value
    }

    override fun setOnlineLyrics(on: Boolean) {
        data.setLyricsOnline(on)
    }

    override fun getSleepDefaultMinutes(): Int {
        return data.sleepTimerMinutes().value
    }

    override fun setSleepDefaultMinutes(minutes: Int) {
        data.setSleepTimerMinutes(minutes)
    }

    override fun isBatterySaver(): Boolean {
        return data.batterySaver().value == BatterySaverMode.ON
    }

    override fun setBatterySaver(on: Boolean) {
        data.setBatterySaver(if (on) BatterySaverMode.ON else BatterySaverMode.AUTO)
    }
}

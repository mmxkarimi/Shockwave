package ir.mmxkarimi.shockwave.ui.settings

/**
 * User preferences the UI reads and writes. [PrefsSettingsStore] keeps them in SharedPreferences; the
 * player/lyrics tasks read the same keys through this interface. Values are plain enums/primitives.
 */
interface SettingsStore {

    enum class Theme { SYSTEM, LIGHT, DARK, BLACK }

    companion object {
        /** BCP-47 tag, or "" for the system language. */
        const val LANG_SYSTEM: String = ""
    }

    fun getTheme(): Theme
    fun setTheme(theme: Theme)

    fun isArtworkColors(): Boolean
    fun setArtworkColors(on: Boolean)

    fun getLanguage(): String
    fun setLanguage(tag: String)

    fun isOnlineLyrics(): Boolean
    fun setOnlineLyrics(on: Boolean)

    /** Minutes; 0 means "end of track". */
    fun getSleepDefaultMinutes(): Int
    fun setSleepDefaultMinutes(minutes: Int)

    fun isBatterySaver(): Boolean
    fun setBatterySaver(on: Boolean)
}

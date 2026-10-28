package ir.mmxkarimi.shockwave.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Collections
import java.util.HashSet

/**
 * User settings on SharedPreferences. Every setting is a `LiveData` that is never empty (defaults apply) and
 * updates when its setter is called; setters may be called from any thread and persist asynchronously (`apply`).
 * Values are loaded once in the constructor, so reading `getValue()` from a background thread is fine.
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val theme = MutableLiveData(readEnum(K_THEME, ThemeMode.SYSTEM))
    private val artworkColors = MutableLiveData(prefs.getBoolean(K_ARTWORK_COLORS, true))
    private val language = MutableLiveData(prefs.getString(K_LANGUAGE, "") ?: "")
    private val lyricsOnline = MutableLiveData(prefs.getBoolean(K_LYRICS_ONLINE, true))
    private val lyricsTextSize = MutableLiveData(readEnum(K_LYRICS_SIZE, LyricsTextSize.MEDIUM))
    private val sleepTimerMinutes = MutableLiveData(prefs.getInt(K_SLEEP_MINUTES, DEFAULT_SLEEP_TIMER_MINUTES))
    private val batterySaver = MutableLiveData(readEnum(K_BATTERY_SAVER, BatterySaverMode.AUTO))
    private val includedFolders = MutableLiveData(readSet(K_INCLUDE))
    private val excludedFolders = MutableLiveData(readSet(K_EXCLUDE))

    fun theme(): LiveData<ThemeMode> = theme

    fun setTheme(value: ThemeMode) {
        prefs.edit().putString(K_THEME, value.name).apply()
        publish(theme, value)
    }

    /** Tint the player with colors taken from the artwork. */
    fun artworkColors(): LiveData<Boolean> = artworkColors

    fun setArtworkColors(value: Boolean) {
        prefs.edit().putBoolean(K_ARTWORK_COLORS, value).apply()
        publish(artworkColors, value)
    }

    /** BCP-47 language tag, or the empty string to follow the system language. */
    fun language(): LiveData<String> = language

    fun setLanguage(languageTag: String) {
        prefs.edit().putString(K_LANGUAGE, languageTag).apply()
        publish(language, languageTag)
    }

    /** Whether missing lyrics may be fetched from LRCLIB (network, on demand, cached). */
    fun lyricsOnline(): LiveData<Boolean> = lyricsOnline

    fun setLyricsOnline(value: Boolean) {
        prefs.edit().putBoolean(K_LYRICS_ONLINE, value).apply()
        publish(lyricsOnline, value)
    }

    fun lyricsTextSize(): LiveData<LyricsTextSize> = lyricsTextSize

    fun setLyricsTextSize(value: LyricsTextSize) {
        prefs.edit().putString(K_LYRICS_SIZE, value.name).apply()
        publish(lyricsTextSize, value)
    }

    /** Minutes preselected when the sleep timer sheet opens. */
    fun sleepTimerMinutes(): LiveData<Int> = sleepTimerMinutes

    fun setSleepTimerMinutes(minutes: Int) {
        prefs.edit().putInt(K_SLEEP_MINUTES, minutes).apply()
        publish(sleepTimerMinutes, minutes)
    }

    fun batterySaver(): LiveData<BatterySaverMode> = batterySaver

    fun setBatterySaver(value: BatterySaverMode) {
        prefs.edit().putString(K_BATTERY_SAVER, value.name).apply()
        publish(batterySaver, value)
    }

    /** Absolute directory paths the scanner is limited to; empty = the whole device. Changing it needs a rescan. */
    fun includedFolders(): LiveData<Set<String>> = includedFolders

    fun setIncludedFolders(folders: Set<String>) {
        prefs.edit().putStringSet(K_INCLUDE, HashSet(folders)).apply()
        publish(includedFolders, Collections.unmodifiableSet(HashSet(folders)))
    }

    /** Absolute directory paths the scanner skips (wins over includes). Changing it needs a rescan. */
    fun excludedFolders(): LiveData<Set<String>> = excludedFolders

    fun setExcludedFolders(folders: Set<String>) {
        prefs.edit().putStringSet(K_EXCLUDE, HashSet(folders)).apply()
        publish(excludedFolders, Collections.unmodifiableSet(HashSet(folders)))
    }

    private fun <E : Enum<E>> readEnum(key: String, fallback: E): E {
        return try {
            val stored = prefs.getString(key, null)
            if (stored == null) fallback else java.lang.Enum.valueOf(fallback.javaClass, stored)
        } catch (e: IllegalArgumentException) { // removed value or old type
            fallback
        } catch (e: ClassCastException) {
            fallback
        }
    }

    private fun readSet(key: String): Set<String> =
        Collections.unmodifiableSet(HashSet(prefs.getStringSet(key, emptySet()) ?: emptySet()))

    private companion object {
        const val DEFAULT_SLEEP_TIMER_MINUTES = 30

        private const val PREFS = "settings"
        private const val K_THEME = "theme"
        private const val K_ARTWORK_COLORS = "artwork_colors"
        private const val K_LANGUAGE = "language"
        private const val K_LYRICS_ONLINE = "lyrics_online"
        private const val K_LYRICS_SIZE = "lyrics_text_size"
        private const val K_SLEEP_MINUTES = "sleep_timer_minutes"
        private const val K_BATTERY_SAVER = "battery_saver"
        private const val K_INCLUDE = "folders_include"
        private const val K_EXCLUDE = "folders_exclude"

        private fun <T> publish(live: MutableLiveData<T>, value: T) {
            if (Looper.myLooper() == Looper.getMainLooper()) live.setValue(value) else live.postValue(value)
        }
    }
}

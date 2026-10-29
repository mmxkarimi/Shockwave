package ir.mmxkarimi.shockwave.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Settings screen state. Each setter persists to the store and publishes a new immutable snapshot. */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    data class State(
        val theme: SettingsStore.Theme,
        val dynamicColor: Boolean,
        val artworkColors: Boolean,
        val language: String,
        val onlineLyrics: Boolean,
        val sleepDefaultMinutes: Int,
        val batterySaver: Boolean
    )

    private val stateLiveData = MutableLiveData<State>()

    init {
        publish()
    }

    private fun publish() {
        stateLiveData.value = State(
            theme = store.getTheme(),
            dynamicColor = store.isDynamicColor(),
            artworkColors = store.isArtworkColors(),
            language = store.getLanguage(),
            onlineLyrics = store.isOnlineLyrics(),
            sleepDefaultMinutes = store.getSleepDefaultMinutes(),
            batterySaver = store.isBatterySaver()
        )
    }

    fun getState(): LiveData<State> = stateLiveData

    fun setTheme(t: SettingsStore.Theme) {
        store.setTheme(t)
        ThemeApplier.applyNightMode(t)
        publish()
    }

    fun setDynamicColor(on: Boolean) {
        store.setDynamicColor(on)
        publish()
    }

    fun setArtworkColors(on: Boolean) {
        store.setArtworkColors(on)
        publish()
    }

    fun setLanguage(tag: String) {
        store.setLanguage(tag)
        publish()
    }

    fun setOnlineLyrics(on: Boolean) {
        store.setOnlineLyrics(on)
        publish()
    }

    fun setSleepDefault(minutes: Int) {
        store.setSleepDefaultMinutes(minutes)
        publish()
    }

    fun setBatterySaver(on: Boolean) {
        store.setBatterySaver(on)
        publish()
    }

    companion object {
        /** Sleep timer presets in minutes; 0 = end of track. */
        @JvmField
        val SLEEP_PRESETS = intArrayOf(15, 30, 45, 60, 0)

        @JvmStatic
        fun factory(store: SettingsStore): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(store) as T
                }
            }
        }
    }
}

package ir.mmxkarimi.shockwave.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.domain.model.SleepTimer
import ir.mmxkarimi.shockwave.domain.repository.PlayerController

/** Settings screen state. Each setter persists to the store and publishes a new immutable snapshot. */
class SettingsViewModel(
    private val store: SettingsStore,
    private val player: PlayerController
) : ViewModel() {

    data class State(
        val theme: SettingsStore.Theme,
        val dynamicColor: Boolean,
        val artworkColors: Boolean,
        val language: String,
        val onlineLyrics: Boolean,
        val sleepTimer: SleepTimer,
        val batterySaver: Boolean
    )

    private val stateLiveData = MediatorLiveData<State>()

    init {
        stateLiveData.addSource(player.getState()) { publish() }
        publish()
    }

    private fun publish() {
        val pState = player.getState().value
        val timer = pState?.sleepTimer ?: SleepTimer.OFF
        stateLiveData.value = State(
            theme = store.getTheme(),
            dynamicColor = store.isDynamicColor(),
            artworkColors = store.isArtworkColors(),
            language = store.getLanguage(),
            onlineLyrics = store.isOnlineLyrics(),
            sleepTimer = timer,
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

    fun setSleepMinutes(minutes: Int) {
        val currentTimer = player.getState().value?.sleepTimer
        if (minutes == 0) {
            if (currentTimer?.kind == SleepTimer.Kind.END_OF_TRACK) {
                player.cancelSleepTimer()
            } else {
                player.setSleepTimerEndOfTrack()
            }
        } else {
            val isCurrentCountdown = currentTimer?.kind == SleepTimer.Kind.COUNTDOWN &&
                    (currentTimer.totalMs / 60_000L).toInt() == minutes
            if (isCurrentCountdown) {
                player.cancelSleepTimer()
            } else {
                player.setSleepTimer(minutes)
            }
        }
        publish()
    }

    fun cancelSleepTimer() {
        player.cancelSleepTimer()
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
        fun factory(store: SettingsStore, player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(store, player) as T
                }
            }
        }
    }
}

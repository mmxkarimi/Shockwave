package ir.mmxkarimi.shockwave.ui.nav

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mmxkarimi.shockwave.domain.model.PlaybackState
import ir.mmxkarimi.shockwave.domain.repository.PlayerController

/** Shell state: the selected tab and the playback snapshot that drives the mini-player. */
class ShellViewModel(private val player: PlayerController) : ViewModel() {
    private val tabLiveData = MutableLiveData(ScreenNavigator.Tab.HOME)

    fun getPlayback(): LiveData<PlaybackState> = player.state

    fun getTab(): LiveData<ScreenNavigator.Tab> = tabLiveData

    fun selectTab(t: ScreenNavigator.Tab) {
        if (tabLiveData.value != t) {
            tabLiveData.value = t
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun next() {
        player.next()
    }

    fun previous() {
        player.previous()
    }

    fun pause() {
        player.pause()
    }

    companion object {
        @JvmStatic
        fun factory(player: PlayerController): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShellViewModel(player) as T
                }
            }
        }
    }
}

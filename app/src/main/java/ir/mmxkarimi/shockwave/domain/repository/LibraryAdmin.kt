package ir.mmxkarimi.shockwave.domain.repository

import androidx.lifecycle.LiveData
import ir.mmxkarimi.shockwave.domain.model.ScanResult

/**
 * Library maintenance the screens need beyond [LibraryRepository]: scanning the device and the remaining
 * playlist edits. Fire-and-forget like the repository's writes; results arrive through the repository's LiveData.
 * Obtain it from `AppContainer.libraryAdmin()`.
 */
interface LibraryAdmin {

    /** True while a scan runs (progress indicator). */
    fun isScanning(): LiveData<Boolean>

    /** Manual rescan. [callback] (may be null) gets the result on the main thread; FAILED if one is already running. */
    fun rescan(callback: Callback<ScanResult>?)

    fun renamePlaylist(playlistId: String, name: String)

    fun deletePlaylist(playlistId: String)

    fun deleteTrack(trackId: String)

    fun removeFromPlaylist(playlistId: String, trackId: String)

    /** Moves the member at index [from] to index [to] (indices into the playlist's current order). */
    fun moveInPlaylist(playlistId: String, from: Int, to: Int)
}

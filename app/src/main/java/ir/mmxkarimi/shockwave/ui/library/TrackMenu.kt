package ir.mmxkarimi.shockwave.ui.library

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.LibraryRepository
import ir.mmxkarimi.shockwave.domain.repository.PlayerController
import ir.mmxkarimi.shockwave.ui.nav.Screens

/** Overflow menu for a track row: play next, add to queue, like, go to album / artist. */
object TrackMenu {
    private const val PLAY_NEXT = 1
    private const val ADD_QUEUE = 2
    private const val LIKE = 3
    private const val ADD_PLAYLIST = 4
    private const val DELETE = 5

    @JvmStatic
    fun show(anchor: View, track: Track) {
        val player: PlayerController = Screens.container(anchor.context).player
        val library: LibraryRepository = Screens.container(anchor.context).library
        val menu = PopupMenu(anchor.context, anchor)
        menu.menu.add(0, PLAY_NEXT, 0, R.string.library_menu_play_next)
        menu.menu.add(0, ADD_QUEUE, 1, R.string.library_menu_add_queue)
        menu.menu.add(
            0,
            LIKE,
            2,
            if (track.liked) R.string.library_menu_unlike else R.string.library_menu_like
        )
        menu.menu.add(0, ADD_PLAYLIST, 3, R.string.library_menu_add_playlist)
        menu.menu.add(0, DELETE, 4, R.string.library_menu_delete_track)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                PLAY_NEXT -> player.playNext(track)
                ADD_QUEUE -> player.addToQueue(track)
                LIKE -> library.toggleLike(track.id)
                ADD_PLAYLIST -> pickPlaylist(anchor.context, library, track)
                DELETE -> confirmDeleteTrack(anchor.context, track)
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        menu.show()
    }

    private fun confirmDeleteTrack(context: Context, track: Track) {
        AlertDialog.Builder(context)
            .setTitle(R.string.library_delete_track_title)
            .setMessage(R.string.library_delete_track_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                Screens.container(context).libraryAdmin().deleteTrack(track.id)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /** Lists the playlists (last row: a new one) and adds [track] to the chosen one. */
    private fun pickPlaylist(context: Context, library: LibraryRepository, track: Track) {
        val playlistsLiveData = library.getPlaylists()
        playlistsLiveData.observeForever(object : Observer<List<Playlist>> {
            override fun onChanged(value: List<Playlist>) {
                playlistsLiveData.removeObserver(this)
                val list = value ?: emptyList()
                val names = Array(list.size + 1) { "" }
                for (i in list.indices) {
                    names[i] = list[i].name
                }
                names[list.size] = context.getString(R.string.library_new_playlist)
                AlertDialog.Builder(context)
                    .setTitle(R.string.library_menu_add_playlist)
                    .setItems(names) { _, which ->
                        if (which < list.size) {
                            library.addToPlaylist(list[which].id, track.id)
                        } else {
                            createAndAdd(context, library, track)
                        }
                    }
                    .show()
            }
        })
    }

    private fun createAndAdd(context: Context, library: LibraryRepository, track: Track) {
        val name = EditText(context)
        name.isSingleLine = true
        name.setText(R.string.library_new_playlist)
        AlertDialog.Builder(context)
            .setTitle(R.string.library_new_playlist)
            .setView(name)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = name.text.toString().trim()
                if (text.isNotEmpty()) {
                    library.createPlaylist(text) { p ->
                        if (p != null) {
                            library.addToPlaylist(p.id, track.id)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

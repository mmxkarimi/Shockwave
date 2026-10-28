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
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                PLAY_NEXT -> player.playNext(track)
                ADD_QUEUE -> player.addToQueue(track)
                LIKE -> library.toggleLike(track.id)
                ADD_PLAYLIST -> pickPlaylist(anchor.context, library, track)
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        menu.show()
    }

    /** Lists the playlists (last row: a new one) and adds [track] to the chosen one. */
    private fun pickPlaylist(context: Context, library: LibraryRepository, track: Track) {
        library.playlists.observeForever(object : Observer<List<Playlist>> {
            override fun onChanged(value: List<Playlist>) {
                library.playlists.removeObserver(this)
                val playlists = value ?: emptyList()
                val names = Array(playlists.size + 1) { "" }
                for (i in playlists.indices) {
                    names[i] = playlists[i].name
                }
                names[playlists.size] = context.getString(R.string.library_playlist_create)
                AlertDialog.Builder(context)
                    .setTitle(R.string.library_menu_add_playlist)
                    .setItems(names) { _, which ->
                        if (which < playlists.size) {
                            library.addToPlaylist(playlists[which].id, track.id)
                        } else {
                            newPlaylist(context, library, track)
                        }
                    }
                    .show()
            }
        })
    }

    private fun newPlaylist(context: Context, library: LibraryRepository, track: Track) {
        val input = EditText(context)
        input.setHint(R.string.library_playlist_name_hint)
        AlertDialog.Builder(context)
            .setTitle(R.string.library_playlist_create)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    library.createPlaylist(name, track)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

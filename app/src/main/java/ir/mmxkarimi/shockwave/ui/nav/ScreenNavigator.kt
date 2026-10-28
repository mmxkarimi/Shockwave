package ir.mmxkarimi.shockwave.ui.nav

import android.content.Context
import android.content.ContextWrapper

/** In-app navigation between library screens. */
interface ScreenNavigator {

    /** What a track-list screen shows. */
    enum class TrackList { ALBUM, ARTIST, PLAYLIST, LIKED, HISTORY, RECENT }

    /** What a collection grid shows. */
    enum class Collection { ALBUMS, ARTISTS, PLAYLISTS }

    /** [id] is the album/artist/playlist id, or null for LIKED / HISTORY / RECENT. */
    fun openTrackList(kind: TrackList, id: String?)

    fun openCollection(kind: Collection)

    /** Switches to a top-level tab (clears the back stack). */
    fun openTab(tab: Tab)

    enum class Tab { HOME, SEARCH, LIBRARY, SETTINGS }

    companion object {
        @JvmStatic
        fun from(context: Context?): ScreenNavigator? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is ScreenNavigator) return ctx
                ctx = ctx.baseContext
            }
            return null
        }
    }
}

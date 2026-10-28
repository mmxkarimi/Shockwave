package ir.mmxkarimi.shockwave.ui.nav

import android.content.Context
import android.content.ContextWrapper

/**
 * Navigation for full player and queue.
 */
interface PlayerNavigator {

    /** Opens the full-screen player over the current screen. */
    fun openFullPlayer()

    /** Opens the queue sheet. */
    fun openQueue()

    /** Closes the full player (and the queue) if open. */
    fun closeFullPlayer()

    companion object {
        @JvmStatic
        fun from(context: Context?): PlayerNavigator? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is PlayerNavigator) return ctx
                ctx = ctx.baseContext
            }
            return null
        }
    }
}

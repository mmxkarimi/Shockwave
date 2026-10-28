package ir.mmxkarimi.shockwave.playback

import java.util.Collections
import java.util.Random

/**
 * "Shuffle the queue" with a one-step undo, pure Java. [shuffle] keeps everything up to and including the
 * current item in place, shuffles the rest and remembers the previous order; [undo] hands that exact order
 * back. Any other queue edit must call [clear] (the snapshot would no longer match).
 */
internal class QueueShuffler<T> {
    private var snapshot: List<T>? = null

    fun canUndo(): Boolean {
        return snapshot != null
    }

    fun clear() {
        snapshot = null
    }

    /** New order, or null (and no snapshot) when fewer than two items follow `currentIndex`. */
    fun shuffle(queue: List<T>, currentIndex: Int, random: Random): List<T>? {
        if (currentIndex < 0 || queue.size - currentIndex - 1 < 2) return null
        snapshot = ArrayList(queue)
        val result = ArrayList<T>(queue)
        Collections.shuffle(result.subList(currentIndex + 1, result.size), random)
        return result
    }

    /** The order from before the shuffle (and forgets it), or null when there is nothing to undo. */
    fun undo(): List<T>? {
        val previous = snapshot
        snapshot = null
        return previous
    }
}
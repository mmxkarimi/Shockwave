package ir.mmxkarimi.shockwave.data.scanner

/**
 * Decides, row by row while the MediaStore cursor streams past, which tracks need writing, and afterwards which stored
 * tracks vanished. Keeps only ids in memory, never the scanned rows.
 */
class IncrementalDiff(storedModified: Map<String, Long>) {
    enum class Change { UNCHANGED, ADDED, UPDATED }

    private val storedModified: Map<String, Long> = storedModified
    private val missing: MutableSet<String> = HashSet(storedModified.keys)

    fun visit(id: String, modifiedMs: Long): Change {
        missing.remove(id)
        val stored = storedModified[id] ?: return Change.ADDED
        return if (stored == modifiedMs) Change.UNCHANGED else Change.UPDATED
    }

    /** Stored ids never visited: files deleted, moved out of the included folders, or filtered away. */
    fun missingIds(): Set<String> = missing
}

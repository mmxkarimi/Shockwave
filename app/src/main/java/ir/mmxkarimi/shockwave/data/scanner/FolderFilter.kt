package ir.mmxkarimi.shockwave.data.scanner

/**
 * Include / exclude folder rules from the settings (absolute directory paths). An empty include set means "everything";
 * an exclude always wins. Matching is by directory prefix, so `/sdcard/Music` does not match `/sdcard/Music2`.
 */
class FolderFilter(
    private val include: Set<String>,
    private val exclude: Set<String>,
) {

    /** @param filePath absolute path of the audio file; null (unknown) is accepted unless includes are set */
    fun accepts(filePath: String?): Boolean {
        if (filePath != null) {
            for (dir in exclude) if (isUnder(filePath, dir)) return false
        }
        if (include.isEmpty()) return true
        if (filePath == null) return false
        for (dir in include) if (isUnder(filePath, dir)) return true
        return false
    }

    companion object {
        @JvmStatic private fun isUnder(filePath: String, dir: String): Boolean {
            val prefix = if (dir.endsWith("/")) dir else "$dir/"
            return filePath.startsWith(prefix)
        }
    }
}

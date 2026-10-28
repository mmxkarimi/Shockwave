package ir.mmxkarimi.shockwave.data.lyrics

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

/** Reads the iTunes-style `moov/udta/meta/ilst/©lyr` atom of an M4A/MP4 file (plain text). */
internal class Mp4LyricsReader private constructor() {

    companion object {
        private const val MAX_ATOM = 1024 * 1024
        private val PATH = arrayOf("moov", "udta", "meta", "ilst", "©lyr", "data")

        @JvmStatic internal fun read(f: RandomAccessFile): String? {
            val data = find(f, 0, f.length(), 0)
            if (data == null || data.size <= 8) return null // data atom body: 4 bytes type/flags + 4 bytes locale + text
            val s = String(data, 8, data.size - 8, StandardCharsets.UTF_8).trim()
            return if (s.isEmpty()) null else s
        }

        /** Body of the atom at `PATH[depth]` inside [start, end), or null. */
        @JvmStatic private fun find(f: RandomAccessFile, start: Long, end: Long, depth: Int): ByteArray? {
            val h = ByteArray(8)
            var pos = start
            while (pos + 8 <= end) {
                f.seek(pos)
                f.readFully(h)
                var size = ((h[0].toLong() and 0xFFL) shl 24) or ((h[1].toLong() and 0xFFL) shl 16) or
                    ((h[2].toLong() and 0xFFL) shl 8) or (h[3].toLong() and 0xFFL)
                val type = String(h, 4, 4, StandardCharsets.ISO_8859_1)
                var header = 8L
                if (size == 1L) { // 64-bit size follows
                    val big = ByteArray(8)
                    f.readFully(big)
                    size = 0
                    for (x in big) size = (size shl 8) or (x.toLong() and 0xFFL)
                    header = 16
                } else if (size == 0L) {
                    size = end - pos
                }
                if (size < header || pos + size > end) return null
                if (type == PATH[depth]) {
                    val bodyStart = pos + header + (if (type == "meta") 4 else 0) // meta is a full box
                    if (depth == PATH.size - 1) {
                        val len = pos + size - bodyStart
                        if (len > MAX_ATOM) return null
                        val body = ByteArray(len.toInt())
                        f.seek(bodyStart)
                        f.readFully(body)
                        return body
                    }
                    return find(f, bodyStart, pos + size, depth + 1)
                }
                pos += size
            }
            return null
        }
    }
}

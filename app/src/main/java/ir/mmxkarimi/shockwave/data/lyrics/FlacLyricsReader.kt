package ir.mmxkarimi.shockwave.data.lyrics

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Reads the LYRICS / UNSYNCEDLYRICS / SYNCEDLYRICS Vorbis comment of a FLAC file (synced text wins). */
internal class FlacLyricsReader private constructor() {

    companion object {
        private const val MAX_BLOCK = 1024 * 1024

        @JvmStatic internal fun read(f: RandomAccessFile): String? {
            val magic = ByteArray(4)
            f.seek(0)
            if (f.read(magic) != 4 || magic[0].toInt() != 'f'.code || magic[1].toInt() != 'L'.code ||
                magic[2].toInt() != 'a'.code || magic[3].toInt() != 'C'.code
            ) return null
            var pos = 4L
            var last = false
            val h = ByteArray(4)
            while (!last) {
                f.seek(pos)
                f.readFully(h)
                last = (h[0].toInt() and 0x80) != 0
                val type = h[0].toInt() and 0x7F
                val len = ((h[1].toInt() and 0xFF) shl 16) or ((h[2].toInt() and 0xFF) shl 8) or (h[3].toInt() and 0xFF)
                if (type == 4) return if (len > MAX_BLOCK) null else comments(readBlock(f, len))
                pos += 4 + len
            }
            return null
        }

        @JvmStatic private fun readBlock(f: RandomAccessFile, len: Int): ByteArray {
            val b = ByteArray(len)
            f.readFully(b)
            return b
        }

        @JvmStatic internal fun comments(b: ByteArray): String? {
            var plain: String? = null
            var synced: String? = null
            val vendor = le(b, 0)
            if (vendor < 0 || 4 + vendor + 4 > b.size) return null
            var pos = (4 + vendor).toInt() // skip vendor string
            val count = le(b, pos)
            pos += 4
            var i = 0L
            while (i < count && pos + 4 <= b.size) {
                i++
                val len = le(b, pos)
                pos += 4
                if (len < 0 || pos + len > b.size) break
                val kv = String(b, pos, len.toInt(), StandardCharsets.UTF_8)
                pos += len.toInt()
                val eq = kv.indexOf('=')
                if (eq <= 0) continue
                val key = kv.substring(0, eq).uppercase(Locale.ROOT)
                val value = kv.substring(eq + 1)
                if (value.trim().isEmpty()) continue
                if (key == "SYNCEDLYRICS" && synced == null) synced = value
                else if ((key == "LYRICS" || key == "UNSYNCEDLYRICS") && plain == null) plain = value
            }
            return synced ?: plain
        }

        @JvmStatic private fun le(b: ByteArray, off: Int): Long {
            if (off < 0 || off + 4 > b.size) return -1
            return (b[off].toLong() and 0xFFL) or ((b[off + 1].toLong() and 0xFFL) shl 8) or
                ((b[off + 2].toLong() and 0xFFL) shl 16) or ((b[off + 3].toLong() and 0xFFL) shl 24)
        }
    }
}

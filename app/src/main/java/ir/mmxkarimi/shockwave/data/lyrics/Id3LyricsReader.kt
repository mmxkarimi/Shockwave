package ir.mmxkarimi.shockwave.data.lyrics

import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Reads lyrics from an ID3v2.2/2.3/2.4 tag at the start of a file: synchronised lyrics (SYLT, converted to LRC text,
 * millisecond timestamps only) win over unsynchronised lyrics (USLT). Walks frame headers with seeks, so large
 * artwork frames are skipped, not loaded. Not handled: tag-level unsynchronisation, compressed/encrypted frames.
 */
internal class Id3LyricsReader private constructor() {

    companion object {
        private const val MAX_FRAME = 512 * 1024

        @JvmStatic internal fun read(f: RandomAccessFile): String? {
            val h = ByteArray(10)
            f.seek(0)
            if (f.read(h) != 10 || h[0].toInt() != 'I'.code || h[1].toInt() != 'D'.code || h[2].toInt() != '3'.code) return null
            val ver = h[3].toInt() and 0xFF
            if (ver < 2 || ver > 4) return null
            val end = 10 + synchsafe(h, 6)
            var pos = 10L
            if (ver >= 3 && (h[5].toInt() and 0x40) != 0) { // extended header
                val e = ByteArray(4)
                f.readFully(e)
                pos += if (ver == 4) synchsafe(e, 0) else 4 + be(e, 0, 4)
            }
            val idLen = if (ver == 2) 3 else 4
            val hdr = if (ver == 2) 6 else 10
            var uslt: String? = null
            var sylt: String? = null
            val fh = ByteArray(hdr)
            while (pos + hdr <= end) {
                f.seek(pos)
                f.readFully(fh)
                if (fh[0].toInt() == 0) break // padding
                val id = String(fh, 0, idLen, StandardCharsets.ISO_8859_1)
                val size = if (ver == 2) be(fh, 3, 3) else if (ver == 4) synchsafe(fh, 4) else be(fh, 4, 4)
                if (size <= 0 || pos + hdr + size > end) break
                val isUslt = id == "USLT" || id == "ULT"
                val isSylt = id == "SYLT" || id == "SLT"
                val flags2 = if (ver == 2) 0 else fh[9].toInt() and 0xFF
                val unreadable = (flags2 and 0x0C) != 0 // compressed or encrypted
                if ((isUslt && uslt == null || isSylt && sylt == null) && size <= MAX_FRAME && !unreadable) {
                    val data = ByteArray(size.toInt())
                    f.readFully(data)
                    val from = if (ver == 4 && (flags2 and 0x01) != 0) 4 else 0 // data length indicator
                    val text = if (isUslt) uslt(data, from) else sylt(data, from)
                    if (isUslt) uslt = text else sylt = text
                }
                pos += hdr + size
            }
            return sylt ?: uslt
        }

        @JvmStatic internal fun uslt(d: ByteArray, from: Int): String? {
            if (d.size < from + 5) return null
            val enc = d[from].toInt()
            val textStart = afterTerminator(d, from + 4, enc)
            val s = decode(d, textStart, d.size, enc).replace("\u0000", "").trim()
            return if (s.isEmpty()) null else s
        }

        /** SYLT -> LRC text. Entries are lines when none contains a newline, otherwise a newline starts the next line. */
        @JvmStatic internal fun sylt(d: ByteArray, from: Int): String? {
            if (d.size < from + 7 || d[from + 4] != 2.toByte()) return null // only millisecond timestamps
            val enc = d[from].toInt()
            var pos = afterTerminator(d, from + 6, enc)
            val times = mutableListOf<Long>()
            val texts = mutableListOf<String>()
            var anyNewline = false
            while (pos < d.size) {
                val term = indexOfTerminator(d, pos, enc)
                if (term < 0 || term + terminatorLength(enc) + 4 > d.size) break
                val text = decode(d, pos, term, enc)
                val t = term + terminatorLength(enc)
                times.add(be(d, t, 4))
                texts.add(text)
                anyNewline = anyNewline || text.indexOf('\n') >= 0
                pos = t + 4
            }
            val out = StringBuilder()
            val line = StringBuilder()
            var lineTime = -1L
            for (i in texts.indices) {
                val t = texts[i]
                val breaksBefore = !anyNewline || t.startsWith("\n")
                if (breaksBefore && line.isNotEmpty()) {
                    appendLrc(out, lineTime, line)
                    line.setLength(0)
                }
                if (line.isEmpty()) lineTime = times[i]
                line.append(t.replace("\n", "").replace("\r", ""))
                if (anyNewline && t.endsWith("\n")) {
                    appendLrc(out, lineTime, line)
                    line.setLength(0)
                }
            }
            if (line.isNotEmpty()) appendLrc(out, lineTime, line)
            return if (out.isEmpty()) null else out.toString()
        }

        @JvmStatic private fun appendLrc(out: StringBuilder, ms: Long, text: CharSequence) {
            out.append(String.format(Locale.ROOT, "[%02d:%02d.%02d]", ms / 60000, ms / 1000 % 60, ms % 1000 / 10))
                .append(text).append('\n')
        }

        @JvmStatic private fun terminatorLength(enc: Int): Int {
            return if (enc == 1 || enc == 2) 2 else 1
        }

        @JvmStatic private fun indexOfTerminator(d: ByteArray, from: Int, enc: Int): Int {
            val n = terminatorLength(enc)
            var i = from
            while (i + n <= d.size) {
                if (d[i].toInt() == 0 && (n == 1 || d[i + 1].toInt() == 0)) return i
                i += n
            }
            return -1
        }

        @JvmStatic private fun afterTerminator(d: ByteArray, from: Int, enc: Int): Int {
            val i = indexOfTerminator(d, from, enc)
            return if (i < 0) d.size else i + terminatorLength(enc)
        }

        @JvmStatic internal fun decode(d: ByteArray, from: Int, to: Int, enc: Int): String {
            if (to <= from) return ""
            return when (enc) {
                1 -> // UTF-16 with BOM
                    if (to - from >= 2 && (d[from].toInt() and 0xFF) == 0xFF && (d[from + 1].toInt() and 0xFF) == 0xFE) {
                        String(d, from + 2, to - from - 2, StandardCharsets.UTF_16LE)
                    } else if (to - from >= 2 && (d[from].toInt() and 0xFF) == 0xFE && (d[from + 1].toInt() and 0xFF) == 0xFF) {
                        String(d, from + 2, to - from - 2, StandardCharsets.UTF_16BE)
                    } else {
                        String(d, from, to - from, StandardCharsets.UTF_16LE)
                    }
                2 -> String(d, from, to - from, StandardCharsets.UTF_16BE)
                3 -> String(d, from, to - from, StandardCharsets.UTF_8)
                else -> String(d, from, to - from, StandardCharsets.ISO_8859_1)
            }
        }

        @JvmStatic private fun synchsafe(b: ByteArray, off: Int): Long {
            return ((b[off].toLong() and 0x7FL) shl 21) or ((b[off + 1].toLong() and 0x7FL) shl 14) or
                ((b[off + 2].toLong() and 0x7FL) shl 7) or (b[off + 3].toLong() and 0x7FL)
        }

        @JvmStatic private fun be(b: ByteArray, off: Int, n: Int): Long {
            var v = 0L
            for (i in 0 until n) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
            return v
        }
    }
}

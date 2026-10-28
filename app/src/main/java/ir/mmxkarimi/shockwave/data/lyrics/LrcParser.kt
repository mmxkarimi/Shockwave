package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.domain.model.LyricLine
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import java.util.Locale
import java.util.regex.Pattern

/**
 * Tolerant LRC parser (pure Java, no Android types).
 *
 * Handles `[mm:ss.xx]`, `[mm:ss.xxx]`, `[mm:ss]`, `[mm:ss:xx]`, `[h:mm:ss.xx]`, several
 * timestamps on one line, `[offset:+/-ms]` (positive = lyrics appear earlier), header tags
 * (`ti ar al au by length re ve tool lang`), enhanced word tags `<mm:ss.xx>` (removed: the domain model
 * has no word timings), Persian/Arabic-Indic digits in timestamps, invisible bidi marks, BOM, LF/CRLF/CR.
 * Output is sorted by time (stable, so equal timestamps keep file order; exact duplicates are dropped).
 * Without any timestamp the text becomes unsynced lines.
 */
class LrcParser private constructor() {

    companion object {
        private val WORD_TAG = Pattern.compile("<\\s*\\d+\\s*:\\s*\\d+(?:\\s*[.:,]\\s*\\d+)?\\s*>")
        private val SHORT_TS = Pattern.compile("(\\d{1,5}):(\\d{1,2})(?:[.:,](\\d{1,3}))?")
        private val LONG_TS = Pattern.compile("(\\d{1,4}):(\\d{1,2}):(\\d{1,2})[.,](\\d{1,3})")
        private val META = Pattern.compile("([A-Za-z]+)\\s*:(.*)", Pattern.DOTALL)
        private val META_KEYS = setOf(
            "ti", "ar", "al", "au", "by", "offset", "length", "re", "ve", "tool", "lang",
        )

        /** @return the parsed lyrics, or null when the text holds no lyric line at all */
        @JvmStatic fun parse(raw: String?): ParsedLrc? {
            if (raw == null) return null
            val timed = mutableListOf<LyricLine>()
            val plain = mutableListOf<String>()
            val meta = LinkedHashMap<String, String>()

            for (rawLine in Pattern.compile("\r\n|\n|\r").split(raw, -1)) {
                var i = skipInvisible(rawLine, 0)
                val start = i
                val times = mutableListOf<Long>()
                var isMeta = false
                while (i < rawLine.length && rawLine[i] == '[') {
                    val close = rawLine.indexOf(']', i)
                    if (close < 0) break
                    val inner = rawLine.substring(i + 1, close)
                    val ts = parseTimestamp(inner)
                    if (ts != null) {
                        times.add(ts)
                        i = skipInvisible(rawLine, close + 1)
                        continue
                    }
                    val m = if (times.isEmpty()) META.matcher(inner) else null
                    if (m != null && m.matches() && META_KEYS.contains(m.group(1)!!.lowercase(Locale.ROOT))) {
                        meta[m.group(1)!!.lowercase(Locale.ROOT)] = m.group(2)!!.trim()
                        isMeta = true
                    }
                    break
                }
                if (isMeta) continue
                if (times.isEmpty()) {
                    plain.add(clean(rawLine.substring(start)))
                } else {
                    val text = clean(rawLine.substring(i))
                    for (t in times) timed.add(LyricLine(t, text))
                }
            }

            val offset = parseOffset(meta["offset"])
            if (timed.isNotEmpty()) {
                val lines = syncedLines(timed, offset)
                if (!hasText(lines)) return null
                // "[00:00.00]" on every line carries no timing: treat as plain text.
                val allSame = lines.size > 1 && lines.all { it.timeMs == lines[0].timeMs }
                if (!allSame) return ParsedLrc(lines, true, meta, offset)
                plain.clear()
                for (l in lines) plain.add(l.text)
            }
            return plainLines(plain, meta, offset)
        }

        /** Parses `raw` and wraps it as [Lyrics]; null if empty. */
        @JvmStatic fun toLyrics(trackId: String, raw: String?, source: String?): Lyrics? {
            val p = parse(raw)
            return p?.toLyrics(trackId, source)
        }

        @JvmStatic private fun syncedLines(timed: List<LyricLine>, offset: Long): List<LyricLine> {
            val shifted = ArrayList<LyricLine>(timed.size)
            for (l in timed) shifted.add(LyricLine(Math.max(0L, l.timeMs - offset), l.text))
            shifted.sortWith(compareBy { it.timeMs }) // stable
            val out = ArrayList<LyricLine>(shifted.size)
            val seenAtTime = HashSet<LyricLine>()
            var seenTime = -1L
            for (l in shifted) {
                if (l.timeMs != seenTime) {
                    seenAtTime.clear()
                    seenTime = l.timeMs
                }
                if (!seenAtTime.add(l)) continue // exact duplicate
                val prev = if (out.isEmpty()) null else out[out.size - 1]
                if (l.text.isEmpty() && prev != null && prev.text.isEmpty()) continue // collapse blank runs
                out.add(l)
            }
            return out
        }

        @JvmStatic private fun plainLines(texts: List<String>, meta: Map<String, String>, offset: Long): ParsedLrc? {
            val out = mutableListOf<LyricLine>()
            for (t in texts) {
                if (t.isEmpty() && (out.isEmpty() || out[out.size - 1].text.isEmpty())) continue
                out.add(LyricLine(LyricLine.UNSYNCED, t))
            }
            while (out.isNotEmpty() && out[out.size - 1].text.isEmpty()) out.removeAt(out.size - 1)
            return if (out.isEmpty()) null else ParsedLrc(out, false, meta, offset)
        }

        @JvmStatic private fun hasText(lines: List<LyricLine>): Boolean {
            for (l in lines) if (l.text.isNotEmpty()) return true
            return false
        }

        /** Milliseconds for the inside of a `[...]` tag, or null if it is not a timestamp. */
        @JvmStatic internal fun parseTimestamp(inner: String): Long? {
            val s = normalizeDigits(inner.trim())
            var m = LONG_TS.matcher(s)
            if (m.matches()) {
                val h = toLong(m.group(1)!!)
                return ((h * 60 + toLong(m.group(2)!!)) * 60 + toLong(m.group(3)!!)) * 1000 + fractionMs(m.group(4)!!)
            }
            m = SHORT_TS.matcher(s)
            if (m.matches()) {
                val ms = (toLong(m.group(1)!!) * 60 + toLong(m.group(2)!!)) * 1000
                return if (m.group(3) == null) ms else ms + fractionMs(m.group(3)!!)
            }
            return null
        }

        @JvmStatic private fun toLong(digits: String): Long = digits.toLong()

        /** "5" -> 500, "50" -> 500, "05" -> 50, "500" -> 500. */
        @JvmStatic private fun fractionMs(frac: String): Long {
            val sb = StringBuilder(frac)
            while (sb.length < 3) sb.append('0')
            return sb.substring(0, 3).toLong()
        }

        @JvmStatic private fun parseOffset(value: String?): Long {
            if (value == null) return 0
            return try {
                val v = normalizeDigits(value.trim())
                (if (v.startsWith("+")) v.substring(1) else v).toLong()
            } catch (e: NumberFormatException) {
                0
            }
        }

        /** Maps Persian/Arabic-Indic digits, the Arabic decimal separator and the full-width colon to ASCII. */
        @JvmStatic private fun normalizeDigits(s: String): String {
            var sb: StringBuilder? = null
            for (i in s.indices) {
                val c = s[i]
                if (c.code < 128) {
                    sb?.append(c)
                    continue
                }
                if (sb == null) sb = StringBuilder(s.length).append(s, 0, i)
                val d = Character.digit(c, 10)
                sb.append(
                    when {
                        d >= 0 -> ('0' + d).toChar()
                        c == '\u066B' -> '.'
                        c == '\uFF1A' -> ':'
                        else -> c
                    },
                )
            }
            return sb?.toString() ?: s
        }

        /** Removes word tags and collapses whitespace; keeps all other characters (Persian etc.) intact. */
        @JvmStatic private fun clean(s: String): String {
            var t = s
            if (t.indexOf('<') >= 0) t = WORD_TAG.matcher(t).replaceAll("")
            return t.replace(Regex("[ \\t]{2,}"), " ").trim()
        }

        @JvmStatic private fun skipInvisible(s: String, from: Int): Int {
            var i = from
            while (i < s.length && isInvisible(s[i])) i++
            return i
        }

        @JvmStatic private fun isInvisible(c: Char): Boolean {
            return c == ' ' || c == '\t' || c == '\uFEFF' || c == '\u200B' || c == '\u200E' || c == '\u200F' ||
                c == '\u061C' || c in '\u202A'..'\u202E' || c in '\u2066'..'\u2069'
        }
    }
}

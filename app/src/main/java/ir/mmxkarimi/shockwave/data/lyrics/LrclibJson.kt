package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LyricsSource.Found
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Maps LRCLIB JSON (`/api/get` object, `/api/search` array) and picks the best match for a track. */
internal class LrclibJson private constructor() {

    /** One LRCLIB record; `synced`/`plain` are null when absent or blank. */
    internal data class Candidate(
        @get:JvmName("durationSec") val durationSec: Double,
        @get:JvmName("instrumental") val instrumental: Boolean,
        @get:JvmName("synced") val synced: String?,
        @get:JvmName("plain") val plain: String?,
    ) {

        internal fun hasLyrics(): Boolean = synced != null || plain != null

        /** Unknown track duration (<= 0) accepts everything. */
        internal fun durationMatches(trackSec: Int): Boolean {
            return trackSec <= 0 || (durationSec > 0 && Math.abs(durationSec - trackSec) <= DURATION_TOLERANCE_SEC)
        }
    }

    companion object {
        /** Largest accepted difference between the track's duration and a result's, in seconds. */
        internal const val DURATION_TOLERANCE_SEC = 3.0

        /** @throws IOException when the body is not the expected JSON */
        @JvmStatic internal fun parseObject(json: String): Candidate {
            return try {
                candidate(JSONObject(json))
            } catch (e: JSONException) {
                throw IOException("bad LRCLIB response", e)
            }
        }

        @JvmStatic internal fun parseArray(json: String): List<Candidate> {
            return try {
                val array = JSONArray(json)
                val out = ArrayList<Candidate>(array.length())
                for (i in 0 until array.length()) out.add(candidate(array.getJSONObject(i)))
                out
            } catch (e: JSONException) {
                throw IOException("bad LRCLIB response", e)
            }
        }

        /** Synced lyrics first, then closest duration; null when nothing has lyrics within the tolerance. */
        @JvmStatic internal fun pickBest(candidates: List<Candidate>, trackSec: Int): Candidate? {
            var best: Candidate? = null
            for (c in candidates) {
                if (c.instrumental || !c.hasLyrics() || !c.durationMatches(trackSec)) continue
                if (best == null || better(c, best, trackSec)) best = c
            }
            return best
        }

        @JvmStatic private fun better(a: Candidate, b: Candidate, trackSec: Int): Boolean {
            if ((a.synced != null) != (b.synced != null)) return a.synced != null
            return Math.abs(a.durationSec - trackSec) < Math.abs(b.durationSec - trackSec)
        }

        /** The lyrics text of a candidate (synced preferred), or null. */
        @JvmStatic internal fun toFound(c: Candidate?): Found? {
            if (c == null || !c.hasLyrics()) return null
            return Found(c.synced ?: c.plain!!, "LRCLIB")
        }

        @JvmStatic private fun candidate(o: JSONObject): Candidate {
            return Candidate(
                o.optDouble("duration", -1.0),
                o.optBoolean("instrumental", false),
                text(o, "syncedLyrics"),
                text(o, "plainLyrics"),
            )
        }

        @JvmStatic private fun text(o: JSONObject, key: String): String? {
            if (o.isNull(key)) return null // optString would turn JSON null into the string "null"
            val s = o.optString(key, "")
            return if (s.trim().isEmpty()) null else s
        }
    }
}

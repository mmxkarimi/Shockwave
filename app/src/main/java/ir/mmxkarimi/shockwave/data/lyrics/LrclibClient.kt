package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LrclibJson.Candidate
import ir.mmxkarimi.shockwave.data.lyrics.LyricsSource.Found
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

/**
 * LRCLIB (https://lrclib.net) lookup: exact `/api/get` first (needs title, artist, album and duration), then
 * `/api/search`; synced lyrics are preferred and the result must match the track's duration within 3 s.
 * Sends only title / artist / album / duration. At most two requests per call; a failed /get falls through to /search.
 */
class LrclibClient internal constructor(
    private val http: HttpFetcher,
    private val baseUrl: String,
) : LyricsSource {

    constructor(http: HttpFetcher) : this(http, DEFAULT_BASE_URL)

    override fun load(track: Track, cancel: CancelToken): Found? {
        val title = track.title
        if (title == null || title.trim().isEmpty()) return null
        val artist = known(track.artistName)
        val album = known(track.albumTitle)
        val sec = Math.round(track.durationMs / 1000.0).toInt()

        if (artist != null && album != null && sec > 0) {
            try {
                val body = http.get(
                    baseUrl + "/get?" + param("track_name", title) + "&" + param("artist_name", artist) +
                        "&" + param("album_name", album) + "&duration=" + sec,
                    cancel,
                )
                val exact = if (body == null) null else LrclibJson.parseObject(body)
                if (exact != null && exact.instrumental) return null
                if (exact != null && exact.durationMatches(sec)) {
                    val found = LrclibJson.toFound(exact)
                    if (found != null) return found
                }
            } catch (e: IOException) {
                // /get is the busier endpoint (503s are common); /search may still answer, so only fail if it fails too
            }
        }
        if (cancel.isCancelled()) return null
        val query = "/search?" + param("track_name", title) +
            (if (artist == null) "" else "&" + param("artist_name", artist))
        val body = http.get(baseUrl + query, cancel)
        return body?.let { LrclibJson.toFound(LrclibJson.pickBest(LrclibJson.parseArray(it), sec)) }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://lrclib.net/api"

        /** MediaStore reports missing tags as "&lt;unknown&gt;". */
        @JvmStatic private fun known(s: String?): String? {
            if (s == null) return null
            val t = s.trim()
            return if (t.isEmpty() || t.lowercase(Locale.ROOT) == "<unknown>") null else t
        }

        @JvmStatic private fun param(key: String, value: String): String {
            return try {
                key + "=" + URLEncoder.encode(value, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw IllegalStateException(e) // UTF-8 is always supported
            }
        }
    }
}

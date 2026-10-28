package ir.mmxkarimi.shockwave.data.lyrics

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** [HttpFetcher] on the platform [HttpURLConnection]: short timeouts, capped body, cancellable. */
class UrlConnectionFetcher(private val userAgent: String) : HttpFetcher {

    /** @param userAgent LRCLIB asks clients to identify themselves, e.g. `"Shockwave/0.1.0 (https://github.com/mmxkarimi/Shockwave)"` */
    override fun get(url: String, cancel: CancelToken): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        cancel.setOnCancel(conn::disconnect)
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS)
            conn.setReadTimeout(READ_TIMEOUT_MS)
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.getResponseCode()
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code / 100 != 2) throw IOException("HTTP " + code)
            conn.getInputStream().use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BODY) throw IOException("response too large")
                }
                return out.toString(StandardCharsets.UTF_8.name())
            }
        } finally {
            cancel.setOnCancel(null)
            conn.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 8_000
        private const val MAX_BODY = 2 * 1024 * 1024
    }
}

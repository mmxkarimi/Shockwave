package ir.mmxkarimi.shockwave.data.lyrics

/** Minimal HTTP GET so the LRCLIB client can be tested without a network. */
fun interface HttpFetcher {

    /** @return the response body, or null for 404; IOException for any other failure */
    fun get(url: String, cancel: CancelToken): String?
}

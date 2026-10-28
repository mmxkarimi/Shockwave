package ir.mmxkarimi.shockwave.fake

import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.domain.repository.Callback
import ir.mmxkarimi.shockwave.domain.repository.MusicSource
import java.util.Locale

/** Searches [SampleData]; has no playable URIs. Callbacks run inline. */
class FakeMusicSource : MusicSource {
    override fun getId(): String {
        return "fake"
    }

    override fun getDisplayName(): String {
        return "Sample catalog"
    }

    override fun search(query: String, limit: Int, callback: Callback<List<Track>>) {
        val q = query.lowercase(Locale.ROOT)
        val out = ArrayList<Track>()
        for (t in SampleData.TRACKS) {
            if (out.size >= limit) break
            if (t.title.lowercase(Locale.ROOT).contains(q) || t.artistName.lowercase(Locale.ROOT).contains(q)) {
                out.add(t)
            }
        }
        callback.onResult(out)
    }

    override fun resolvePlayableUri(track: Track, callback: Callback<String?>) {
        callback.onResult(null)
    }
}

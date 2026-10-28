package ir.mmxkarimi.shockwave.fake

import ir.mmxkarimi.shockwave.domain.model.Album
import ir.mmxkarimi.shockwave.domain.model.Artist
import ir.mmxkarimi.shockwave.domain.model.LyricLine
import ir.mmxkarimi.shockwave.domain.model.Lyrics
import ir.mmxkarimi.shockwave.domain.model.Playlist
import ir.mmxkarimi.shockwave.domain.model.Track
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Deterministic in-memory catalog for UI work and tests (37 tracks, 7 artists incl. NEFFEX and a Persian one).
 */
class SampleData private constructor() {
    /** Album spec: tracks are "title|seconds" with an optional "|x" suffix for explicit. */
    private data class AlbumSpec(
        @get:JvmName("artist") val artist: String,
        @get:JvmName("title") val title: String,
        @get:JvmName("year") val year: Int,
        @get:JvmName("tracks") val tracks: List<String>,
    )

    companion object {
        private const val DAY = 86_400_000L

        /** Fixed "now" so sorting by addedAtMs is deterministic. */
        const val NOW_MS = 1_790_000_000_000L

        private val SPECS = listOf(
            AlbumSpec(
                "NEFFEX", "Best of Me", 2018, listOf("Best of Me|191", "Grateful|183", "Fight Back|205",
                "Cold|192", "Rise|188", "Careless|176", "Destiny|179", "Never Give Up|198"),
            ),
            AlbumSpec(
                "NEFFEX", "Anxiety", 2020, listOf("Anxiety|187", "Hate|168|x", "Light It Up|181",
                "Dream On|194", "Walk It Off|172"),
            ),
            AlbumSpec(
                "Aurora Lane", "Paper Lanterns", 2023, listOf("Paper Lanterns|214", "Slow Tide|233",
                "Glass Garden|201", "Last Train Home|247", "Honey & Smoke|189"),
            ),
            AlbumSpec(
                "Midnight Atlas", "Northbound", 2022, listOf("Northbound|262", "Static Bloom|218",
                "Cartographer|244", "Neon Rain|207|x"),
            ),
            AlbumSpec("Orchid Hours", "Quiet Rooms", 2022, listOf("Afterglow Drive|229", "Low Orbit|196")),
            AlbumSpec(
                "Kasra", "Concrete Gardens", 2024, listOf("Concrete Gardens|176|x", "Rooftop Weather|193",
                "Blue Hour|205", "Tape Hiss|158|x", "Late Bus|184"),
            ),
            AlbumSpec(
                "Velvet Static", "Soft Machines", 2021, listOf("Soft Machines|223", "Dial Tone Lullaby|198",
                "Chrome Heart|211", "Overexposed|187"),
            ),
            AlbumSpec(
                "سهراب نوری", "شب‌های تهران", 2019, listOf("شب‌های تهران|238", "باران بی‌وقفه|221",
                "خانه‌ی کوچک|204", "آخرین ایستگاه|252"),
            ),
        )

        @JvmField val ARTISTS: List<Artist>
        @JvmField val ALBUMS: List<Album>
        @JvmField val TRACKS: List<Track>

        init {
            val artists = ArrayList<Artist>()
            val albums = ArrayList<Album>()
            val tracks = ArrayList<Track>()
            var order = 0
            for (spec in SPECS) {
                val artistId = "artist-" + Integer.toHexString(spec.artist.hashCode())
                val albumId = "album-" + Integer.toHexString(spec.artist.hashCode()) + "-" +
                    Integer.toHexString(spec.title.hashCode())
                val seed = spec.title.hashCode()
                for (i in spec.tracks.indices) {
                    val t = spec.tracks[i].split("|")
                    order++
                    tracks.add(
                        Track(
                            "track-$order", t[0], artistId, spec.artist, albumId, spec.title,
                            Integer.parseInt(t[1]) * 1000L, null, seed, null, t.size > 2, false,
                            spec.year, i + 1, NOW_MS - order * DAY / 2,
                        ),
                    )
                }
                albums.add(
                    Album(
                        albumId, spec.title, artistId, spec.artist, spec.year,
                        spec.tracks.size, null, seed,
                    ),
                )
                var existing = -1
                for (i in artists.indices) if (artists[i].id == artistId) existing = i
                if (existing >= 0) {
                    val a = artists[existing]
                    artists[existing] = Artist(
                        a.id, a.name, a.albumCount + 1,
                        a.trackCount + spec.tracks.size, null, a.placeholderSeed,
                    )
                } else {
                    artists.add(
                        Artist(
                            artistId, spec.artist, 1, spec.tracks.size, null,
                            spec.artist.hashCode(),
                        ),
                    )
                }
            }
            ARTISTS = Collections.unmodifiableList(artists)
            ALBUMS = Collections.unmodifiableList(albums)
            TRACKS = Collections.unmodifiableList(tracks)
        }

        /** Ids of tracks liked on first launch. */
        @JvmField val INITIALLY_LIKED_IDS: Set<String> =
            setOf("track-1", "track-2", "track-9", "track-14", "track-20", "track-27")

        /** Seed playlists in display order: playlist -> its track ids. */
        @JvmField val SEED_PLAYLISTS: Map<Playlist, List<String>>

        init {
            val m = LinkedHashMap<Playlist, List<String>>()
            m[playlist("playlist-gym", "Gym motivation", "High energy", 11)] =
                listOf("track-1", "track-3", "track-4", "track-5", "track-9", "track-10", "track-12")
            m[playlist("playlist-night", "Late night drive", "Synth and slow tempo", 22)] =
                listOf("track-17", "track-22", "track-23", "track-24", "track-25", "track-29")
            m[playlist("playlist-fa", "فارسی", "Persian favorites", 33)] =
                listOf("track-34", "track-35", "track-36", "track-37")
            SEED_PLAYLISTS = Collections.unmodifiableMap(m)
        }

        /** Track ids in the initial listening history, most recent first. */
        @JvmField val SEED_HISTORY_IDS: List<String> =
            listOf("track-2", "track-14", "track-1", "track-22", "track-9", "track-27", "track-35", "track-5")

        @JvmStatic private fun playlist(id: String, name: String, description: String, seed: Int): Playlist {
            return Playlist(id, name, 0, description, emptyList(), seed)
        }

        private val ENGLISH_TIMES_AND_LINES = arrayOf(
            "0|", "12000|Streetlights flicker on the avenue", "17500|I carry the weight and I carry the view",
            "23000|Every step is a promise I make to myself", "28500|Nobody's counting but I still keep score",
            "34000|So turn it up, let the engine run loud", "40000|We're building a skyline out of the doubt",
            "46000|Hold the line, hold the line", "52000|The morning is coming, it's almost time",
            "60000|", "64000|Footsteps echo in a quiet town", "70000|I wrote my name where the rain came down",
            "76000|Every scar is a map to where I've been", "82000|And I'm not afraid to begin again",
            "90000|Turn it up, let the engine run loud", "96000|We're building a skyline out of the doubt",
            "102000|Hold the line, hold the line", "108000|The morning is here and it's burning bright", "118000|",
        )

        private val PERSIAN_TIMES_AND_LINES = arrayOf(
            "0|", "10000|شب‌های تهران بوی باران می‌دهد", "18000|کوچه‌ها آرام و چراغ‌ها بیدار",
            "26000|من هنوز اینجا منتظر صبحم", "34000|با دلی که هیچ‌وقت خسته نمی‌شود", "44000|",
        )

        @JvmStatic private fun lines(timesAndLines: Array<String>, synced: Boolean, skipBlank: Boolean): List<LyricLine> {
            val out = ArrayList<LyricLine>()
            for (s in timesAndLines) {
                val bar = s.indexOf('|')
                val text = s.substring(bar + 1)
                if (skipBlank && text.isEmpty()) continue
                out.add(LyricLine(if (synced) s.substring(0, bar).toLong() else LyricLine.UNSYNCED, text))
            }
            return out
        }

        /**
         * track-3 has plain (unsynced) lyrics, track-4 has none, Persian tracks (34-37) have synced Persian lyrics,
         * every other track has synced English lyrics.
         */
        @JvmStatic fun lyricsFor(trackId: String): Lyrics {
            return when (trackId) {
                "track-4" -> Lyrics.none(trackId)
                "track-3" -> Lyrics(trackId, lines(ENGLISH_TIMES_AND_LINES, false, true), false, "Embedded")
                "track-34", "track-35", "track-36", "track-37" ->
                    Lyrics(trackId, lines(PERSIAN_TIMES_AND_LINES, true, false), true, "LRC file")
                else -> Lyrics(trackId, lines(ENGLISH_TIMES_AND_LINES, true, false), true, "LRCLIB")
            }
        }
    }
}

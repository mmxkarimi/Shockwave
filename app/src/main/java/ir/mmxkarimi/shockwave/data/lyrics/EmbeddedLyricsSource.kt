package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LyricsSource.Found
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File
import java.io.RandomAccessFile

/**
 * Lyrics stored inside the audio file: ID3v2 USLT/SYLT (MP3), Vorbis comments (FLAC), `©lyr` (M4A/MP4).
 * Ogg Vorbis/Opus comments are not read (would need an Ogg page walker). Only the tag area is read, never the audio.
 */
class EmbeddedLyricsSource(private val locator: AudioFileLocator) : LyricsSource {

    override fun load(track: Track, cancel: CancelToken): Found? {
        val file = locator.locate(track)
        if (file == null || !file.isFile) return null
        return RandomAccessFile(file, "r").use { f -> read(f) }
    }

    companion object {
        private const val LABEL = "Embedded"

        @JvmStatic internal fun read(f: RandomAccessFile): Found? {
            val head = ByteArray(12)
            f.seek(0)
            val n = f.read(head)
            var text: String? = null
            if (n >= 4 && head[0].toInt() == 'I'.code && head[1].toInt() == 'D'.code && head[2].toInt() == '3'.code) {
                text = Id3LyricsReader.read(f)
            } else if (n >= 4 && head[0].toInt() == 'f'.code && head[1].toInt() == 'L'.code && head[2].toInt() == 'a'.code && head[3].toInt() == 'C'.code) {
                text = FlacLyricsReader.read(f)
            } else if (n >= 12 && head[4].toInt() == 'f'.code && head[5].toInt() == 't'.code && head[6].toInt() == 'y'.code && head[7].toInt() == 'p'.code) {
                text = Mp4LyricsReader.read(f)
            }
            return text?.let { Found(it, LABEL) }
        }
    }
}

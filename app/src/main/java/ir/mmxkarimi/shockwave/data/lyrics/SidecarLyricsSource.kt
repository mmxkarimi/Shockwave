package ir.mmxkarimi.shockwave.data.lyrics

import ir.mmxkarimi.shockwave.data.lyrics.LyricsSource.Found
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File
import java.io.RandomAccessFile

/** A `.lrc` (then `.txt`) file next to the audio file with the same base name. */
class SidecarLyricsSource(private val locator: AudioFileLocator) : LyricsSource {

    override fun load(track: Track, cancel: CancelToken): Found? {
        val audio = locator.locate(track) ?: return null
        val name = audio.name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        for (ext in EXTENSIONS) {
            val candidate = File(audio.parentFile, base + ext)
            if (!candidate.isFile) continue
            return RandomAccessFile(candidate, "r").use { f ->
                val buf = ByteArray(minOf(f.length(), MAX_BYTES.toLong()).toInt())
                f.readFully(buf)
                Found(LyricsTextDecoder.decode(buf), "LRC file")
            }
        }
        return null
    }

    companion object {
        private val EXTENSIONS = arrayOf(".lrc", ".LRC", ".txt")
        private const val MAX_BYTES = 512 * 1024
    }
}

package ir.mmxkarimi.shockwave.data.scanner

import ir.mmxkarimi.shockwave.data.db.query.Seeds
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * One MediaStore audio row, already read from the cursor; [toTrack] is the pure mapping to the domain.
 * `trackNumber` is MediaStore's raw value (disc * 1000 + track); the track queries split it on read.
 */
data class ScannedAudio(
    @get:JvmName("id") val id: Long,
    @get:JvmName("title") val title: String?,
    @get:JvmName("displayName") val displayName: String?,
    @get:JvmName("artist") val artist: String?,
    @get:JvmName("artistId") val artistId: Long,
    @get:JvmName("album") val album: String?,
    @get:JvmName("albumId") val albumId: Long,
    @get:JvmName("durationMs") val durationMs: Long,
    @get:JvmName("trackNumber") val trackNumber: Int,
    @get:JvmName("year") val year: Int,
    @get:JvmName("addedAtMs") val addedAtMs: Long,
    @get:JvmName("modifiedMs") val modifiedMs: Long,
    @get:JvmName("path") val path: String?,
) {

    fun toTrack(): Track {
        val albumKey = albumId.toString()
        return Track(
            id.toString(), titleOrFileName(), artistId.toString(),
            known(artist, "Unknown artist"), albumKey, known(album, "Unknown album"), durationMs,
            if (albumId > 0) ALBUM_ART_URI + albumId else null, Seeds.of(albumKey), AUDIO_URI + id,
            false, false, year, trackNumber, addedAtMs,
        )
    }

    private fun titleOrFileName(): String {
        if (title != null && !title.trim().isEmpty()) return title.trim()
        if (displayName == null || displayName.trim().isEmpty()) return "Unknown title"
        val dot = displayName.lastIndexOf('.')
        return if (dot > 0) displayName.substring(0, dot) else displayName
    }

    companion object {
        /** MediaStore's placeholder for missing tags. */
        private const val UNKNOWN = "<unknown>"
        private const val AUDIO_URI = "content://media/external/audio/media/"
        private const val ALBUM_ART_URI = "content://media/external/audio/albumart/"

        @JvmStatic private fun known(value: String?, fallback: String): String =
            if (value == null || value.trim().isEmpty() || UNKNOWN == value) fallback else value.trim()
    }
}

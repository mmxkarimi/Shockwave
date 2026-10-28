package ir.mmxkarimi.shockwave.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ir.mmxkarimi.shockwave.domain.model.Track

/**
 * Domain [Track] <-> Media3 [MediaItem]. The mediaId is the track id; the complete track travels in
 * the metadata extras, so the player itself is the single source of truth for the queue (no side maps, and a
 * queue restored after process death or seen by a headless service decodes the same way).
 */
internal class TrackMapper private constructor() {
    companion object {
        private const val ID = "id"
        private const val TITLE = "title"
        private const val ARTIST_ID = "artistId"
        private const val ARTIST = "artist"
        private const val ALBUM_ID = "albumId"
        private const val ALBUM = "album"
        private const val DURATION = "duration"
        private const val ARTWORK = "artwork"
        private const val SEED = "seed"
        private const val URI = "uri"
        private const val EXPLICIT = "explicit"
        private const val LIKED = "liked"
        private const val YEAR = "year"
        private const val NUMBER = "number"
        private const val ADDED = "added"

        /** Only tracks with a playable URI can be queued (fakes have none). */
        @JvmStatic
        @JvmName("isPlayable")
        internal fun isPlayable(track: Track): Boolean {
            return track.mediaUri != null
        }

        @JvmStatic
        @JvmName("toMediaItem")
        internal fun toMediaItem(t: Track): MediaItem {
            val extras = Bundle()
            extras.putString(ID, t.id)
            extras.putString(TITLE, t.title)
            extras.putString(ARTIST_ID, t.artistId)
            extras.putString(ARTIST, t.artistName)
            extras.putString(ALBUM_ID, t.albumId)
            extras.putString(ALBUM, t.albumTitle)
            extras.putLong(DURATION, t.durationMs)
            extras.putString(ARTWORK, t.artworkUri)
            extras.putInt(SEED, t.placeholderSeed)
            extras.putString(URI, t.mediaUri)
            extras.putBoolean(EXPLICIT, t.explicit)
            extras.putBoolean(LIKED, t.liked)
            extras.putInt(YEAR, t.year)
            extras.putInt(NUMBER, t.trackNumber)
            extras.putLong(ADDED, t.addedAtMs)
            val meta = MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artistName)
                    .setAlbumTitle(t.albumTitle)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
            if (t.durationMs > 0) meta.setDurationMs(t.durationMs)
            if (t.trackNumber > 0) meta.setTrackNumber(t.trackNumber)
            if (t.artworkUri != null) meta.setArtworkUri(Uri.parse(t.artworkUri))
            return MediaItem.Builder()
                    .setMediaId(t.id)
                    .setUri(t.mediaUri)
                    .setMediaMetadata(meta.build())
                    .build()
        }

        /** Inverse of [toMediaItem]; null for an item that was not created by it. */
        @JvmStatic
        @JvmName("fromMediaItem")
        internal fun fromMediaItem(item: MediaItem?): Track? {
            val e: Bundle? = if (item == null) null else item.mediaMetadata.extras
            if (e == null || e.getString(ID) == null) return null
            return Track(e.getString(ID)!!, nz(e.getString(TITLE)), nz(e.getString(ARTIST_ID)),
                    nz(e.getString(ARTIST)), nz(e.getString(ALBUM_ID)), nz(e.getString(ALBUM)),
                    e.getLong(DURATION), e.getString(ARTWORK), e.getInt(SEED), e.getString(URI),
                    e.getBoolean(EXPLICIT), e.getBoolean(LIKED), e.getInt(YEAR), e.getInt(NUMBER),
                    e.getLong(ADDED))
        }

        @JvmStatic
        private fun nz(s: String?): String {
            return s ?: ""
        }
    }
}
package ir.mmxkarimi.shockwave.playback

/** Receives "this track has really been listened to" events; the app wires it to `LibraryRepository.recordPlay`. */
fun interface PlayCountListener {
    /** Main thread, once per play-through of a track (see [PlayCountTracker]). */
    fun onTrackPlayed(trackId: String)
}
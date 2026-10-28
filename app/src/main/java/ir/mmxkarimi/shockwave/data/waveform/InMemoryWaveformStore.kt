package ir.mmxkarimi.shockwave.data.waveform

/** Non-persistent [WaveformStore] for tests and previews. */
class InMemoryWaveformStore : WaveformStore {
    private data class Entry(
        @get:JvmName("fingerprint") val fingerprint: String,
        @get:JvmName("blob") val blob: ByteArray,
    )

    private val entries = HashMap<String, Entry>()

    @Synchronized
    override fun get(trackId: String, fingerprint: String): ByteArray? {
        val e = entries[trackId]
        return if (e != null && e.fingerprint == fingerprint) e.blob else null
    }

    @Synchronized
    override fun put(trackId: String, fingerprint: String, blob: ByteArray) {
        entries[trackId] = Entry(fingerprint, blob)
    }
}

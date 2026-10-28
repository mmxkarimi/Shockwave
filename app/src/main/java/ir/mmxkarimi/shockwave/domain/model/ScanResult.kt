package ir.mmxkarimi.shockwave.domain.model

/** Outcome of one scan. [ok] == false means nothing was changed (e.g. permission missing). */
data class ScanResult(
    @get:JvmName("ok") val ok: Boolean,
    @get:JvmName("added") val added: Int,
    @get:JvmName("updated") val updated: Int,
    @get:JvmName("removed") val removed: Int,
) {
    fun changed(): Boolean = added + updated + removed > 0

    companion object {
        @JvmField val FAILED = ScanResult(false, 0, 0, 0)
    }
}

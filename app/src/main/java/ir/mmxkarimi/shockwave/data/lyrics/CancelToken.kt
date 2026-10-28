package ir.mmxkarimi.shockwave.data.lyrics

/** Cooperative cancellation for one lyrics lookup; a running HTTP call registers `disconnect` here. */
class CancelToken {
    @Volatile private var cancelled = false
    @Volatile private var onCancel: Runnable? = null

    fun cancel() {
        cancelled = true
        val r = onCancel
        if (r != null) r.run()
    }

    fun isCancelled(): Boolean = cancelled

    /** Registers (or clears, with null) what to run on cancel; runs it at once when already cancelled. */
    fun setOnCancel(r: Runnable?) {
        onCancel = r
        if (r != null && cancelled) r.run()
    }
}

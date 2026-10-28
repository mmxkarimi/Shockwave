package ir.mmxkarimi.shockwave.domain.repository

/** One-shot async result. Always delivered on the main thread. */
fun interface Callback<T> {
    fun onResult(value: T)
}

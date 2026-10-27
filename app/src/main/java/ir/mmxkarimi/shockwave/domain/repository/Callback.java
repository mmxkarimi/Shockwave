package ir.mmxkarimi.shockwave.domain.repository;

/** One-shot async result. Always delivered on the main thread. */
public interface Callback<T> {
    void onResult(T value);
}

package ir.mmxkarimi.shockwave.data.db.query

/**
 * Stable placeholder-gradient seeds. Derived from the MediaStore ids (never stored), so a track, its album and its
 * artist always get the same colors across runs. `String.hashCode` is specified, hence stable.
 */
class Seeds private constructor() {
    companion object {
        @JvmStatic
        fun of(key: String): Int {
            var h = key.hashCode()
            h = h xor (h ushr 16)
            h *= 0x85ebca6b.toInt() // same two's-complement int multiply as Java's h *= 0x85ebca6b
            h = h xor (h ushr 13)
            return h
        }
    }
}

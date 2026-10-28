package ir.mmxkarimi.shockwave.ui.player

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Pure formatting helpers for the player (locale-aware digits, so Persian shows ۰–۹). */
object PlayerFormat {
    /** "3:07" / "1:02:09" in the locale's digits. Negative input clamps to 0. */
    @JvmStatic
    fun time(ms: Long, locale: Locale): String {
        val totalSec = max(0L, ms) / 1000L
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val nf = NumberFormat.getIntegerInstance(locale).apply {
            isGroupingUsed = false
        }
        val two = NumberFormat.getIntegerInstance(locale).apply {
            isGroupingUsed = false
            minimumIntegerDigits = 2
        }
        return if (h > 0) {
            "${nf.format(h)}:${two.format(m)}:${two.format(s)}"
        } else {
            "${nf.format(m)}:${two.format(s)}"
        }
    }

    /** "-2:09": time left in the track. */
    @JvmStatic
    fun remaining(positionMs: Long, durationMs: Long, locale: Locale): String {
        return "−" + time(max(0L, durationMs - positionMs), locale)
    }

    /** Whole minutes, rounded up so "0:30 left" still reads as 1 min. */
    @JvmStatic
    fun minutesCeil(ms: Long): Int {
        if (ms <= 0) return 0
        return ((ms + 59_999L) / 60_000L).toInt()
    }

    /** Fraction in [0,1] of position over duration (0 when duration is unknown). */
    @JvmStatic
    fun fraction(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0) return 0f
        return max(0f, min(1f, positionMs / durationMs.toFloat()))
    }
}

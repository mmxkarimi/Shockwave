package ir.mmxkarimi.shockwave.ui.library

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** Locale-aware number/duration formatting (Persian digits in fa). */
object Format {
    @JvmStatic
    fun duration(ms: Long, locale: Locale): String {
        val s = max(0, ms / 1000)
        val nf = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
        val two = NumberFormat.getIntegerInstance(locale).apply {
            minimumIntegerDigits = 2
            isGroupingUsed = false
        }
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            "${nf.format(h)}:${two.format(m)}:${two.format(sec)}"
        } else {
            "${nf.format(m)}:${two.format(sec)}"
        }
    }
}

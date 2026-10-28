package ir.mmxkarimi.shockwave.ui.library

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import java.util.Locale

/** Bolds the first case-insensitive match of the query. */
object Highlight {
    @JvmStatic
    fun apply(text: String, query: String?): CharSequence {
        if (query.isNullOrBlank()) return text
        val trimmed = query.trim()
        val i = text.lowercase(Locale.ROOT).indexOf(trimmed.lowercase(Locale.ROOT))
        if (i < 0) return text
        val s = SpannableString(text)
        s.setSpan(StyleSpan(Typeface.BOLD), i, i + trimmed.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }
}

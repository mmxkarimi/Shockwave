package ir.mmxkarimi.shockwave.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import ir.mmxkarimi.shockwave.R

/**
 * Per-script font helper.
 */
object ScriptFont {
    private var vazir: Typeface? = null
    private val BY_WEIGHT = arrayOfNulls<Typeface>(10)

    @JvmStatic
    fun setText(view: TextView, text: CharSequence?) {
        view.text = text
        apply(view, text)
    }

    @JvmStatic
    fun apply(view: TextView, text: CharSequence?) {
        val tag = view.getTag(R.id.script_font_base)
        val base: Typeface = if (tag is Typeface) {
            tag
        } else {
            val tf = view.typeface ?: Typeface.DEFAULT
            view.setTag(R.id.script_font_base, tf)
            tf
        }
        val want = if (hasArabicScript(text)) vazirFor(view.context, base) else base
        if (want != null && want != view.typeface) view.typeface = want
    }

    @JvmStatic
    fun hasArabicScript(s: CharSequence?): Boolean {
        if (s == null) return false
        val n = s.length
        for (i in 0 until n) {
            val c = s[i]
            if (c < 0x0600.toChar()) continue
            if (c <= 0x06FF.toChar() ||
                (c in 0x0750.toChar()..0x077F.toChar()) ||
                (c in 0x08A0.toChar()..0x08FF.toChar()) ||
                (c in 0xFB50.toChar()..0xFDFF.toChar()) ||
                (c in 0xFE70.toChar()..0xFEFF.toChar())
            ) {
                return true
            }
        }
        return false
    }

    private fun vazirFor(context: Context, match: Typeface?): Typeface? {
        val base = baseVazir(context) ?: return match
        val weight = if (Build.VERSION.SDK_INT >= 28 && match != null) {
            match.weight
        } else if (match != null && match.isBold) {
            700
        } else {
            400
        }
        val slot = (weight / 100).coerceIn(1, 9)
        var at = BY_WEIGHT[slot]
        if (at == null) {
            at = if (Build.VERSION.SDK_INT >= 28) {
                Typeface.create(base, weight, match?.isItalic == true)
            } else {
                Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
            }
            BY_WEIGHT[slot] = at
        }
        return at
    }

    private fun baseVazir(context: Context): Typeface? {
        if (vazir == null) {
            try {
                vazir = ResourcesCompat.getFont(context.applicationContext, R.font.vazirmatn_regular)
            } catch (e: Exception) {
                vazir = Typeface.SANS_SERIF
            }
        }
        return vazir
    }
}

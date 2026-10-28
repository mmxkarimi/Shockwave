package ir.mmxkarimi.shockwave.data.lyrics

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Turns the bytes of a lyrics file into text: UTF-8 / UTF-16 BOMs, then strict UTF-8, then a legacy 8-bit fallback.
 * Old Persian/Arabic `.lrc` files are usually Windows-1256, so that is chosen when most non-ASCII characters decode
 * to Arabic-script letters; anything else falls back to ISO-8859-1.
 */
internal class LyricsTextDecoder private constructor() {

    companion object {
        @JvmStatic internal fun decode(b: ByteArray): String {
            if (b.size >= 3 && (b[0].toInt() and 0xFF) == 0xEF && (b[1].toInt() and 0xFF) == 0xBB &&
                (b[2].toInt() and 0xFF) == 0xBF
            ) {
                return String(b, 3, b.size - 3, StandardCharsets.UTF_8)
            }
            if (b.size >= 2 && (b[0].toInt() and 0xFF) == 0xFF && (b[1].toInt() and 0xFF) == 0xFE) {
                return String(b, 2, b.size - 2, StandardCharsets.UTF_16LE)
            }
            if (b.size >= 2 && (b[0].toInt() and 0xFF) == 0xFE && (b[1].toInt() and 0xFF) == 0xFF) {
                return String(b, 2, b.size - 2, StandardCharsets.UTF_16BE)
            }
            return try {
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b))
                    .toString()
            } catch (e: CharacterCodingException) {
                val arabic = String(b, legacyArabic())
                if (mostlyArabic(arabic)) arabic else String(b, StandardCharsets.ISO_8859_1)
            }
        }

        @JvmStatic private fun mostlyArabic(s: String): Boolean {
            var nonAscii = 0
            var arabic = 0
            for (i in s.indices) {
                val c = s[i]
                if (c.code < 0x80) continue
                nonAscii++
                if (c.code in 0x0600..0x06FF) arabic++
            }
            return nonAscii > 0 && arabic * 2 >= nonAscii
        }

        @JvmStatic private fun legacyArabic(): Charset {
            return try {
                Charset.forName("windows-1256")
            } catch (e: RuntimeException) { // unsupported on this runtime
                StandardCharsets.ISO_8859_1
            }
        }
    }
}

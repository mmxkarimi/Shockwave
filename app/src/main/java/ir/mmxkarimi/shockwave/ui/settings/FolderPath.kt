package ir.mmxkarimi.shockwave.ui.settings

import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Turns a tree URI from the system folder picker into the absolute path the scanner matches MediaStore file paths against.
 */
object FolderPath {
    @JvmStatic
    fun fromTreeUri(uri: Uri?): String? {
        if (uri == null) return null
        val segments = uri.pathSegments
        if (segments.size < 2 || "tree" != segments[0]) return null
        val tree = segments[1]
        val colon = tree.indexOf(':')
        if (colon <= 0) return null
        val volume = tree.substring(0, colon)
        val rest = tree.substring(colon + 1)
        if ("primary".equals(volume, ignoreCase = true)) {
            val base = Environment.getExternalStorageDirectory().absolutePath
            return if (rest.isEmpty()) base else "$base/$rest"
        }
        val path = "/storage/$volume" + (if (rest.isEmpty()) "" else "/$rest")
        return if (File(path).isDirectory) path else null
    }
}

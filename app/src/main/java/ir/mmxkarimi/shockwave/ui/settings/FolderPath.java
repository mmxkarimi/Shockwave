package ir.mmxkarimi.shockwave.ui.settings;

import android.net.Uri;
import android.os.Environment;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * Turns a tree URI from the system folder picker ({@code ACTION_OPEN_DOCUMENT_TREE}) into the absolute path the
 * scanner matches MediaStore file paths against. The app only needs the path string (the scanner never reads the
 * folder itself), so no persistable URI permission is taken. Unknown volumes are rejected: a bogus include path
 * would silently filter the whole library out.
 */
public final class FolderPath {
    private FolderPath() {}

    /** @return the picked directory's absolute path, or null when it is not a local storage folder */
    @Nullable
    public static String fromTreeUri(@Nullable Uri uri) {
        if (uri == null) return null;
        List<String> segments = uri.getPathSegments();
        // content://.../tree/<volume>:<path>, e.g. "primary:Music" or "1234-ABCD:Podcasts/Shows"
        if (segments.size() < 2 || !"tree".equals(segments.get(0))) return null;
        String tree = segments.get(1);
        int colon = tree.indexOf(':');
        if (colon <= 0) return null;
        String volume = tree.substring(0, colon);
        String rest = tree.substring(colon + 1);
        if ("primary".equalsIgnoreCase(volume)) {
            String base = Environment.getExternalStorageDirectory().getAbsolutePath();
            return rest.isEmpty() ? base : base + "/" + rest;
        }
        // SD cards and other volumes: only accept a directory that really exists on this device.
        String path = "/storage/" + volume + (rest.isEmpty() ? "" : "/" + rest);
        return new File(path).isDirectory() ? path : null;
    }
}

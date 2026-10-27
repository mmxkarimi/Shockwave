package ir.mmxkarimi.shockwave.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.net.Uri;
import android.os.Environment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** The folder picker's tree URI is the only input to the scanner's include filter. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FolderPathTest {
    @Test public void primaryVolumeMapsToExternalStorage() {
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic");
        assertEquals(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music",
                FolderPath.fromTreeUri(uri));
    }

    @Test public void nestedPrimaryPathKeepsItsSlashes() {
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic%2FDownload");
        assertEquals(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/Download",
                FolderPath.fromTreeUri(uri));
    }

    @Test public void primaryRootIsTheStorageRoot() {
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A");
        assertEquals(Environment.getExternalStorageDirectory().getAbsolutePath(), FolderPath.fromTreeUri(uri));
    }

    @Test public void missingVolumeIsRejected() {
        assertNull(FolderPath.fromTreeUri(null));
        assertNull(FolderPath.fromTreeUri(Uri.parse("content://com.android.externalstorage.documents/document/primary%3AMusic")));
        assertNull(FolderPath.fromTreeUri(Uri.parse("content://com.example/tree/no-colon-here")));
        // A volume that does not exist on this device would filter every song out.
        assertNull(FolderPath.fromTreeUri(Uri.parse("content://com.android.externalstorage.documents/tree/NOPE%3AMusic")));
    }
}

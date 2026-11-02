package ir.mmxkarimi.shockwave.ui.components

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import ir.mmxkarimi.shockwave.domain.model.Track
import java.io.File

object TrackDeleteHelper {

    /**
     * Attempts to delete [track] from Android storage.
     *
     * - On Android 11+ (API 30+): Requests user confirmation through [MediaStore.createDeleteRequest].
     *   When the user grants consent in the system dialog, the system deletes the file from storage and MediaStore,
     *   and [launcher] receives Activity.RESULT_OK.
     * - On Android 10 (API 29): Tries direct ContentResolver deletion; if [RecoverableSecurityException] is caught,
     *   launches its IntentSender through [launcher].
     * - On older versions / file URIs: Deletes directly and calls [onDeletedDirectly].
     */
    fun deleteTrack(
        context: Context,
        track: Track,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onDeletedDirectly: () -> Unit
    ) {
        val uriStr = track.mediaUri
        val uri = if (!uriStr.isNullOrEmpty()) Uri.parse(uriStr) else null
        if (uri == null) {
            onDeletedDirectly()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri.scheme == "content") {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                launcher.launch(request)
            } catch (e: Exception) {
                try {
                    val count = context.contentResolver.delete(uri, null, null)
                    if (count > 0) {
                        onDeletedDirectly()
                        return
                    }
                } catch (_: Exception) {}
                onDeletedDirectly()
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && uri.scheme == "content") {
            try {
                val count = context.contentResolver.delete(uri, null, null)
                if (count > 0) {
                    onDeletedDirectly()
                }
            } catch (e: RecoverableSecurityException) {
                val request = IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                launcher.launch(request)
            } catch (e: Exception) {
                onDeletedDirectly()
            }
        } else {
            try {
                if (uri.scheme == "content") {
                    context.contentResolver.delete(uri, null, null)
                } else if (uri.scheme == "file") {
                    val path = uri.path
                    if (path != null) {
                        File(path).delete()
                    }
                }
            } catch (_: Exception) {}
            onDeletedDirectly()
        }
    }
}

package ir.mmxkarimi.shockwave.ui.components

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.nav.Screens
import ir.mmxkarimi.shockwave.ui.widget.ArtworkLoader
import ir.mmxkarimi.shockwave.widget.ShockwaveWidgetUpdater
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Deletes a track's audio file from storage, and only then removes it from the library.
 *
 * The library row must outlive a failed file delete: otherwise the next scan (or a reinstall) finds the file in
 * MediaStore again and the "deleted" track comes back.
 *
 * - Android 11+ (API 30+): [MediaStore.createDeleteRequest]; the system deletes the file once the user agrees.
 * - Android 10 (API 29): the app runs with legacy storage, so deleting needs WRITE_EXTERNAL_STORAGE, which is asked
 *   for first. A [RecoverableSecurityException] (scoped mode) is answered through its consent dialog, then retried.
 * - `file://` URIs are deleted directly.
 */
class TrackDeleter internal constructor(private val start: (Track) -> Unit) {
    fun delete(track: Track) = start(track)
}

/** What is being deleted while a system dialog is open; strings only so it survives activity recreation. */
private data class PendingDelete(val id: String, val mediaUri: String?, val artworkUri: String?) {
    fun toList(): List<String?> = listOf(id, mediaUri, artworkUri)

    companion object {
        fun of(track: Track) = PendingDelete(track.id, track.mediaUri, track.artworkUri)
        fun fromList(list: List<String?>?): PendingDelete? {
            val id = list?.getOrNull(0) ?: return null
            return PendingDelete(id, list.getOrNull(1), list.getOrNull(2))
        }
    }
}

/** Result of one storage delete attempt; [NeedsConsent] carries the system dialog to show (Android 10). */
private sealed interface Outcome {
    data object Deleted : Outcome
    data object Failed : Outcome
    class NeedsConsent(val request: IntentSenderRequest) : Outcome
}

@Composable
fun rememberTrackDeleter(): TrackDeleter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSaved by rememberSaveable { mutableStateOf<List<String?>?>(null) }

    lateinit var consentLauncher: ActivityResultLauncher<IntentSenderRequest>

    fun finish(pending: PendingDelete, outcome: Outcome) {
        when (outcome) {
            Outcome.Deleted -> {
                pendingSaved = null
                TrackDeleteHelper.onFileDeleted(context, pending.id, pending.mediaUri, pending.artworkUri)
            }
            Outcome.Failed -> {
                pendingSaved = null
                Toast.makeText(context, R.string.delete_track_failed, Toast.LENGTH_LONG).show()
            }
            is Outcome.NeedsConsent -> consentLauncher.launch(outcome.request)
        }
    }

    consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = PendingDelete.fromList(pendingSaved) ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            pendingSaved = null // the user said no: nothing to report
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // createDeleteRequest: the system already deleted the file.
            finish(pending, Outcome.Deleted)
        } else {
            // Android 10: write access was granted, the delete itself still has to run (no second dialog).
            scope.launch { finish(pending, deleteLegacy(context, pending, allowConsent = false)) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = PendingDelete.fromList(pendingSaved) ?: return@rememberLauncherForActivityResult
        if (!granted) {
            pendingSaved = null
            Toast.makeText(context, R.string.delete_track_permission_denied, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch { finish(pending, deleteLegacy(context, pending, allowConsent = true)) }
    }

    return TrackDeleter { track ->
        val pending = PendingDelete.of(track)
        pendingSaved = pending.toList()
        val uri = track.mediaUri?.let { Uri.parse(it) }
        when {
            uri == null -> finish(pending, Outcome.Deleted) // nothing on storage (sample data)
            uri.scheme == "file" -> scope.launch {
                val ok = withContext(Dispatchers.IO) { deleteFile(uri.path) }
                finish(pending, if (ok) Outcome.Deleted else Outcome.Failed)
            }
            uri.scheme != "content" -> finish(pending, Outcome.Failed)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                try {
                    val intent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    consentLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                } catch (e: Exception) {
                    finish(pending, Outcome.Failed)
                }
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ->
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> scope.launch { finish(pending, deleteLegacy(context, pending, allowConsent = true)) }
        }
    }
}

/** Android 10: deletes through MediaStore, then the file itself if MediaStore left it behind. */
private suspend fun deleteLegacy(context: Context, pending: PendingDelete, allowConsent: Boolean): Outcome =
    withContext(Dispatchers.IO) {
        val uri = Uri.parse(pending.mediaUri)
        try {
            val path = TrackDeleteHelper.dataPath(context, uri)
            val rows = context.contentResolver.delete(uri, null, null)
            val fileGone = path == null || deleteFile(path)
            if ((rows > 0 || path != null) && fileGone) Outcome.Deleted else Outcome.Failed
        } catch (e: RecoverableSecurityException) {
            if (allowConsent) {
                Outcome.NeedsConsent(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
            } else {
                Outcome.Failed
            }
        } catch (e: Exception) {
            Outcome.Failed
        }
    }

/** True when [path] no longer exists afterwards. */
private fun deleteFile(path: String?): Boolean {
    if (path == null) return false
    val file = File(path)
    if (!file.exists()) return true
    return try {
        file.delete() || !file.exists()
    } catch (e: SecurityException) {
        false
    }
}

object TrackDeleteHelper {
    /** MediaStore's file path for [uri] (legacy storage can delete it directly), or null. */
    @Suppress("DEPRECATION")
    internal fun dataPath(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** The file is gone: drop the row, the queue entries and every cached cover of the track. */
    internal fun onFileDeleted(context: Context, trackId: String, mediaUri: String?, artworkUri: String?) {
        val container = Screens.container(context)
        container.libraryAdmin().deleteTrack(trackId)
        val player = container.player
        val queue = player.getState().value?.queue.orEmpty()
        for (i in queue.indices.reversed()) {
            if (queue[i].id == trackId) player.removeFromQueue(i)
        }
        val loader = ArtworkLoader.get(context)
        artworkUri?.let { loader.evict(it) }
        mediaUri?.let { loader.evict(it) }
        ShockwaveWidgetUpdater.request(context)
    }
}

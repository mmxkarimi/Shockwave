package ir.mmxkarimi.shockwave.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mmxkarimi.shockwave.R
import ir.mmxkarimi.shockwave.domain.model.Track
import ir.mmxkarimi.shockwave.ui.player.PlayerFormat
import java.io.File
import java.util.Locale

data class TrackTechnicalDetails(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String?,
    val duration: String,
    val year: String?,
    val trackNumber: String?,
    val mimeType: String?,
    val bitrate: String?,
    val samplingRate: String?,
    val fileSize: String?,
    val filePath: String?
)

fun extractTrackDetails(context: Context, track: Track): TrackTechnicalDetails {
    var genre: String? = null
    var year: String? = if (track.year > 0) track.year.toString() else null
    var cdTrack: String? = if (track.trackNumber > 0) {
        val num = if (track.trackNumber > 1000) track.trackNumber % 1000 else track.trackNumber
        num.toString()
    } else null
    var mimeType: String? = null
    var bitrateStr: String? = null
    var sampleRateStr: String? = null
    var fileSizeStr: String? = null
    var filePathStr: String? = null

    val uriStr = track.mediaUri
    if (!uriStr.isNullOrEmpty()) {
        val uri = Uri.parse(uriStr)
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            }
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val extractedYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!extractedYear.isNullOrBlank()) {
                year = extractedYear
            }
            val extractedTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            if (!extractedTrack.isNullOrBlank()) {
                cdTrack = extractedTrack
            }
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!rawBitrate.isNullOrBlank()) {
                val b = rawBitrate.toLongOrNull()
                bitrateStr = if (b != null && b > 0) "${b / 1000} kbps" else rawBitrate
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val rawSample = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                if (!rawSample.isNullOrBlank()) {
                    val hz = rawSample.toIntOrNull()
                    sampleRateStr = if (hz != null) String.format(Locale.US, "%.1f kHz", hz / 1000f) else "$rawSample Hz"
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE, MediaStore.Audio.Media.DATA),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                            val bytes = cursor.getLong(sizeIdx)
                            fileSizeStr = formatBytes(bytes)
                        }
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (dataIdx != -1 && !cursor.isNull(dataIdx)) {
                            filePathStr = cursor.getString(dataIdx)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    fileSizeStr = formatBytes(file.length())
                    filePathStr = file.absolutePath
                }
            }
        } catch (_: Exception) {}
    }

    val durationFormatted = PlayerFormat.time(track.durationMs, Locale.getDefault())

    return TrackTechnicalDetails(
        title = track.title,
        artist = track.artistName,
        album = track.albumTitle,
        genre = genre,
        duration = durationFormatted,
        year = year,
        trackNumber = cdTrack,
        mimeType = mimeType,
        bitrate = bitrateStr,
        samplingRate = sampleRateStr,
        fileSize = fileSizeStr,
        filePath = filePathStr
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.2f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.1f KB", kb)
    }
}

@Composable
fun TrackDetailsDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val details = remember(track) { extractTrackDetails(context, track) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.track_details_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            val items = buildList {
                add(stringResource(R.string.track_details_title_label) to details.title)
                add(stringResource(R.string.track_details_artist) to details.artist)
                add(stringResource(R.string.track_details_album) to details.album)
                details.genre?.let { add(stringResource(R.string.track_details_genre) to it) }
                add(stringResource(R.string.track_details_duration) to details.duration)
                details.year?.let { add(stringResource(R.string.track_details_year) to it) }
                details.trackNumber?.let { add(stringResource(R.string.track_details_track_number) to it) }
                details.mimeType?.let { add(stringResource(R.string.track_details_format) to it) }
                details.bitrate?.let { add(stringResource(R.string.track_details_bitrate) to it) }
                details.samplingRate?.let { add(stringResource(R.string.track_details_sampling_rate) to it) }
                details.fileSize?.let { add(stringResource(R.string.track_details_file_size) to it) }
                details.filePath?.let { add(stringResource(R.string.track_details_file_path) to it) }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items.forEachIndexed { index, (label, value) ->
                    DetailItem(
                        label = label,
                        value = value,
                        isFirst = index == 0,
                        isLast = index == items.lastIndex
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

/** Grouped-list row: tonal card, title on top, value below; big corners only on the first / last row. */
@Composable
private fun DetailItem(label: String, value: String, isFirst: Boolean, isLast: Boolean) {
    val outer = 20.dp
    val inner = 5.dp
    Surface(
        shape = RoundedCornerShape(
            topStart = if (isFirst) outer else inner,
            topEnd = if (isFirst) outer else inner,
            bottomStart = if (isLast) outer else inner,
            bottomEnd = if (isLast) outer else inner
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun shareTrack(context: Context, track: Track) {
    val uriStr = track.mediaUri ?: return
    val uri = Uri.parse(uriStr)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, "${track.artistName} - ${track.title}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.track_menu_share)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

package ir.mmxkarimi.shockwave.data.waveform

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Simple file-backed [WaveformStore]: one small file per track (`id, fingerprint, blob`) in a directory.
 * The file name is a hash of the id; the id inside the file guards against hash collisions.
 */
class FileWaveformStore(private val dir: File) : WaveformStore {

    private fun fileFor(trackId: String): File = File(dir, Integer.toHexString(trackId.hashCode()) + ".wf")

    override fun get(trackId: String, fingerprint: String): ByteArray? {
        val f = fileFor(trackId)
        if (!f.isFile) return null
        try {
            DataInputStream(FileInputStream(f)).use { input ->
                if (trackId != input.readUTF() || fingerprint != input.readUTF()) return null
                val blob = ByteArray(input.readInt())
                input.readFully(blob)
                return blob
            }
        } catch (e: IOException) {
            return null // corrupt or truncated: behave as a miss, put() will overwrite it
        } catch (e: RuntimeException) {
            return null // corrupt or truncated: behave as a miss, put() will overwrite it
        }
    }

    override fun put(trackId: String, fingerprint: String, blob: ByteArray) {
        if (!dir.isDirectory && !dir.mkdirs()) return
        val target = fileFor(trackId)
        val tmp = File(dir, target.name + ".tmp")
        try {
            DataOutputStream(FileOutputStream(tmp)).use { output ->
                output.writeUTF(trackId)
                output.writeUTF(fingerprint)
                output.writeInt(blob.size)
                output.write(blob)
            }
        } catch (e: IOException) {
            tmp.delete()
            return
        }
        if (!tmp.renameTo(target)) tmp.delete()
    }
}

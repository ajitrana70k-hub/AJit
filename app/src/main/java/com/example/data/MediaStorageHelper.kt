package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedMediaResult(
    val localUri: String,
    val durationSec: Long,
    val generatedThumbUri: String
)

object MediaStorageHelper {
    private fun mediaDir(context: Context): File {
        val dir = File(context.filesDir, "myvideo_media")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun importVideoWithProgress(
        context: Context,
        sourceUri: Uri,
        onProgress: (Int) -> Unit
    ): ImportedMediaResult = withContext(Dispatchers.IO) {
        val dir = mediaDir(context)
        val outFile = File(dir, "vid_${UUID.randomUUID()}.mp4")

        val resolver = context.contentResolver
        val totalBytes = resolver.openAssetFileDescriptor(sourceUri, "r")?.use { it.length } ?: -1L
        var copiedBytes = 0L

        resolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    copiedBytes += read
                    if (totalBytes > 0) {
                        val pct = ((copiedBytes * 100L) / totalBytes).toInt().coerceIn(5, 95)
                        onProgress(pct)
                    }
                }
            }
        } ?: throw IllegalStateException("Unable to read selected video")

        onProgress(98)

        var durationSec = 15L
        var thumbUri = ""
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(outFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 15000L
            durationSec = (durationMs / 1000L).coerceAtLeast(1L)
            val bitmap = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (bitmap != null) {
                val thumbFile = File(dir, "thumb_${UUID.randomUUID()}.jpg")
                FileOutputStream(thumbFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                bitmap.recycle()
                thumbUri = Uri.fromFile(thumbFile).toString()
            }
        } catch (_: Exception) {
            // Keep default duration and empty auto thumb if codec metadata unavailable
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        onProgress(100)
        ImportedMediaResult(
            localUri = Uri.fromFile(outFile).toString(),
            durationSec = durationSec,
            generatedThumbUri = thumbUri
        )
    }

    suspend fun importImage(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val dir = mediaDir(context)
        val outFile = File(dir, "img_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read selected image")
        Uri.fromFile(outFile).toString()
    }

    fun createRecordingOutputFile(context: Context): File {
        val dir = mediaDir(context)
        return File(dir, "cam_${System.currentTimeMillis()}.mp4")
    }
}

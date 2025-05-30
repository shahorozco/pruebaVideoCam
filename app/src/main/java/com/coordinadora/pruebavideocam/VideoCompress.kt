package com.coordinadora.pruebavideocam

import android.content.Context
import android.net.Uri
import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoCompressorManager(private val context: Context) {
    private val appContext = context.applicationContext
    companion object {
        private const val TAG = "VideoCompressor"
    }

    suspend fun compressVideoAsync(inputUri: Uri): Uri? = suspendCancellableCoroutine { cont ->
        try {
            val subFolder = File(appContext.filesDir, "CompressedVideos")
            if (!subFolder.exists()) subFolder.mkdirs()

            val filePath = File(subFolder, "compressed_${System.currentTimeMillis()}.mp4")

            val appSpecificStorageConfig = AppSpecificStorageConfiguration(
                subFolderName = "CompressedVideos",
                videoName = filePath.name
            )

            VideoCompressor.start(
                context = appContext,
                uris = listOf(inputUri),
                isStreamable = false,
                sharedStorageConfiguration = null,
                appSpecificStorageConfiguration = appSpecificStorageConfig,
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    isMinBitrateCheckEnabled = false,
                    videoBitrateInMbps = 1,
                    disableAudio = false,
                    keepOriginalResolution = false,
                    videoWidth = 320.0,
                    videoHeight = 480.0
                ),
                listener = object : CompressionListener {
                    override fun onCancelled(index: Int) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onProgress(index: Int, percent: Float) {
                        Log.d(TAG, "Progreso: ${percent.toInt()}%")
                    }

                    override fun onStart(index: Int) {
                        Log.d(TAG, "Iniciando compresión...")
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        val uri = path?.let { moveToCache(it) }
                        if (cont.isActive) cont.resume(uri)
                    }
                }
            )
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
    private fun moveToCache(path: String): Uri? {
        val originalFile = File(path)
        val cacheFolder = File(appContext.cacheDir, "CompressedVideos")
        if (!cacheFolder.exists()) cacheFolder.mkdirs()

        val cachedFile = File(cacheFolder, originalFile.name)
        return try {
            originalFile.copyTo(cachedFile, overwrite = true)
            originalFile.delete()
            Uri.fromFile(cachedFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileSize(uri: Uri): Long {
        return try {
            if (uri.scheme == "file") {
                File(uri.path!!).length()
            } else {
                appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.available().toLong()
                } ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }


    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${String.format("%.2f", bytes / (1024f * 1024f * 1024f))} GB"
            bytes >= 1024 * 1024 -> "${String.format("%.2f", bytes / (1024f * 1024f))} MB"
            bytes >= 1024 -> "${String.format("%.2f", bytes / 1024f)} KB"
            else -> "$bytes bytes"
        }
    }

    private fun createOutputPath(): String {
        val timestamp = System.currentTimeMillis()
        return "${appContext.cacheDir}/compressed_video_$timestamp.mp4"
    }
}
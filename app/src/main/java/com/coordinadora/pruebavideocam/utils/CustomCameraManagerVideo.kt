package com.coordinadora.pruebavideocam.utils

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

class CustomCameraManagerVideo(
    private val context: Context,
    private val previewView: PreviewView
) {
    var autoStopRunnable: Runnable? = null
    var handler: Handler? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private lateinit var outputFile: File
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var currentRecording: Recording? = null
    var onRecordingStarted: (() -> Unit)? = null
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture!!
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, executor)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(onVideoSaved: (Uri) -> Unit) {
        outputFile = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")

        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        currentRecording = videoCapture?.output
            ?.prepareRecording(context, outputOptions)
            ?.withAudioEnabled()
            ?.start(executor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Handler(Looper.getMainLooper()).post {
                            onRecordingStarted?.invoke()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        Handler(Looper.getMainLooper()).post {
                            onVideoSaved(Uri.fromFile(outputFile))
                        }
                        currentRecording = null
                    }
                    else -> {}
                }
            }
    }



    fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }
}

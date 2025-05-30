package com.coordinadora.pruebavideocam

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File


class CustomCameraDialogVideoFragment(private val onVideoCaptured: (Uri) -> Unit) :
    DialogFragment() {
    private lateinit var previewView: PreviewView
    private lateinit var imagePreview: ImageView
    private lateinit var btnCapture: ImageView
    private lateinit var btnAccept: ImageButton
    private lateinit var btnRetake: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnClose2: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnStop: ImageView
    private lateinit var videoView: VideoView
    private lateinit var actionButtons: LinearLayout
    private lateinit var customCameraManager: CustomCameraManagerVideo
    private lateinit var txtCounter: TextView
    private var capturedVideoUri: Uri? = null
    private var recordingStartTime = 0L
    private var elapsedTimeInSeconds = 0
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private val COUNTER = "00:00"
    private var playbackHandler: Handler? = null
    private var playbackRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    @SuppressLint("MissingPermission", "MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        imagePreview = view.findViewById(R.id.imagePreview)
        btnCapture = view.findViewById(R.id.btnCapture)
        btnAccept = view.findViewById(R.id.btnAccept)
        btnRetake = view.findViewById(R.id.btnRetake)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose2 = view.findViewById(R.id.btnClose2)
        btnBack = view.findViewById(R.id.btnBack)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnStop = view.findViewById(R.id.btnStop)
        btnPause = view.findViewById(R.id.btnPause)
        actionButtons = view.findViewById(R.id.actionButtons)
        videoView = view.findViewById(R.id.videoPreview)
        txtCounter = view.findViewById(R.id.txtCounter)
        txtCounter.text = COUNTER
        btnBack.visibility = View.VISIBLE
        btnClose.visibility = View.GONE
        btnCapture.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_double_circle_video)

        customCameraManager = CustomCameraManagerVideo(requireContext(), previewView)
        customCameraManager.startCamera(this)

        dialog!!.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                dismiss()
                true
            } else {
                false
            }
        }
        btnCapture.setOnClickListener {
            btnCapture.visibility = View.GONE
            btnBack.visibility = View.GONE
            btnStop.visibility = View.VISIBLE
            recordingStartTime = System.currentTimeMillis()
            startTimer()
            customCameraManager.startRecording { uri ->
                capturedVideoUri = uri
                actionButtons.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                videoView.setVideoURI(uri)
                videoView.seekTo(1000)
                videoView.visibility = View.VISIBLE
            }
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                previewView.visibility = View.GONE
                imagePreview.visibility = View.GONE
                actionButtons.visibility = View.VISIBLE
                btnStop.visibility = View.GONE
                btnBack.visibility = View.GONE
                btnPlay.visibility = View.VISIBLE
                customCameraManager.stopRecording()
                customCameraManager.autoStopRunnable?.let {
                    customCameraManager.handler!!.removeCallbacks(it)
                }
                stopTimer()
            }, 20_000)
        }

        btnStop.setOnClickListener {
            previewView.visibility = View.GONE
            imagePreview.visibility = View.GONE
            actionButtons.visibility = View.VISIBLE
            btnStop.visibility = View.GONE
            btnBack.visibility = View.GONE
            btnPlay.visibility = View.VISIBLE
            customCameraManager.autoStopRunnable?.let {
                customCameraManager.handler!!.removeCallbacks(it)
            }
            customCameraManager.stopRecording()
            stopTimer()
        }

        btnPlay.setOnClickListener {
            btnPause.visibility = View.VISIBLE
            btnPlay.visibility = View.GONE
            capturedVideoUri.let { uri ->
                videoView.start()
                startPlaybackTimer()
            }
        }

        btnPause.setOnClickListener {
            btnPlay.visibility = View.VISIBLE
            btnPause.visibility = View.GONE
            capturedVideoUri.let { uri ->
                videoView.pause()
                pausePlaybackTimer()
            }
        }

        btnRetake.setOnClickListener {
            previewView.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            actionButtons.visibility = View.GONE
            videoView.visibility = View.GONE
            btnCapture.visibility = View.VISIBLE
            btnBack.visibility = View.VISIBLE
            btnPlay.visibility = View.GONE
            btnPause.visibility = View.GONE
            txtCounter.text = COUNTER
            elapsedTimeInSeconds = 0
        }

        btnAccept.setOnClickListener {
            capturedVideoUri?.let {
                val fileSizeInBytes = getFileSizeFromUri(it)
                val fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0)

                val compressor = VideoCompressorManager(requireActivity())
                val compressionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                compressionScope.launch {
                    val compressedUri = compressor.compressVideoAsync(it)
                    if (compressedUri != null) {
                        val originalSize = compressor.getFileSize(it)
                        val compressedSize = compressor.getFileSize(compressedUri)
                        val reduction = if (originalSize > 0) {
                            String.format("%.1f", ((originalSize - compressedSize).toFloat() / originalSize.toFloat()) * 100)
                        } else "0.0"

                        Log.d("VideoCompression", "=== COMPRESIÓN COMPLETADA ===")
                        Log.d("VideoCompression", "Tamaño original: %.2f MB".format(fileSizeInMB))
                        Log.d(
                            "VideoCompression",
                            "Tamaño comprimido: ${compressor.formatFileSize(compressedSize)}"
                        )
                        Log.d("VideoCompression", "Reducción: $reduction%")
                        Log.d("VideoCompression", "URI comprimida: $compressedUri")
                        onVideoCaptured(compressedUri)
                    } else {
                        Log.e("VideoCompressor", "Falló la compresión")
                    }
                }
                dismiss()
            }
        }

        btnClose.setOnClickListener {
            dismiss()
        }

        btnClose2.setOnClickListener {
            dismiss()
        }

        btnBack.setOnClickListener {
            customCameraManager.stopRecording()
            dismiss()
        }
        return view
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            val file = File(uri.path!!)
            file.length()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun startPlaybackTimer() {
        pausePlaybackTimer()
        val videoDurationSec = videoView.duration / 1000
        playbackHandler = Handler(Looper.getMainLooper())
        playbackRunnable = object : Runnable {
            override fun run() {
                val currentPositionSec = videoView.currentPosition / 1000

                if (currentPositionSec <= videoDurationSec) {
                    val count =
                        if (currentPositionSec < 10) "0$currentPositionSec" else "$currentPositionSec"
                    txtCounter.text = "00:$count"
                    playbackHandler?.postDelayed(this, 1000)
                } else {
                    pausePlaybackTimer()
                }
            }
        }
        playbackHandler?.post(playbackRunnable!!)
    }


    private fun pausePlaybackTimer() {
        playbackHandler?.removeCallbacks(playbackRunnable!!)
    }


    private fun startTimer() {
        if (timerHandler != null && timerRunnable != null) return

        elapsedTimeInSeconds = 0
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                elapsedTimeInSeconds = ((currentTime - recordingStartTime) / 1000).toInt()

                val count =
                    if (elapsedTimeInSeconds < 10) "0$elapsedTimeInSeconds" else "$elapsedTimeInSeconds"
                txtCounter.text = "00:$count"

                timerHandler?.postDelayed(this, 1000)
            }
        }
        timerHandler?.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
        timerRunnable = null
        timerHandler = null

        val stopTime = System.currentTimeMillis()
        elapsedTimeInSeconds = ((stopTime - recordingStartTime) / 1000).toInt()

        val count =
            if (elapsedTimeInSeconds < 10) "0$elapsedTimeInSeconds" else "$elapsedTimeInSeconds"
        txtCounter.text = "00:$count"
    }


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

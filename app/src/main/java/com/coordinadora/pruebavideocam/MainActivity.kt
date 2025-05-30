package com.coordinadora.pruebavideocam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        FirebaseApp.initializeApp(this)
        val btnCaptureVideo = findViewById<MaterialButton>(R.id.btnCaptureVideo)
        val btnNav = findViewById<MaterialButton>(R.id.btnNav)
        btnCaptureVideo.setOnClickListener {
            CustomCameraDialogVideoFragment {
                    uri->
                Log.d("URL",uri.toString())
                val rutaProcesada = uri.toString().removePrefix("file://")
                val fecha = obtenerFechaHora("yyyy-MM-dd")
                subirVideoAFirebaseStorage(
                    pathLocal = rutaProcesada,
                    nombreEnStorage = "video_qa_${fecha}.mp4",
                    onSuccess = {
                        Log.d("Firebase", "Video subido exitosamente")
                        clearCacheFiles(uri.toString())
                    },
                    onFailure = { e ->
                        Log.e("Firebase", "Error al subir video: ${e.message}")
                    }
                )
            }.show(supportFragmentManager,"video")
        }
        btnNav.setOnClickListener {
            val intent = Intent(this, Page2::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    fun clearCacheFiles(uri: String) {
        val convert = Uri.parse(uri)
        val file = File(convert.path ?: return)
        if (file.exists()) {
            file.delete()
        }
    }

    fun obtenerFechaHora(format: String): String {
        val date = Timestamp.now().toDate()
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun subirVideoAFirebaseStorage(pathLocal: String, nombreEnStorage: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val file = File(pathLocal)
        if (!file.exists()) {
            onFailure(Exception("El archivo no existe en: $pathLocal"))
            return
        }

        val uri = Uri.fromFile(file)

        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val videoRef = storageRef.child("videos/$nombreEnStorage")

        val uploadTask = videoRef.putFile(uri)

        uploadTask
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun requestPermissions() {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                2
            )
        }
    }
}
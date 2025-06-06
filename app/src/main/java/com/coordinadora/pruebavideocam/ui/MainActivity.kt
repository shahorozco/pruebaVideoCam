package com.coordinadora.pruebavideocam.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.coordinadora.pruebavideocam.utils.CustomCameraDialogVideoFragment
import com.coordinadora.pruebavideocam.R
import com.coordinadora.pruebavideocam.application.dagger.PruebasCamApplication
import com.coordinadora.pruebavideocam.database.AppDatabase
import com.coordinadora.pruebavideocam.database.entity.Globals
import com.coordinadora.pruebavideocam.utils.Connectivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var dbLocal: AppDatabase
    @Inject
    lateinit var connectivity: Connectivity
    @Inject
    lateinit var pruebasCamApplication: PruebasCamApplication
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
        (applicationContext as PruebasCamApplication).getPruebasCamComponent().inject(this)
        requestPermissions()
        FirebaseApp.initializeApp(this)

        val btnCaptureVideo = findViewById<MaterialButton>(R.id.btnCaptureVideo)
        val btnNav = findViewById<MaterialButton>(R.id.btnNav)
        val btnSync = findViewById<MaterialButton>(R.id.btnSync)
        btnCaptureVideo.setOnClickListener {
            CustomCameraDialogVideoFragment {
                    uri->
                Log.d("URL",uri.toString())
                if(connectivity.checkForInternetData(pruebasCamApplication.applicationContext)){
                    guardarFirebase(uri.toString())
                }else{
                    dbLocal.globalDao()!!.insertAll(Globals("video",uri.toString()))
                }
            }.show(supportFragmentManager,"video")
        }
        btnNav.setOnClickListener {
            val intent = Intent(this, Page2::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        btnSync.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val video = dbLocal.globalDao()!!.getValue("video")
                if (video != "") {
                    guardarFirebase(video)
                } else {
                    launch(Dispatchers.Main) {
                        toastPersonalizado("No hay video disponible para sincronizar")
                    }
                }
            }
        }
    }

    private fun toastPersonalizado(mensaje: String){
        val toast = Toast(pruebasCamApplication.applicationContext)
        val layout = LayoutInflater.from( pruebasCamApplication.applicationContext).inflate(R.layout.custom_toast, null)

        layout.findViewById<TextView>(R.id.toast_text).text = mensaje

        toast.view = layout
        toast.duration = Toast.LENGTH_SHORT
        toast.show()
    }

    fun guardarFirebase(ruta: String){
        val rutaProcesada = ruta.removePrefix("file://")
        val fecha = obtenerFechaHora("ddMMyyyyHHmmss")
        subirVideoAFirebaseStorage(
            pathLocal = rutaProcesada,
            nombreEnStorage = "video_qa_${fecha}.mp4",
            onSuccess = {
                Log.d("Firebase", "Video subido exitosamente")
                clearCacheFiles(ruta)
                Toast.makeText(pruebasCamApplication.applicationContext,"Video sincronizado en Storage",Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(pruebasCamApplication.applicationContext,"Error al subir el video",Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error al subir video: ${e.message}")
            }
        )
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
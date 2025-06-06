package com.coordinadora.pruebavideocam.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coordinadora.pruebavideocam.R
import com.coordinadora.pruebavideocam.application.dagger.PruebasCamApplication
import com.google.android.material.button.MaterialButton

class Page2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page2)
        (applicationContext as PruebasCamApplication).getPruebasCamComponent().inject(this)

        val btnNav2 = findViewById<MaterialButton>(R.id.btnNav2)
        btnNav2.setOnClickListener {
            val intent = Intent(this, Page3::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
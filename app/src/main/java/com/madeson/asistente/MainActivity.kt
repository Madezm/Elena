package com.madeson.asistente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var texto: TextView

    private val voz = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val frases = resultado.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val frase = frases?.firstOrNull()?.lowercase(Locale.getDefault())
        if (frase != null) {
            texto.text = frase
            Comandos.ejecutar(this, frase)
        }
    }

    private val permisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidos ->
        if (concedidos[Manifest.permission.RECORD_AUDIO] == true) {
            arrancarServicio()
            escuchar()
        } else {
            Toast.makeText(this, "Necesito el micrófono", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        texto = findViewById(R.id.texto)
        findViewById<Button>(R.id.boton).setOnClickListener { iniciar() }
        if (debeEscuchar(intent)) {
            iniciar()
        } else {
            arrancarServicio()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (debeEscuchar(intent)) iniciar()
    }

    private fun debeEscuchar(i: Intent?): Boolean {
        if (i == null) return false
        return i.getBooleanExtra("escuchar", false) ||
            i.getBooleanExtra("escuchar_al_abrir", false) ||
            i.action == Intent.ACTION_ASSIST
    }

    private fun arrancarServicio() {
        val tieneMicro = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!tieneMicro) return
        try {
            ContextCompat.startForegroundService(this, Intent(this, EscuchaElena::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "No pude activar Hey Elena", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciar() {
        val lista = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= 33) {
            lista.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val faltan = lista.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltan.isEmpty()) escuchar()
        else permisos.launch(faltan.toTypedArray())
    }

    private fun escuchar() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué hacer")
        }
        voz.launch(intent)
    }
}

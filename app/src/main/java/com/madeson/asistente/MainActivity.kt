package com.madeson.asistente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
            procesar(frase)
        }
    }

    private val permisoMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) escuchar()
        else Toast.makeText(this, "Necesito el micrófono", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        texto = findViewById(R.id.texto)
        findViewById<Button>(R.id.boton).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) escuchar() else permisoMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun escuchar() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime qué app abrir")
        }
        voz.launch(intent)
    }

    private fun procesar(frase: String) {
        if (!frase.startsWith("abre ")) {
            Toast.makeText(this, "Di: abre y el nombre de la app", Toast.LENGTH_SHORT).show()
            return
        }
        abrirApp(frase.removePrefix("abre ").trim())
    }

    private fun abrirApp(nombre: String) {
        val pm = packageManager
        val lanzador = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val app = pm.queryIntentActivities(lanzador, 0).firstOrNull {
            it.loadLabel(pm).toString().lowercase(Locale.getDefault()).contains(nombre)
        }
        val abrir = app?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (abrir != null) startActivity(abrir)
        else Toast.makeText(this, "No encontré $nombre", Toast.LENGTH_SHORT).show()
    }
}

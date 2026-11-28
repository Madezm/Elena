
package com.madeson.asistente

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

class EscuchaElena : Service(), RecognitionListener {

    private var servicio: SpeechService? = null
    private var modelo: Model? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        crearCanal()
        mostrarNotificacion("Preparando a Elena...")
        if (servicio == null) {
            Thread { prepararModelo() }.start()
        }
        return START_STICKY
    }

    private fun mostrarNotificacion(texto: String) {
        val n = construirNotificacion(texto)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, n)
        }
    }

    private fun prepararModelo() {
        try {
            val carpeta = File(filesDir, "modelo-es")
            if (carpeta.listFiles()?.firstOrNull { it.isDirectory } == null) {
                mostrarNotificacion("Descargando voz en español...")
                descargar(carpeta)
            }
            val raiz = carpeta.listFiles()?.firstOrNull { it.isDirectory } ?: carpeta
            modelo = Model(raiz.absolutePath)
            empezarAEscuchar()
        } catch (e: Exception) {
            mostrarNotificacion("No pude preparar la voz. Revisa internet")
        }
    }

    private fun descargar(destino: File) {
        destino.deleteRecursively()
        destino.mkdirs()
        val url = URL("https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip")
        ZipInputStream(url.openStream().buffered()).use { zip ->
            var entrada = zip.nextEntry
            while (entrada != null) {
                val archivo = File(destino, entrada.name)
                if (entrada.isDirectory) {
                    archivo.mkdirs()
                } else {
                    archivo.parentFile?.mkdirs()
                    archivo.outputStream().use { salida -> zip.copyTo(salida) }
                }
                entrada = zip.nextEntry
            }
        }
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= 26) {
            val canal = NotificationChannel(
                "elena",
                "Elena escuchando",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun construirNotificacion(texto: String): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "elena")
        } else {
            Notification.Builder(this)
        }
        return b.setContentTitle("Elena")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun empezarAEscuchar() {
        val m = modelo ?: return
        mostrarNotificacion("Di: Hey Elena")
        val reconocedor = Recognizer(m, 16000.0f, "[\"hey elena\", \"[unk]\"]")
        servicio = SpeechService(reconocedor, 16000.0f)
        servicio?.startListening(this)
    }

    private fun despertar() {
        servicio?.stop()
        servicio = null
        val i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        i.putExtra("escuchar", true)
        startActivity(i)
        Thread {
            Thread.sleep(8000)
            empezarAEscuchar()
        }.start()
    }

    override fun onPartialResult(hypothesis: String?) {
        if (hypothesis != null && hypothesis.contains("hey elena")) despertar()
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis != null && hypothesis.contains("hey elena")) despertar()
    }

    override fun onFinalResult(hypothesis: String?) {}

    override fun onError(exception: Exception?) {}

    override fun onTimeout() {}

    override fun onDestroy() {
        servicio?.stop()
        servicio?.shutdown()
        super.onDestroy()
    }
}

package com.madeson.asistente

import android.content.Intent
import android.speech.RecognitionService

class AsistenteRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent?, callback: Callback?) {}
    override fun onCancel(callback: Callback?) {}
    override fun onStopListening(callback: Callback?) {}
}

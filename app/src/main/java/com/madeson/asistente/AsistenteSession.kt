package com.madeson.asistente

import android.content.Context
import android.content.Intent
import android.service.voice.VoiceInteractionSession

class AsistenteSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: android.os.Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("escuchar_al_abrir", true)
        context.startActivity(intent)
        hide()
    }
}

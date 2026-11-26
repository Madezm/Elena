package com.madeson.asistente

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class LlamadaAccesibilidad : AccessibilityService() {

    companion object {
        @Volatile var quiereLlamar: Boolean = false
        @Volatile var limite: Long = 0L

        fun pedirLlamada() {
            quiereLlamar = true
            limite = System.currentTimeMillis() + 20000
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!quiereLlamar) return
        if (System.currentTimeMillis() > limite) {
            quiereLlamar = false
            return
        }
        if (event?.packageName?.toString() != "com.whatsapp") return
        val raiz = rootInActiveWindow ?: return
        val boton = buscar(raiz)
        if (boton != null) {
            quiereLlamar = false
            clicar(boton)
        }
    }

    private fun esLlamadaDeVoz(nodo: AccessibilityNodeInfo): Boolean {
        val desc = nodo.contentDescription?.toString()?.lowercase() ?: ""
        val id = nodo.viewIdResourceName?.lowercase() ?: ""
        if (desc.contains("video") || desc.contains("vídeo") || id.contains("video")) return false
        if (desc.contains("llamada") || desc.contains("llamar") || desc.contains("voz")) return true
        if (desc.contains("call") || desc.contains("voice")) return true
        if (id.contains("voip_call") || id.contains("audio_call") || id.contains("voice_call")) return true
        return false
    }

    private fun buscar(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (esLlamadaDeVoz(nodo)) return nodo
        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            val encontrado = buscar(hijo)
            if (encontrado != null) return encontrado
        }
        return null
    }

    private fun clicar(nodo: AccessibilityNodeInfo) {
        var actual: AccessibilityNodeInfo? = nodo
        while (actual != null && !actual.isClickable) {
            actual = actual.parent
        }
        actual?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override fun onInterrupt() {}
}

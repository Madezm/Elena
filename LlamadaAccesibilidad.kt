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
            limite = System.currentTimeMillis() + 15000
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

    private fun buscar(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = nodo.contentDescription?.toString()?.lowercase() ?: ""
        if (desc.contains("llamada de voz") || desc.contains("voice call")) return nodo
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

package com.madeson.asistente

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale

object Comandos {

    fun ejecutar(contexto: Context, fraseOriginal: String) {
        val frase = fraseOriginal.lowercase(Locale.getDefault()).trim()

        val busqueda = extraerBusqueda(frase)
        if (busqueda != null) {
            if (frase.contains("youtube")) {
                buscarEnYouTube(contexto, busqueda.replace(" en youtube", "").trim())
            } else {
                buscarEnGoogle(contexto, busqueda)
            }
            return
        }

        val contacto = extraerContacto(frase)
        if (contacto != null) {
            llamarPorWhatsApp(contexto, contacto)
            return
        }

        val app = extraerApp(frase)
        if (app != null) {
            abrirApp(contexto, app)
            return
        }

        aviso(contexto, "No entendí. Prueba: abre WhatsApp, búscame algo, o llama a alguien")
    }

    private fun extraerBusqueda(frase: String): String? {
        val marcas = listOf("búscame ", "busca ", "buscar ", "búsqueme ")
        for (m in marcas) {
            val i = frase.indexOf(m)
            if (i >= 0) {
                val texto = frase.substring(i + m.length).trim()
                if (texto.isNotEmpty()) return texto
            }
        }
        return null
    }

    private fun extraerContacto(frase: String): String? {
        val marcas = listOf("llama a ", "llamar a ", "llamada a ", "llámale a ")
        for (m in marcas) {
            val i = frase.indexOf(m)
            if (i >= 0) {
                val nombre = frase.substring(i + m.length)
                    .replace(" por whatsapp", "")
                    .replace(" en whatsapp", "")
                    .trim()
                if (nombre.isNotEmpty()) return nombre
            }
        }
        return null
    }

    private fun extraerApp(frase: String): String? {
        val marcas = listOf("abre ", "abrir ", "abra ", "ejecuta ")
        for (m in marcas) {
            val i = frase.indexOf(m)
            if (i >= 0) {
                return frase.substring(i + m.length)
                    .replace("la aplicación de ", "")
                    .replace("la aplicación ", "")
                    .replace("la app de ", "")
                    .replace("la app ", "")
                    .replace("el ", "")
                    .replace("la ", "")
                    .trim()
            }
        }
        return null
    }

    private fun abrirApp(contexto: Context, nombre: String) {
        val pm = contexto.packageManager
        val lanzador = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val app = pm.queryIntentActivities(lanzador, 0).firstOrNull {
            it.loadLabel(pm).toString().lowercase(Locale.getDefault()).contains(nombre)
        }
        val abrir = app?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (abrir != null) contexto.startActivity(abrir)
        else aviso(contexto, "No encontré $nombre")
    }

    private fun buscarEnGoogle(contexto: Context, texto: String) {
        val q = URLEncoder.encode(texto, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contexto.startActivity(intent)
    }

    private fun buscarEnYouTube(contexto: Context, texto: String) {
        val q = URLEncoder.encode(texto, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contexto.startActivity(intent)
    }

    private fun llamarPorWhatsApp(contexto: Context, nombre: String) {
        val numero = buscarNumero(contexto, nombre)
        if (numero == null) {
            aviso(contexto, "No encontré a $nombre en tus contactos")
            return
        }
        val limpio = numero.filter { it.isDigit() }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$limpio"))
        intent.setPackage("com.whatsapp")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            contexto.startActivity(intent)
            aviso(contexto, "Chat de $nombre listo. Toca el teléfono para llamar")
        } catch (e: Exception) {
            aviso(contexto, "No pude abrir WhatsApp")
        }
    }

    private fun buscarNumero(contexto: Context, nombre: String): String? {
        val cursor = contexto.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        ) ?: return null
        cursor.use {
            while (it.moveToNext()) {
                val n = it.getString(0)?.lowercase(Locale.getDefault()) ?: continue
                if (n.contains(nombre)) return it.getString(1)
            }
        }
        return null
    }

    private fun aviso(contexto: Context, texto: String) {
        Toast.makeText(contexto, texto, Toast.LENGTH_LONG).show()
    }
}

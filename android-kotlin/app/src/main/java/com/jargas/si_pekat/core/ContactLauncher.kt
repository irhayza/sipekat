package com.jargas.si_pekat.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Helper aksi kontak & lokasi yang dipakai semua modul (telepon, WhatsApp, peta, bagikan lokasi).
 * Port dari utils/contact_launcher.dart.
 */
object ContactLauncher {
    /** Normalisasi nomor Indonesia ke format internasional tanpa '+': "0812-3456-7890" → "6281234567890". */
    fun normalizePhone(raw: String?): String? {
        if (raw == null) return null
        var digits = raw.replace(Regex("[^0-9]"), "")
        if (digits.isEmpty()) return null
        digits = when {
            digits.startsWith("0") -> "62" + digits.substring(1)
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
        if (digits.length < 9) return null
        return digits
    }

    fun hasValidPhone(raw: String?): Boolean = normalizePhone(raw) != null

    fun callPhone(context: Context, raw: String?): Boolean {
        val normalized = normalizePhone(raw) ?: return false
        return launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized")))
    }

    fun openWhatsApp(context: Context, raw: String?, message: String? = null): Boolean {
        val normalized = normalizePhone(raw) ?: return false
        val text = if (!message.isNullOrEmpty()) "?text=${Uri.encode(message)}" else ""
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized$text")))
    }

    fun openMap(context: Context, lat: Double, lng: Double): Boolean =
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))

    fun shareLocationViaWhatsApp(context: Context, lat: Double, lng: Double, toPhone: String? = null, label: String? = null): Boolean {
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
        val text = if (!label.isNullOrEmpty()) "Lokasi $label: $mapsUrl" else "Lokasi: $mapsUrl"
        if (toPhone != null && hasValidPhone(toPhone)) return openWhatsApp(context, toPhone, text)
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(text)}")))
    }

    fun showLaunchFailure(context: Context, what: String) {
        Toast.makeText(context, "Tidak bisa membuka $what di perangkat ini.", Toast.LENGTH_SHORT).show()
    }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

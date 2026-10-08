package com.jargas.si_pekat.core

import android.util.Base64
import com.jargas.si_pekat.data.KunjunganLocalCache
import com.jargas.si_pekat.data.OcrLocalDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Foto laporan Kunjungan/Pembukaan yang menunggu dikirim (antrean offline). Disimpan sebagai BERKAS di filesDir
 * (tahan lama, tidak dihapus sistem seperti cache) — bukan sebagai teks base64 di SharedPreferences, yang membengkak
 * dan memperlambat pembukaan aplikasi bila antrean banyak.
 */
object PendingPhotos {
    private fun dir(): File = File(AppContext.app.filesDir, "pending_photos").apply { mkdirs() }
    private fun safe(s: String) = s.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "x" }

    /** Simpan data-URI / base64 JPEG ke berkas dan kembalikan path-nya. */
    fun saveBase64(idpel: String, tag: String, base64: String): String {
        val bytes = Base64.decode(base64.substringAfter("base64,", base64), Base64.DEFAULT)
        val f = File(dir(), "${safe(idpel)}_${tag}_${System.currentTimeMillis()}.jpg")
        f.writeBytes(bytes)
        return f.absolutePath
    }

    fun exists(path: String?): Boolean = !path.isNullOrEmpty() && File(path).exists()

    /** Baca kembali sebagai data-URI base64 siap unggah. */
    fun readDataUri(path: String): String = "data:image/jpeg;base64," + Base64.encodeToString(File(path).readBytes(), Base64.NO_WRAP)

    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        try { File(path).delete() } catch (_: Exception) {}
    }
}

/** Pembersihan berkas foto sementara & yatim; dijalankan saat aplikasi dibuka. */
object PhotoCache {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    suspend fun purge() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        fun old(f: File) = now - f.lastModified() > DAY_MS
        try {
            // Foto kerja/kamera di cache: aman dihapus setelah sehari.
            File(AppContext.app.cacheDir, "photos").listFiles()?.filter(::old)?.forEach { it.delete() }

            // Foto antrean Kunjungan yang tidak lagi dirujuk antrean mana pun.
            val pendingRefs = KunjunganLocalCache.getPendingSubmissions()
                .flatMap { listOf(it["fotoSebPath"], it["fotoSesPath"]) }.mapNotNull { it?.toString() }.toSet()
            File(AppContext.app.filesDir, "pending_photos").listFiles()
                ?.filter { it.absolutePath !in pendingRefs && old(it) }?.forEach { it.delete() }

            // Foto bukti meter yang barisnya sudah terkirim/dihapus.
            val meterRefs = OcrLocalDb.getUnsynced().mapNotNull { it.fotoPath }.toSet()
            File(AppContext.app.filesDir, "meter_pending").listFiles()
                ?.filter { it.absolutePath !in meterRefs && old(it) }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
        Unit
    }
}

package com.jargas.si_pekat.data

import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.PendingPhotos
import com.jargas.si_pekat.core.SessionStore
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList
import com.jargas.si_pekat.model.VisitCustomer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Cache offline modul Kunjungan/Pembukaan: daftar pelanggan terakhir, antrean laporan pending (belum terkirim),
 * dan riwayat laporan. Port dari kunjungan_local_cache_service.dart (kunci & bentuk data sama).
 */
object KunjunganLocalCache {
    private const val KEY_VISIT = "cached_visit_list"
    private const val KEY_REOPEN = "cached_reopen_list"
    private const val KEY_PENDING = "pending_submissions"
    private const val KEY_HISTORY = "history_submissions"

    private val prefs get() = SessionStore.prefs

    // ── Daftar pelanggan ────────────────────────────────────────────────────
    private suspend fun saveList(key: String, list: List<VisitCustomer>) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, Json.encode(list.map { it.toMap() })).apply()
    }

    private suspend fun getList(key: String): List<VisitCustomer> = withContext(Dispatchers.IO) {
        val s = prefs.getString(key, null)
        if (s.isNullOrEmpty()) return@withContext emptyList()
        try {
            Json.decode(s).asList()?.mapNotNull { it.asJsonMap()?.let(VisitCustomer.Companion::fromMap) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveVisitList(list: List<VisitCustomer>) = saveList(KEY_VISIT, list)
    suspend fun saveReopenList(list: List<VisitCustomer>) = saveList(KEY_REOPEN, list)
    suspend fun getCachedVisitList() = getList(KEY_VISIT)
    suspend fun getCachedReopenList() = getList(KEY_REOPEN)

    suspend fun removeCustomerFromCache(idpel: String) {
        saveVisitList(getCachedVisitList().filter { it.idpel != idpel })
        saveReopenList(getCachedReopenList().filter { it.idpel != idpel })
    }

    // ── Peta-peta (pending & riwayat) ───────────────────────────────────────
    private suspend fun getMaps(key: String): List<MutableMap<String, Any?>> = withContext(Dispatchers.IO) {
        val s = prefs.getString(key, null)
        if (s.isNullOrEmpty()) return@withContext emptyList()
        try {
            Json.decode(s).asList()?.mapNotNull { it.asJsonMap()?.toMutableMap() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun putMaps(key: String, list: List<JsonMap>) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, Json.encode(list)).apply()
    }

    // ── Antrean pending ─────────────────────────────────────────────────────
    suspend fun getPendingSubmissions(): List<MutableMap<String, Any?>> = getMaps(KEY_PENDING)

    private val PHOTO_KEYS = listOf("fotoSebBase64" to "seb", "fotoSesBase64" to "ses")

    private fun photoPaths(m: Map<String, Any?>) = listOf("fotoSebPath", "fotoSesPath").mapNotNull { m[it]?.toString() }

    /**
     * Simpan laporan pending; satu pelanggan hanya punya satu entri (yang baru menimpa yang lama).
     * Foto (base64) dipindah ke berkas di filesDir; antrean hanya menyimpan path-nya.
     */
    suspend fun savePendingSubmission(submission: JsonMap) {
        val idpel = submission["idpel"]?.toString()
        val entry = submission.toMutableMap()
        withContext(Dispatchers.IO) {
            for ((key, tag) in PHOTO_KEYS) {
                val v = entry[key]?.toString().orEmpty()
                if (v.isNotEmpty() && !v.startsWith("http")) {
                    entry[key.replace("Base64", "Path")] = PendingPhotos.saveBase64(idpel ?: "x", tag, v)
                    entry.remove(key)
                }
            }
        }
        val existing = getMaps(KEY_PENDING)
        existing.filter { it["idpel"]?.toString() == idpel }.forEach { old -> photoPaths(old).forEach(PendingPhotos::delete) }
        putMaps(KEY_PENDING, existing.filter { it["idpel"]?.toString() != idpel } + entry)
    }

    suspend fun removePendingSubmission(idpel: String) {
        val existing = getMaps(KEY_PENDING)
        existing.filter { it["idpel"]?.toString() == idpel }.forEach { old -> photoPaths(old).forEach(PendingPhotos::delete) }
        putMaps(KEY_PENDING, existing.filter { it["idpel"]?.toString() != idpel })
    }

    // ── Riwayat ─────────────────────────────────────────────────────────────
    private val ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS"

    suspend fun saveHistorySubmission(submission: MutableMap<String, Any?>) {
        val idpel = submission["idpel"]?.toString()
        // Riwayat tidak butuh foto mentah: buang base64 (yang berupa URL Drive tetap disimpan) agar prefs tetap kecil.
        for ((key, _) in PHOTO_KEYS) {
            if (submission[key]?.toString()?.startsWith("http") != true) submission.remove(key)
        }
        if (!submission.containsKey("timestamp")) submission["timestamp"] = SimpleDateFormat(ISO, Locale.US).format(System.currentTimeMillis())
        val list = getMaps(KEY_HISTORY).filter { it["idpel"]?.toString() != idpel } + submission
        putMaps(KEY_HISTORY, list)
    }

    suspend fun getHistorySubmissions(): List<MutableMap<String, Any?>> =
        getMaps(KEY_HISTORY).sortedByDescending { it["timestamp"]?.toString() ?: "" }

    suspend fun removeHistorySubmission(idpel: String) {
        putMaps(KEY_HISTORY, getMaps(KEY_HISTORY).filter { it["idpel"]?.toString() != idpel })
    }

    suspend fun updateHistorySubmissionStatus(idpel: String, status: String) {
        val item = getHistorySubmissions().firstOrNull { it["idpel"]?.toString() == idpel } ?: return
        item["status"] = status
        saveHistorySubmission(item)
    }
}

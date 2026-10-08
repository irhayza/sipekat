package com.jargas.si_pekat.data

import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.SessionStore
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList
import com.jargas.si_pekat.model.MeterReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Riwayat laporan Pengaduan & Perbaikan yang berhasil dikirim dari perangkat ini (terbaru di atas, maks. 100). */
object ActivityHistory {
    enum class Kind(val key: String) { PENGADUAN("history_pengaduan"), PERBAIKAN("history_perbaikan") }

    private const val MAX = 100
    private val prefs get() = SessionStore.prefs

    suspend fun list(kind: Kind): List<JsonMap> = withContext(Dispatchers.IO) {
        val s = prefs.getString(kind.key, null)
        if (s.isNullOrEmpty()) return@withContext emptyList()
        try { Json.decode(s).asList()?.mapNotNull { it.asJsonMap() } ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    /** [id] unik (nomor tiket); entri dengan id sama diganti. */
    suspend fun add(kind: Kind, id: String, nama: String, idpel: String, title: String, status: String) {
        val item: JsonMap = mapOf(
            "id" to id, "nama" to nama, "idpel" to idpel, "title" to title, "status" to status,
            "timestamp" to MeterReading.formatIso(System.currentTimeMillis()),
        )
        val next = (listOf(item) + list(kind).filter { it["id"]?.toString() != id }).take(MAX)
        withContext(Dispatchers.IO) { prefs.edit().putString(kind.key, Json.encode(next)).apply() }
    }

    suspend fun remove(kind: Kind, id: String) {
        val next = list(kind).filter { it["id"]?.toString() != id }
        withContext(Dispatchers.IO) { prefs.edit().putString(kind.key, Json.encode(next)).apply() }
    }
}

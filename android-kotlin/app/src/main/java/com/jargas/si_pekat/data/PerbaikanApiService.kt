package com.jargas.si_pekat.data

import com.google.gson.JsonParseException
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList
import com.jargas.si_pekat.model.DropdownOptions
import com.jargas.si_pekat.model.Officer
import com.jargas.si_pekat.model.Ticket
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException

/** Port dari perbaikan_api_service.dart. */
object PerbaikanApiService {
    // ── Cache dalam memori ──────────────────────────────────────────────────
    private const val DROPDOWN_TTL_MS = 60L * 60 * 1000 // dropdown jarang berubah

    private var dropdownCache: DropdownOptions? = null
    private var dropdownCachedAt = 0L

    /** Invalidasi semua cache (dipanggil setelah submit berhasil). */
    fun clearCache() {
        dropdownCache = null
        dropdownCachedAt = 0L
    }

    /** Respons berupa array dibungkus sebagai {'_list': [...]} agar semua jawaban bertipe Map. */
    private suspend fun post(body: Map<String, Any?>, timeoutSec: Int): JsonMap {
        val res = try {
            GasHttp.postJson(AppConfig.REPAIR_BRIDGE_URL, body, timeoutSec)
        } catch (e: InterruptedIOException) {
            throw ApiException("Server tidak merespon. Periksa koneksi internet Anda lalu coba lagi.")
        } catch (e: IOException) {
            // Dart membedakan SocketException vs ClientException; di OkHttp keduanya IOException.
            if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                throw ApiException("Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.")
            }
            throw ApiException("Gagal terhubung ke server. Coba lagi beberapa saat.")
        }

        if (res.code != 200) throw ApiException("Server merespon dengan status ${res.code}. Coba lagi.")

        val decoded = try {
            Json.decode(res.body)
        } catch (e: JsonParseException) {
            throw ApiException("Server mengalami gangguan saat memproses permintaan. Coba lagi.")
        }
        decoded.asList()?.let { return mapOf("_list" to it) }
        return decoded.asJsonMap() ?: throw ApiException("Format respons server tidak dikenali.")
    }

    suspend fun getDropdownOptions(forceRefresh: Boolean = false): DropdownOptions {
        val now = System.currentTimeMillis()
        val cached = dropdownCache
        if (!forceRefresh && cached != null && now - dropdownCachedAt < DROPDOWN_TTL_MS) return cached

        val data = post(mapOf("action" to "get_dropdown_options"), AppConfig.CONNECTION_TIMEOUT_SEC)
        val officers = (data["petugas"].asList() ?: emptyList<Any?>()).map { p ->
            p.asJsonMap()?.let(Officer.Companion::fromMap) ?: Officer(nama = p.toString(), area = "")
        }
        val fresh = DropdownOptions(
            petugas = officers,
            jenis = (data["jenis"].asList() ?: emptyList<Any?>()).map { it.toString() },
            penanganan = (data["penanganan"].asList() ?: emptyList<Any?>()).map { it.toString() },
        )
        dropdownCache = fresh
        dropdownCachedAt = now
        return fresh
    }

    // ═══ Endpoint generik (find_row / update_row_cells / upload_photo / readSheet) ═══

    /** Cari satu baris di [sheetName] berdasarkan [keyValue] pada kolom [keyColumn] (mis. "Ticket"). */
    suspend fun findRow(sheetName: String, keyValue: String, keyColumn: String? = null): JsonMap {
        val body = buildMap<String, Any?> {
            put("action", "find_row")
            put("sheetName", sheetName)
            put("idpel", keyValue)
            if (keyColumn != null) put("keyColumn", keyColumn)
        }
        val data = post(body, AppConfig.CONNECTION_TIMEOUT_SEC)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Data tidak ditemukan.")
        return data["row"].asJsonMap() ?: emptyMap()
    }

    suspend fun updateRowCells(
        sheetName: String,
        keyValue: String,
        updates: Map<String, Any?>,
        keyColumn: String? = null,
        filterColumn: String? = null,
        filterValue: String? = null,
        required: Collection<String> = emptyList(),
    ): List<String> {
        val body = buildMap<String, Any?> {
            put("action", "update_row_cells")
            put("sheetName", sheetName)
            put("idpel", keyValue)
            if (keyColumn != null) put("keyColumn", keyColumn)
            if (filterColumn != null) put("filterColumn", filterColumn)
            if (filterValue != null) put("filterValue", filterValue)
            put("updates", updates)
        }
        val data = post(body, AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Gagal menyimpan perubahan ke server.")
        val skipped = (data["skipped"].asList() ?: emptyList<Any?>()).map { it.toString() }
        return SheetColumns.missing(skipped, required)
    }

    /** Upload satu foto base64 ke folder Drive khusus Perbaikan dan kembalikan URL-nya. */
    suspend fun uploadPhoto(base64: String, filename: String): String {
        val data = post(
            mapOf("action" to "upload_photo", "base64" to base64, "filename" to filename, "target" to "perbaikan"),
            AppConfig.UPLOAD_TIMEOUT_SEC,
        )
        if (data["status"] != "success" || data["url"] == null) {
            throw ServerException(data["message"]?.toString() ?: "Gagal upload foto ke server.")
        }
        return data["url"].toString()
    }

    /** Notifikasi WA/Telegram "pengaduan selesai" — best-effort: kegagalan tidak boleh menggagalkan submit. */
    suspend fun notifySelesai(
        ticket: String,
        idpel: String,
        nama: String,
        pengaduan: String,
        petugas: String,
        tindakan: String,
        fotoSeb: String? = null,
        fotoSes: String? = null,
        lat: String? = null,
        lng: String? = null,
    ) {
        try {
            val body = buildMap<String, Any?> {
                put("action", "notify_selesai")
                put("ticket", ticket)
                put("idpel", idpel)
                put("nama", nama)
                put("pengaduan", pengaduan)
                put("petugas", petugas)
                put("tindakan", tindakan)
                if (fotoSeb != null) put("fotoSeb", fotoSeb)
                if (fotoSes != null) put("fotoSes", fotoSes)
                if (lat != null) put("lat", lat)
                if (lng != null) put("lng", lng)
            }
            post(body, AppConfig.CONNECTION_TIMEOUT_SEC)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sengaja diabaikan.
        }
    }
}

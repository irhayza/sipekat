package com.sipekat.app.data.repository

import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.model.DropdownOptions
import com.sipekat.app.data.model.Officer
import com.sipekat.app.data.model.Ticket
import com.sipekat.app.data.remote.ApiException
import com.sipekat.app.data.remote.HttpClient
import com.sipekat.app.data.remote.ServerException

class PerbaikanRepository {

    private suspend fun post(body: Map<String, Any?>): Map<String, Any?> =
        HttpClient.postJson(url = AppConfig.REPAIR_BRIDGE_URL, body = body)

    // ── Dropdown options ──────────────────────────────────────────────
    suspend fun getDropdownOptions(): DropdownOptions {
        val data = post(mapOf("action" to "get_dropdown_options"))
        val petugasRaw  = data["petugas"]    as? List<*> ?: emptyList<Any>()
        val jenisRaw    = data["jenis"]      as? List<*> ?: emptyList<Any>()
        val penangRaw   = data["penanganan"] as? List<*> ?: emptyList<Any>()
        return DropdownOptions(
            petugas = petugasRaw.mapNotNull { p ->
                @Suppress("UNCHECKED_CAST")
                when (p) {
                    is Map<*, *> -> { val m = p as Map<String, Any?>; Officer(m["nama"]?.toString() ?: "", m["area"]?.toString() ?: "") }
                    else         -> Officer(p.toString(), "")
                }
            },
            jenis      = jenisRaw.map { it.toString() },
            penanganan = penangRaw.map { it.toString() }
        )
    }

    // ── Tiket aktif (FULL — semua kolom) ─────────────────────────────
    suspend fun getActiveTicketsFull(officerEmail: String, officerNama: String): List<Ticket> {
        val emailLower = officerEmail.trim().lowercase()
        val nameLower  = officerNama.trim().lowercase()
        if (emailLower.contains("cg") || nameLower.contains("cg")) return emptyList()

        val data = post(mapOf("action" to "readSheet", "sheetName" to AppConfig.PENGADUAN_SHEET_NAME))
        if (data["ok"] != true && data["status"] != "success")
            throw ServerException(data["message"]?.toString() ?: "Gagal memuat daftar tiket.")

        @Suppress("UNCHECKED_CAST")
        val list = (data["data"] as? List<*>) ?: (data["rows"] as? List<*>) ?: emptyList<Any>()
        val allTickets = list.filterIsInstance<Map<*, *>>().map { raw ->
            @Suppress("UNCHECKED_CAST")
            val r = raw as Map<String, Any?>
            Ticket.fromMap(mapOf(
                "ticket"      to (r["Ticket_RP"] ?: r["Ticket"]),
                "nama"        to (r["Nama_RP"]   ?: r["Nama"]),
                "status"      to (r["STATUS_RP"] ?: r["Status"]),
                "area"        to when {
                    (r["Ticket_RP"] ?: r["Ticket"])?.toString()?.uppercase()?.contains("KD") == true -> "KD"
                    (r["Ticket_RP"] ?: r["Ticket"])?.toString()?.uppercase()?.contains("WR") == true -> "WR"
                    else -> ""
                },
                "idPelanggan" to (r["IDPEL_RP"]     ?: r["ID Pelanggan"]),
                "alamat"      to (r["Alamat_RP"]    ?: r["Alamat"]),
                "telepon"     to (r["TELFON_RP"]    ?: r["Telepon"]),
                "kendala"     to (r["PENGADUAN_RP"] ?: r["Pengaduan"]),
                "lat"         to (r["Latitude_RP"]  ?: r["Latitude"]),
                "lng"         to (r["Longitude_RP"] ?: r["Longitude"])
            ))
        }

        val isWr = emailLower.contains("wr") || nameLower.contains("wr")
        val isKd = emailLower.contains("kd") || nameLower.contains("kd")
        return allTickets.filter { t ->
            if (t.status.trim().lowercase() != "open") return@filter false
            val kl = t.kendala.lowercase()
            if (kl.contains("buka segel") || kl.contains("pasang kembali")) return@filter false
            when {
                isWr -> t.ticket.lowercase().contains("wr")
                isKd -> t.ticket.lowercase().contains("kd")
                else -> true
            }
        }
    }

    // ── Generic row operations ────────────────────────────────────────
    suspend fun findRow(sheetName: String, keyValue: String, keyColumn: String? = null): Map<String, Any?> {
        val body = mutableMapOf<String, Any?>("action" to "find_row", "sheetName" to sheetName, "idpel" to keyValue)
        if (keyColumn != null) body["keyColumn"] = keyColumn
        val data = post(body)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Data tidak ditemukan.")
        @Suppress("UNCHECKED_CAST")
        return (data["row"] as? Map<String, Any?>) ?: emptyMap()
    }

    suspend fun updateRowCells(
        sheetName: String,
        keyValue: String,
        keyColumn: String? = null,
        filterColumn: String? = null,
        filterValue: String? = null,
        updates: Map<String, Any?>
    ) {
        val body = mutableMapOf<String, Any?>(
            "action" to "update_row_cells", "sheetName" to sheetName, "idpel" to keyValue, "updates" to updates
        )
        if (keyColumn   != null) body["keyColumn"]    = keyColumn
        if (filterColumn != null) body["filterColumn"] = filterColumn
        if (filterValue  != null) body["filterValue"]  = filterValue
        val data = HttpClient.postJson(url = AppConfig.REPAIR_BRIDGE_URL, body = body, timeoutSec = AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Gagal menyimpan perubahan.")
    }

    suspend fun uploadPhoto(base64: String, filename: String): String {
        val data = HttpClient.postJson(
            url  = AppConfig.REPAIR_BRIDGE_URL,
            body = mapOf("action" to "upload_photo", "base64" to base64, "filename" to filename, "target" to "perbaikan"),
            timeoutSec = AppConfig.UPLOAD_TIMEOUT_SEC
        )
        if (data["status"] != "success" || data["url"] == null)
            throw ServerException(data["message"]?.toString() ?: "Gagal upload foto ke server.")
        return data["url"].toString()
    }

    suspend fun notifySelesai(payload: Map<String, Any?>) {
        try {
            val body = mutableMapOf<String, Any?>("action" to "notify_selesai")
            body.putAll(payload)
            HttpClient.postJson(url = AppConfig.REPAIR_BRIDGE_URL, body = body)
        } catch (_: Exception) { /* best-effort */ }
    }
}

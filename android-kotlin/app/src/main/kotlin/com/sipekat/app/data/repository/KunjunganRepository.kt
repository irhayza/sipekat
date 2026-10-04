package com.sipekat.app.data.repository

import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.model.VisitCustomer
import com.sipekat.app.data.remote.ApiException
import com.sipekat.app.data.remote.HttpClient
import com.sipekat.app.data.remote.ServerException
import com.sipekat.app.util.CsvParser
import java.net.URLEncoder

data class KunjunganRowResult(
    val row: Map<String, Any?>,
    val rowValues: List<Any?>
) {
    fun valueAt(index: Int): String =
        if (index < 0 || index >= rowValues.size) "" else rowValues[index]?.toString() ?: ""
}

class KunjunganRepository {

    private fun norm(v: String): String = v.trim().lowercase().replace(Regex("\\s+"), " ")

    private suspend fun post(body: Map<String, Any?>, timeoutSec: Long = AppConfig.CONNECTION_TIMEOUT_SEC): Map<String, Any?> =
        HttpClient.postJson(url = AppConfig.VISIT_BRIDGE_URL, body = body, timeoutSec = timeoutSec)

    // ── Daftar kunjungan / pembukaan ──────────────────────────────────
    suspend fun getList(
        nama: String, role: String, menu: String, email: String
    ): List<VisitCustomer> {
        val isAdmin   = email.trim().lowercase().let { it.startsWith("admin") || it.startsWith("super") }
        val sheetName = if (menu == "kunjungan") AppConfig.OCR_SHEET_NAME else AppConfig.PENGADUAN_SHEET_NAME
        val csvUrl    = "${AppConfig.GAS_READ_SHEET_URL}?action=read_sheet&sheet=${URLEncoder.encode(sheetName, "UTF-8")}"
        val csvText   = HttpClient.getAsText(url = csvUrl)
        if (csvText.trim().isEmpty()) return emptyList()

        val allRows = CsvParser.parseCsv(csvText)
        if (allRows.isEmpty()) return emptyList()
        val columns = allRows.first()
        val rows    = allRows.drop(1)

        val col = { cands: List<String> -> CsvParser.findColumnIndex(columns, cands) }
        val idpelIdx   = col(listOf("IDPEL","ID PEL","NO PELANGGAN","IDPEL_RP"))
            ?: throw ApiException("Kolom IDPEL tidak ditemukan di sheet $sheetName")
        val namaIdx    = col(listOf("NAMA","NAMA PELANGGAN","Nama_RP"))
        val alamatIdx  = col(listOf("ALAMAT","ALAMAT PELANGGAN","ALMT","ALM","ALAMAT LOKASI","Alamat_RP"))
        val nomgrtIdx  = col(listOf("NOMETER","NO METER","NOMGRT","NoMGRT_RP"))
        val blnIdx     = col(listOf("BLN","BULAN"))
        val rupiahIdx  = col(listOf("RUPIAH","TAGIHAN"))
        val statusIdx  = col(listOf("STATUS","STATUS_RP"))
        val petugasIdx = col(listOf("PTGS_KJG","PETUGAS","PETUGAS ORDER","Petugas_RP","ptgs"))
        val kondisiIdx = col(listOf("KONDISI","KETERANGAN","PENGADUAN","PENGADUAN_RP"))
        val teleponIdx = col(listOf("TELEPON","TELP","TELFON","NO HP","NOHP","HP","TELFON_RP"))
        val latIdx     = col(listOf("LAT","LATITUDE","Latitude_RP"))
        val lngIdx     = col(listOf("LNG","LON","LONGITUDE","Longitude_RP"))
        val ticketIdx  = col(listOf("TICKET","NO TICKET","TIKET","Ticket_RP"))
        val orderIdx   = col(listOf("ORDER"))

        val normPetugas = norm(nama)
        val emailLower  = email.trim().lowercase()
        val nameLower   = nama.trim().lowercase()
        val isWr  = emailLower.contains("wr") || nameLower.contains("wr")
        val isKd  = emailLower.contains("kd") || nameLower.contains("kd")
        val isCg  = emailLower.contains("cg") || nameLower.contains("cg")

        fun get(cells: MutableList<String>, idx: Int?): String =
            if (idx != null && idx < cells.size) cells[idx].trim() else ""

        val result = mutableListOf<VisitCustomer>()
        for (rawRow in rows) {
            val cells = rawRow.toMutableList()
            if (idpelIdx >= cells.size) continue
            val idpel = cells[idpelIdx].trim()
            if (idpel.isEmpty()) continue
            while (cells.size < columns.size) cells.add("")

            when (menu) {
                "kunjungan" -> {
                    if (orderIdx != null && get(cells, orderIdx).toIntOrNull() != 1) continue
                    if (statusIdx != null && get(cells, statusIdx).lowercase() != "y") continue
                    if (!isAdmin && petugasIdx != null && norm(get(cells, petugasIdx)) != normPetugas) continue
                }
                "pembukaan" -> {
                    if (kondisiIdx == null || statusIdx == null) continue
                    val kondisi = get(cells, kondisiIdx).lowercase()
                    val status  = get(cells, statusIdx).lowercase()
                    if (status != "open") continue
                    if (!kondisi.contains("buka segel") && !kondisi.contains("pasang kembali")) continue
                    val ticketVal = get(cells, ticketIdx).lowercase()
                    when {
                        isWr && !ticketVal.contains("wr") -> continue
                        isKd && !ticketVal.contains("kd") -> continue
                        !isWr && !isKd && !isCg && !isAdmin ->
                            if (petugasIdx != null && norm(get(cells, petugasIdx)) != normPetugas) continue
                    }
                }
                else -> continue
            }

            result.add(VisitCustomer(
                idpel   = idpel,
                nama    = get(cells, namaIdx),
                alamat  = get(cells, alamatIdx),
                nomgrt  = get(cells, nomgrtIdx),
                bln     = get(cells, blnIdx).toIntOrNull() ?: 0,
                rupiah  = get(cells, rupiahIdx).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0,
                status  = get(cells, statusIdx).takeIf { it.isNotEmpty() } ?: "Y",
                telepon = get(cells, teleponIdx),
                kendala = get(cells, kondisiIdx),
                lat     = get(cells, latIdx).toDoubleOrNull(),
                lng     = get(cells, lngIdx).toDoubleOrNull()
            ))
        }
        return result
    }

    // ── Generic row operations ────────────────────────────────────────
    suspend fun findRow(sheetName: String, idpel: String, keyColumn: String? = null): KunjunganRowResult {
        val body = mutableMapOf<String, Any?>("action" to "find_row", "sheetName" to sheetName, "idpel" to idpel)
        if (keyColumn != null) body["keyColumn"] = keyColumn
        val data = post(body)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Data tidak ditemukan.")
        @Suppress("UNCHECKED_CAST")
        return KunjunganRowResult(
            row       = (data["row"] as? Map<String, Any?>) ?: emptyMap(),
            rowValues = (data["rowValues"] as? List<Any?>) ?: emptyList()
        )
    }

    suspend fun updateRowCells(
        sheetName: String, idpel: String, keyColumn: String? = null,
        filterColumn: String? = null, filterValue: String? = null,
        updates: Map<String, Any?>? = null, updatesByIndex: Map<String, Any?>? = null
    ) {
        val body = mutableMapOf<String, Any?>("action" to "update_row_cells", "sheetName" to sheetName, "idpel" to idpel)
        if (keyColumn    != null) body["keyColumn"]     = keyColumn
        if (filterColumn != null) body["filterColumn"]  = filterColumn
        if (filterValue  != null) body["filterValue"]   = filterValue
        if (updates      != null) body["updates"]       = updates
        if (updatesByIndex != null) body["updatesByIndex"] = updatesByIndex
        val data = post(body, AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success") throw ServerException(data["message"]?.toString() ?: "Gagal menyimpan perubahan.")
    }

    suspend fun uploadPhoto(base64: String, filename: String, target: String? = null): String {
        val body = mutableMapOf<String, Any?>("action" to "upload_photo", "base64" to base64, "filename" to filename)
        if (target != null) body["target"] = target
        val data = post(body, AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success" || data["url"] == null)
            throw ServerException(data["message"]?.toString() ?: "Gagal upload foto.")
        return data["url"].toString()
    }

    suspend fun changePassword(user: String, oldPw: String, newPw: String): Map<String, Any?> {
        val data = post(mapOf("action" to "changePassword", "user" to user, "oldPassword" to oldPw, "newPassword" to newPw))
        if (data["status"] != "success") throw ApiException(data["message"]?.toString() ?: "Gagal mengubah password.")
        return data
    }
}

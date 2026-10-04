package com.sipekat.app.data.repository

import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.model.Customer
import com.sipekat.app.data.remote.ApiException
import com.sipekat.app.data.remote.HttpClient
import com.sipekat.app.data.remote.ServerException
import com.sipekat.app.util.CsvParser
import java.net.URLEncoder

class OcrRepository {

    private fun sheetUrl(sheetName: String) =
        "${AppConfig.GAS_READ_SHEET_URL}?action=read_sheet&sheet=${URLEncoder.encode(sheetName, "UTF-8")}"

    private fun norm(v: String) = v.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Pelanggan yang di-assign ke petugas tertentu (untuk modul Pencatatan Meter) */
    suspend fun getAssignedCustomers(petugasNama: String): List<Customer> {
        val csvText = HttpClient.getAsText(url = sheetUrl(AppConfig.OCR_SHEET_NAME))
        if (csvText.trim().isEmpty()) return emptyList()

        val allRows = CsvParser.parseCsv(csvText)
        if (allRows.isEmpty()) return emptyList()
        val columns = allRows.first()
        val rows    = allRows.drop(1)

        val col = { cands: List<String> -> CsvParser.findColumnIndex(columns, cands) }
        val idpelIdx   = col(listOf("IDPEL","NO PELANGGAN","ID PEL")) ?: return emptyList()
        val namaIdx    = col(listOf("NAMA","NAMA PELANGGAN"))
        val alamatIdx  = col(listOf("ALAMAT","ALAMAT PELANGGAN"))
        val nometerIdx = col(listOf("NOMETER","NO METER"))
        val petugasIdx = col(listOf("PERSONIL","ptgs_kjg","ptgs","PETUGAS"))
        val standIdx   = col(listOf("STAND_AWAL","STAND AWAL"))
        val bulanIdx   = col(listOf("BULAN_LALU","BULAN LALU","BULAN"))
        val latIdx     = col(listOf("LAT","LATITUDE"))
        val lngIdx     = col(listOf("LNG","LONGITUDE"))

        val normPetugas = norm(petugasNama)

        val result = mutableListOf<Customer>()
        var rowNum = 2
        for (rawCells in rows) {
            val cells = rawCells.toMutableList()
            while (cells.size < columns.size) cells.add("")
            fun g(idx: Int?): String = if (idx != null && idx < cells.size) cells[idx].trim() else ""

            val idpel   = g(idpelIdx)
            if (idpel.isEmpty()) { rowNum++; continue }
            if (petugasIdx != null && norm(g(petugasIdx)) != normPetugas) { rowNum++; continue }

            result.add(Customer(
                id             = idpel,
                noPelanggan    = idpel,
                noMeter        = g(nometerIdx).takeIf { it.isNotEmpty() },
                nama           = g(namaIdx),
                alamat         = g(alamatIdx).takeIf { it.isNotEmpty() },
                standAwal      = g(standIdx).toIntOrNull() ?: 0,
                bulanLalu      = g(bulanIdx).takeIf { it.isNotEmpty() },
                petugas        = g(petugasIdx).takeIf { it.isNotEmpty() },
                lat            = g(latIdx).toDoubleOrNull(),
                lng            = g(lngIdx).toDoubleOrNull(),
                sheetRowNumber = rowNum
            ))
            rowNum++
        }
        return result
    }

    /** Seluruh pelanggan dapell — untuk auto-fill lintas modul */
    suspend fun getAllDapellCustomers(): List<Customer> {
        val csvText = HttpClient.getAsText(url = sheetUrl(AppConfig.OCR_SHEET_NAME))
        if (csvText.trim().isEmpty()) return emptyList()
        val allRows = CsvParser.parseCsv(csvText)
        if (allRows.isEmpty()) return emptyList()
        val columns = allRows.first()
        val rows    = allRows.drop(1)

        val idpelIdx  = CsvParser.findColumnIndex(columns, listOf("IDPEL","NO PELANGGAN","ID PEL")) ?: return emptyList()
        val namaIdx   = CsvParser.findColumnIndex(columns, listOf("NAMA","NAMA PELANGGAN"))
        val alamatIdx = CsvParser.findColumnIndex(columns, listOf("ALAMAT","ALAMAT PELANGGAN"))

        return rows.mapNotNull { rawCells ->
            val cells = rawCells.toMutableList()
            while (cells.size < columns.size) cells.add("")
            fun g(idx: Int?): String = if (idx != null && idx < cells.size) cells[idx].trim() else ""
            val idpel = g(idpelIdx)
            if (idpel.isEmpty()) null
            else Customer(id = idpel, noPelanggan = idpel, nama = g(namaIdx), alamat = g(alamatIdx).takeIf { it.isNotEmpty() })
        }
    }

    /** POST ke OCR bridge (upload baca meter) */
    suspend fun postToOcr(payload: Map<String, Any?>): Map<String, Any?> =
        HttpClient.postJson(url = AppConfig.OCR_BRIDGE_URL, body = payload, timeoutSec = AppConfig.UPLOAD_TIMEOUT_SEC)
}

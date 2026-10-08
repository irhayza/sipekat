package com.jargas.si_pekat.data

import android.net.Uri
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.CsvParser
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.cell
import com.jargas.si_pekat.core.normalizeComparable
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.PhotoMetadata
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.model.Customer
import com.jargas.si_pekat.model.MeterReading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Port dari ocr_api_service.dart: daftar pelanggan (getAssignedCustomers, getAllDapellCustomers, findCustomerInDapel)
 * dan sinkronisasi bacaan + foto ke sheet OCRDAPEL / Google Drive lewat bridge GAS (syncPendingReading).
 * Jalur backend lokal lama (192.168.1.100) tidak dibawa — tidak terpakai karena OCR_BRIDGE_URL selalu terisi.
 */
object OcrApiService {
    private class SheetTable(val columns: List<String>, val rows: List<List<String>>)

    private fun sheetUrl(sheetName: String) = GasHttp.sheetUrl(sheetName)

    private suspend fun fetchPublicSheet(url: String, label: String): SheetTable {
        val res = GasHttp.getText(url, AppConfig.CONNECTION_TIMEOUT_SEC)
        if (res.code < 200 || res.code >= 300) throw ApiException("Sheet $label tidak bisa dibaca")

        val body = res.body
        if (body.trim().startsWith("{") && body.contains("\"status\":\"error\"")) {
            throw ApiException("Sheet $label tidak ditemukan atau tidak bisa dibaca")
        }
        val lines = CsvParser.splitLines(body)
        if (lines.isEmpty()) throw ApiException("Sheet $label kosong")

        val header = CsvParser.parseRow(lines.first()).map { it.trim() }
        val rows = mutableListOf<List<String>>()
        for (i in 1 until lines.size) {
            if (lines[i].trim().isEmpty()) continue
            rows.add(CsvParser.parseRow(lines[i]))
        }
        return SheetTable(header, rows)
    }

    private class ColumnMap(private val columns: List<String>) {
        private fun find(vararg names: String) = CsvParser.findColumnIndex(columns, names.toList())
        val idpel = find("IDPEL", "ID PEL", "NO PELANGGAN")
        val nama = find("NAMA")
        val alamat = find("ALAMAT")
        val tarif = find("TARIP", "TARIF")
        val meter = find("NOMGRT_LAMA", "NOMETER", "NO METER", "METER")
        val aktif = find("STATUSCEK", "STATUS", "AKTIF")
        val sektor = find("SEKTOR")
        val stLalu = find("RUPIAH", "BLN")
        val link = find("Hasil_OCR", "LINK_OCR", "LINK")
    }

    private fun buildCustomer(cells: List<String>, m: ColumnMap, petugasIdx: Int?, idpel: String, rowNumber: Int): Customer {
        val parsedStand = cells.cell(m.stLalu).toIntOrNull()
        return Customer(
            id = idpel,
            noPelanggan = idpel,
            noMeter = m.meter?.let { cells.cell(it) },
            nama = m.nama?.let { cells.cell(it) } ?: idpel,
            alamat = m.alamat?.let { cells.cell(it) },
            standAwal = parsedStand ?: 0,
            standBulanLalu = parsedStand,
            bulanLalu = "St Lalu",
            tarif = m.tarif?.let { cells.cell(it) },
            aktif = m.aktif?.let { cells.cell(it) },
            sektor = m.sektor?.let { cells.cell(it) },
            petugas = petugasIdx?.let { cells.cell(it) } ?: "",
            linkFoto = m.link?.let { cells.cell(it) },
            sheetRowNumber = rowNumber,
        )
    }

    private fun compareText(left: String, right: String): Int {
        val l = left.toIntOrNull()
        val r = right.toIntOrNull()
        return if (l != null && r != null) l.compareTo(r) else left.compareTo(right)
    }

    /** Pelanggan yang ditugaskan ke [petugasName] (admin melihat semua), belum punya foto OCR, status aktif 'Y'. */
    suspend fun getAssignedCustomers(petugasName: String): List<Customer> {
        val normalizedPetugas = normalizeComparable(petugasName)
        if (normalizedPetugas.isEmpty()) return emptyList()

        val email = AppSessionCache.officerEmail.trim().lowercase()
        val isAdmin = email.startsWith("admin") || email.startsWith("super")

        val table = fetchPublicSheet(sheetUrl(AppConfig.OCR_SHEET_NAME), AppConfig.OCR_SHEET_NAME)
        val m = ColumnMap(table.columns)
        val petugasIdx = CsvParser.findColumnIndex(table.columns, listOf("PERSONIL", "PETUGAS"))
        val idpelIdx = m.idpel ?: throw ApiException("Kolom IDPEL pada OCRDAPEL belum terbaca")

        val deduped = LinkedHashMap<String, Customer>()
        for ((rowIndex, cells) in table.rows.withIndex()) {
            val idpel = cells.cell(idpelIdx)
            if (idpel.isEmpty()) continue

            val aktifVal = if (m.aktif != null) cells.cell(m.aktif) else ""
            if (aktifVal.uppercase() != "Y") continue

            if (!isAdmin) {
                val petugasVal = if (petugasIdx != null) cells.cell(petugasIdx) else ""
                if (normalizeComparable(petugasVal) != normalizedPetugas) continue
            }

            val linkFoto = if (m.link != null) cells.cell(m.link) else ""
            if (linkFoto.isNotEmpty()) continue

            deduped[idpel] = buildCustomer(cells, m, petugasIdx, idpel, rowIndex + 2)
        }

        return deduped.values.sortedWith { l, r ->
            val bySektor = compareText(l.sektorLabel, r.sektorLabel)
            if (bySektor != 0) bySektor else compareText(l.noPelanggan, r.noPelanggan)
        }
    }

    /** Seluruh pelanggan dari sheet dbase (untuk autofill nama & alamat di form Pengaduan). */
    suspend fun getAllDapellCustomers(): List<Customer> {
        val table = fetchPublicSheet(sheetUrl("dbase"), "dbase")
        val m = ColumnMap(table.columns)
        val petugasIdx = CsvParser.findColumnIndex(table.columns, listOf("PERSONIL", "PTGS_KJG", "PETUGAS", "ptgs"))
        val idpelIdx = m.idpel ?: return emptyList()

        val result = ArrayList<Customer>(table.rows.size)
        for ((rowIndex, cells) in table.rows.withIndex()) {
            val idpel = cells.cell(idpelIdx)
            if (idpel.isEmpty()) continue
            result.add(buildCustomer(cells, m, petugasIdx, idpel, rowIndex + 2))
        }
        return result
    }

    suspend fun findCustomerInDapel(targetIdpel: String): Customer? {
        val targetId = targetIdpel.trim().lowercase()
        if (targetId.isEmpty()) return null

        val table = fetchPublicSheet(sheetUrl("dbase"), "dbase")
        val m = ColumnMap(table.columns)
        val petugasIdx = CsvParser.findColumnIndex(table.columns, listOf("PERSONIL", "PTGS_KJG", "PETUGAS", "ptgs"))
        val idpelIdx = m.idpel ?: return null

        for ((rowIndex, cells) in table.rows.withIndex()) {
            val idpel = cells.cell(idpelIdx)
            if (idpel.lowercase() == targetId) return buildCustomer(cells, m, petugasIdx, idpel, rowIndex + 2)
        }
        return null
    }

    // ── Sinkronisasi bacaan meter ───────────────────────────────────────────

    /** Kirim bacaan (+ foto bila ada) ke bridge GAS. Mengembalikan (foto_url, foto_gdrive_id). */
    private suspend fun syncOcrDapelReading(reading: MeterReading, petugasName: String): Pair<String, String> {
        var imageBase64: String? = null
        var fileName: String? = null
        val photoPath = reading.fotoPath?.trim()
        if (!photoPath.isNullOrEmpty()) {
            val file = File(photoPath)
            if (withContext(Dispatchers.IO) { file.exists() }) {
                imageBase64 = withContext(Dispatchers.IO) { android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP) }
                fileName = file.name.ifEmpty { "meter-${reading.pelangganId}.jpg" }
            }
        }

        val body = buildMap<String, Any?> {
            put("action", "sync_ocr_dapel")
            put("spreadsheet_id", AppConfig.LOGIN_SHEET_ID)
            put("sheet_name", AppConfig.OCR_SHEET_NAME)
            put("drive_folder_id", AppConfig.OCR_DRIVE_FOLDER_ID)
            put("idpel", reading.pelangganId)
            put("st_lalu", reading.standAngka.toString())
            put("hasil_ocr", reading.standAngka.toString())
            put("stand_column", "RUPIAH")
            put("hasil_ocr_column", "Hasil_OCR")
            put("link_column", "Link_OCR")
            put("koordinat_column", "LATITUDE_OCR")
            put("koordinat_lng_column", "LONGITUDE_OCR")
            put("keterangan_column", "CATATAN")
            put("exif_column", "EXIF_OCR")
            put("keterangan", reading.catatan ?: "")
            put("timestamp", MeterReading.formatIso(reading.createdAt))
            put("exif", PhotoMetadata.formatSheet(reading.createdAt))
            // Admin tidak boleh menimpa kolom PERSONIL (penugasan) milik petugas lain.
            val email = AppSessionCache.officerEmail.trim().lowercase()
            val isAdmin = email.startsWith("admin") || email.startsWith("super")
            if (petugasName.isNotBlank() && !isAdmin) put("petugas", petugasName)
            if (reading.lat != null) put("latitude", reading.lat)
            if (reading.lng != null) put("longitude", reading.lng)
            // `koordinat` sengaja TIDAK dikirim: bila ada, server menulis "lat, lng" ke kolom LATITUDE_OCR saja.
            if (!reading.catatan.isNullOrBlank()) put("catatan", reading.catatan.trim())
            if (imageBase64 != null) put("image_base64", imageBase64)
            if (fileName != null) put("file_name", fileName)
            if (imageBase64 != null) put("content_type", "image/jpeg")
        }

        val res = try {
            GasHttp.postJson(AppConfig.OCR_BRIDGE_URL, body, AppConfig.UPLOAD_TIMEOUT_SEC)
        } catch (e: InterruptedIOException) {
            throw ApiException("Server tidak merespon. Periksa koneksi internet Anda lalu coba lagi.")
        } catch (e: IOException) {
            throw ApiException("Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.")
        }
        if (res.code < 200 || res.code >= 300) throw ApiException("Sinkronisasi OCRDAPEL gagal (Status ${res.code}).")

        val trimmed = res.body.trim()
        if (trimmed.startsWith("<", ignoreCase = true)) {
            throw ApiException(
                "Sinkronisasi OCRDAPEL gagal: Server mengembalikan halaman HTML (Status ${res.code}). " +
                    "Pastikan URL Apps Script sudah benar, di-deploy sebagai Web App, dan diatur aksesnya untuk \"Anyone\" (siapa saja).",
            )
        }
        val payload: Map<String, Any?> = if (trimmed.isEmpty()) emptyMap<String, Any?>() else (try { Json.decode(trimmed).asJsonMap() } catch (e: Exception) { null }
            ?: throw ApiException("Server mengalami gangguan saat memproses permintaan. Coba lagi."))
        val data = payload["data"].asJsonMap() ?: payload
        val status = payload["status"]?.toString()?.lowercase()
        if (!status.isNullOrEmpty() && status != "ok" && status != "success") {
            throw ServerException(payload["error"]?.toString() ?: "Sinkronisasi OCRDAPEL gagal")
        }
        val url = data["foto_url"]?.toString() ?: data["url"]?.toString() ?: data["link"]?.toString() ?: ""
        val driveId = data["foto_gdrive_id"]?.toString() ?: data["file_id"]?.toString() ?: data["drive_id"]?.toString() ?: ""
        return url to driveId
    }

    /** Sinkronkan satu bacaan; hasilnya salinan bacaan bertanda terkirim (fotoPath dikosongkan). */
    suspend fun syncPendingReading(reading: MeterReading, petugasName: String? = null): MeterReading {
        val (url, driveId) = syncOcrDapelReading(reading, petugasName ?: AppSessionCache.currentOfficerName())
        return MeterReading(
            id = reading.id,
            pelangganId = reading.pelangganId,
            petugasId = reading.petugasId,
            standAngka = reading.standAngka,
            fotoPath = null,
            fotoUrl = url.ifEmpty { reading.fotoUrl },
            fotoDriveId = driveId.ifEmpty { reading.fotoDriveId },
            lat = reading.lat,
            lng = reading.lng,
            catatan = reading.catatan,
            bulan = reading.bulan,
            createdAt = reading.createdAt,
            isSynced = true,
            minus = reading.minus,
        )
    }
}

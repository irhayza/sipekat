package com.jargas.si_pekat.data

import android.net.Uri
import com.google.gson.JsonParseException
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.CsvParser
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList
import com.jargas.si_pekat.core.cell
import com.jargas.si_pekat.core.normalizeComparable
import com.jargas.si_pekat.model.VisitCustomer
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Hasil [KunjunganApiService.findRow]: `row` (Map menurut nama header, untuk sheet yang headernya pasti,
 * mis. "Pengaduan") dan `rowValues` (posisi kolom asli 0-based, untuk sheet yang headernya tidak pasti,
 * mis. "CABUT").
 */
class KunjunganRowResult(val row: JsonMap, val rowValues: List<Any?>) {
    fun valueAt(index: Int): String = if (index < 0 || index >= rowValues.size) "" else rowValues[index]?.toString() ?: ""
}

/**
 * Port dari kunjungan_api_service.dart. Data mock (_mockGetList, aktif hanya bila URL masih placeholder)
 * sengaja tidak dibawa karena sudah tidak terpakai.
 */
object KunjunganApiService {
    private suspend fun post(payload: Map<String, Any?>, timeoutSec: Int): JsonMap {
        val res = try {
            GasHttp.postJson(AppConfig.VISIT_BRIDGE_URL, payload, timeoutSec)
        } catch (e: InterruptedIOException) {
            throw ApiException(
                "Koneksi ke server timeout ($timeoutSec detik). Sinyal internet lambat atau server Google Apps Script " +
                    "sedang lambat merespons (cold start). Coba lagi.",
            )
        } catch (e: IOException) {
            throw ApiException("Tidak ada koneksi internet atau server tidak dapat dijangkau: ${e.message}")
        }
        if (res.code != 200) {
            throw ApiException("Server merespons dengan status ${res.code}. Periksa kembali deployment Google Apps Script Anda.")
        }
        val body = res.body.trim()
        if (body.startsWith("<")) {
            throw ApiException(
                "Server mengembalikan halaman HTML, bukan JSON. Kemungkinan besar deployment Web App Google Apps Script " +
                    "belum diset akses ke \"Anyone\", atau URL di AppConfig.kt sudah kedaluwarsa. " +
                    "Cek menu Deploy > Manage deployments di Apps Script.",
            )
        }
        val decoded = try {
            Json.decode(body)
        } catch (e: JsonParseException) {
            throw ApiException("Gagal membaca respons server (format JSON tidak valid).")
        }
        return decoded.asJsonMap() ?: throw ApiException("Format respons server tidak sesuai yang diharapkan.")
    }

    suspend fun getList(nama: String, @Suppress("UNUSED_PARAMETER") role: String, menu: String): List<VisitCustomer> {
        val email = AppSessionCache.officerEmail.trim().lowercase()
        val isAdmin = email.startsWith("admin") || email.startsWith("super")

        val sheetName = if (menu == "kunjungan") AppConfig.OCR_SHEET_NAME else AppConfig.PENGADUAN_SHEET_NAME
        // Endpoint GAS filter-safe: getValues() membaca semua baris termasuk yang difilter/disembunyikan.
        val csvUrl = GasHttp.sheetUrl(sheetName)

        val csvText = try {
            val res = GasHttp.getText(csvUrl, AppConfig.CONNECTION_TIMEOUT_SEC)
            if (res.code != 200) throw ApiException("Server merespon dengan status ${res.code}")
            res.body
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Gagal memuat data pelanggan ($sheetName): ${e.message ?: e}")
        }

        if (csvText.trim().isEmpty()) return emptyList()

        val allRows = CsvParser.splitLines(csvText)
            .filter { it.trim().isNotEmpty() }
            .map { CsvParser.parseRow(it, handleEscapedQuotes = false) }
        if (allRows.isEmpty()) return emptyList()

        val columns = allRows.first()
        val rows = allRows.drop(1)
        fun col(vararg names: String) = CsvParser.findColumnIndex(columns, names.toList())

        val idpelIndex = col("IDPEL", "ID PEL", "NO PELANGGAN", "IDPEL_RP")
        val namaIndex = col("NAMA", "NAMA PELANGGAN", "Nama_RP")
        val alamatIndex = col(
            "ALAMAT", "ALAMAT PELANGGAN", "ALAMATPELANGGAN", "ALMT", "ALM", "ALAMAT LOKASI", "ALAMAT PASANG",
            "ALAMAT_PELANGGAN", "LOKASI", "ADDRESS", "ALAMAT LENGKAP", "JALAN", "Alamat_RP",
        )
        val nomgrtIndex = col("NOMETER", "NO METER", "NOMGRT", "NOMGRT_LAMA", "NoMGRT_RP")
        val blnIndex = col("BLN", "BULAN")
        val rupiahIndex = col("RUPIAH", "TAGIHAN")
        val statusIndex = col("STATUS", "STATUS_RP")
        val petugasIndex = col("PTGS_KJG", "PETUGAS_KJG", "PETUGAS", "PETUGAS ORDER", "NAMA PETUGAS", "PERSONIL", "Petugas_RP", "ptgs")
        val kondisiIndex = col("KONDISI", "KETERANGAN", "PENGADUAN", "PENGADUAN_RP", "KONDISI_PR")
        val teleponIndex = col("TELEPON", "TELP", "TELFON", "NO HP", "NOHP", "HP", "WHATSAPP", "WA", "TELFON_RP")
        val latIndex = col("LAT", "LATITUDE", "Latitude_RP")
        val lngIndex = col("LNG", "LON", "LONGITUDE", "Longitude_RP")
        val ticketIndex = col("TICKET", "NO TICKET", "TIKET", "NO TIKET", "Ticket_RP")
        val orderIndex = col("ORDER")

        if (idpelIndex == null) throw ApiException("Kolom IDPEL pada sheet $sheetName tidak ditemukan")

        val normalizedPetugas = normalizeComparable(nama)
        val result = mutableListOf<VisitCustomer>()

        val emailLower = AppSessionCache.officerEmail.trim().lowercase()
        val nameLower = nama.trim().lowercase()
        val isWr = emailLower.contains("wr") || nameLower.contains("wr")
        val isKd = emailLower.contains("kd") || nameLower.contains("kd")
        val isCg = emailLower.contains("cg") || nameLower.contains("cg")

        // Pemetaan IDPEL → petugas dari sheet dbase (khusus akun CG di menu pembukaan).
        val cgCabutMapping = mutableMapOf<String, String>()
        if (menu == "pembukaan" && !isAdmin && isCg) {
            val dbaseUrl = GasHttp.sheetUrl(AppConfig.OCR_SHEET_NAME)
            try {
                val dbaseRes = GasHttp.getText(dbaseUrl, AppConfig.CONNECTION_TIMEOUT_SEC)
                if (dbaseRes.code == 200) {
                    val dbLines = CsvParser.splitLines(dbaseRes.body)
                    if (dbLines.isNotEmpty()) {
                        val dbCols = CsvParser.parseRow(dbLines.first(), handleEscapedQuotes = false)
                        val idxIdpel = CsvParser.findColumnIndex(dbCols, listOf("IDPEL", "ID PEL", "NO PELANGGAN"))
                        val idxPtgs = CsvParser.findColumnIndex(
                            dbCols, listOf("PERSONIL", "ptgs_kjg", "ptgs", "PETUGAS", "NAMA PETUGAS", "PETUGAS ORDER"),
                        )
                        if (idxIdpel != null && idxPtgs != null) {
                            for (i in 1 until dbLines.size) {
                                if (dbLines[i].trim().isEmpty()) continue
                                val row = CsvParser.parseRow(dbLines[i], handleEscapedQuotes = false)
                                if (idxIdpel < row.size && idxPtgs < row.size) {
                                    val id = row[idxIdpel].trim().lowercase()
                                    if (id.isNotEmpty()) cgCabutMapping[id] = normalizeComparable(row[idxPtgs])
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Diabaikan: tanpa pemetaan, akun CG tidak melihat tiket apa pun (filter ketat di bawah).
            }
        }

        for (cells in rows) {
            if (idpelIndex >= cells.size) continue
            val idpel = cells[idpelIndex].trim()
            if (idpel.isEmpty()) continue
            while (cells.size < columns.size) cells.add("")

            fun build(kendala: String, alamat: String) = VisitCustomer(
                idpel = idpel,
                nama = cells.cell(namaIndex),
                alamat = alamat,
                nomgrt = cells.cell(nomgrtIndex),
                bln = cells.cell(blnIndex).toIntOrNull() ?: 0,
                rupiah = cells.cell(rupiahIndex).replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0,
                status = cells.cell(statusIndex).ifEmpty { "Y" },
                telepon = cells.cell(teleponIndex),
                kendala = kendala,
                lat = cells.cell(latIndex).toDoubleOrNull(),
                lng = cells.cell(lngIndex).toDoubleOrNull(),
                ticket = cells.cell(ticketIndex),
            )

            if (menu == "kunjungan") {
                // Kolom ORDER harus 1
                if (orderIndex != null) {
                    val orderVal = cells[orderIndex].trim().toIntOrNull() ?: 0
                    if (orderVal != 1) continue
                }
                // Kolom STATUS harus 'y'
                if (statusIndex != null && cells[statusIndex].trim().lowercase() != "y") continue
                if (!isAdmin) {
                    if (petugasIndex == null || normalizeComparable(cells[petugasIndex]) != normalizedPetugas) continue
                }

                val candidate = build(kendala = cells.cell(kondisiIndex), alamat = cells.cell(alamatIndex))
                // Tunggakan 0 dan 1 bulan tidak dikunjungi (BLN kosong dianggap 0).
                if (!candidate.eligibleForVisit) continue
                result.add(candidate)
            } else if (menu == "pembukaan") {
                // PENGADUAN_RP memuat 'buka segel'/'pasang kembali' dan STATUS_RP = open
                if (kondisiIndex == null || statusIndex == null) continue
                val kendalaLower = cells[kondisiIndex].trim().lowercase()
                val statusLower = cells[statusIndex].trim().lowercase()
                if (statusLower != "open") continue
                if (!kendalaLower.contains("buka segel") && !kendalaLower.contains("pasang kembali")) continue

                val ticketVal = cells.cell(ticketIndex).lowercase()
                if (isWr) {
                    if (!ticketVal.contains("wr")) continue
                } else if (isKd) {
                    if (!ticketVal.contains("kd")) continue
                } else if (isCg) {
                    if (!isAdmin) {
                        // Filter ketat: PERSONIL di dbase harus cocok dengan petugas yang login.
                        val assigned = cgCabutMapping[idpel.lowercase()]
                        if (assigned == null || assigned != normalizedPetugas) continue
                    }
                } else {
                    if (!isAdmin) {
                        if (petugasIndex == null || petugasIndex >= cells.size || normalizeComparable(cells[petugasIndex]) != normalizedPetugas) continue
                    }
                }

                val alamat = if (alamatIndex != null && alamatIndex < cells.size) cells[alamatIndex].trim()
                else if (cells.size > 2) cells[2].trim() else ""
                result.add(build(kendala = cells[kondisiIndex].trim(), alamat = alamat))
            }
        }
        return result
    }

    // ═══ Endpoint generik — GAS hanya mencari baris & menulis sel; aturan bisnis ada di aplikasi ═══

    /** Cari satu baris di [sheetName] berdasarkan [idpel] (atau kolom lain lewat [keyColumn], mis. "Ticket"). */
    suspend fun findRow(
        sheetName: String,
        idpel: String,
        keyColumn: String? = null,
        filterColumn: String? = null,
        filterValue: String? = null,
    ): KunjunganRowResult {
        val payload = buildMap<String, Any?> {
            put("action", "find_row")
            put("sheetName", sheetName)
            put("idpel", idpel)
            if (keyColumn != null) put("keyColumn", keyColumn)
            if (filterColumn != null) put("filterColumn", filterColumn)
            if (filterValue != null) put("filterValue", filterValue)
        }
        val data = post(payload, AppConfig.CONNECTION_TIMEOUT_SEC)
        if (data["status"] != "success") {
            throw ServerException(data["message"]?.toString() ?: "Data tidak ditemukan.")
        }
        return KunjunganRowResult(
            row = data["row"].asJsonMap() ?: emptyMap(),
            rowValues = data["rowValues"].asList() ?: emptyList<Any?>(),
        )
    }

    /**
     * Tulis [updates] (kunci = nama kolom persis seperti header) dan/atau [updatesByIndex] (kunci = nomor kolom
     * 1-based sebagai String) ke baris yang cocok. GAS mencari ulang barisnya di server (aman dari race condition).
     */
    suspend fun updateRowCells(
        sheetName: String,
        idpel: String,
        keyColumn: String? = null,
        filterColumn: String? = null,
        filterValue: String? = null,
        updates: Map<String, Any?>? = null,
        updatesByIndex: Map<String, Any?>? = null,
        required: Collection<String> = emptyList(),
    ): List<String> {
        val payload = buildMap<String, Any?> {
            put("action", "update_row_cells")
            put("sheetName", sheetName)
            put("idpel", idpel)
            if (keyColumn != null) put("keyColumn", keyColumn)
            if (filterColumn != null) put("filterColumn", filterColumn)
            if (filterValue != null) put("filterValue", filterValue)
            if (updates != null) put("updates", updates)
            if (updatesByIndex != null) put("updatesByIndex", updatesByIndex)
        }
        val data = post(payload, AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success") {
            throw ServerException(data["message"]?.toString() ?: "Gagal menyimpan perubahan ke server.")
        }
        val skipped = (data["skipped"].asList() ?: emptyList<Any?>()).map { it.toString() }
        return SheetColumns.missing(skipped, required)
    }

    /** Upload satu foto base64 ke Drive dan kembalikan URL-nya (dipisah dari tulis sheet agar kegagalannya jelas). */
    suspend fun uploadPhoto(base64: String, filename: String, target: String? = null): String {
        val payload = buildMap<String, Any?> {
            put("action", "upload_photo")
            put("base64", base64)
            put("filename", filename)
            if (target != null) put("target", target)
        }
        val data = post(payload, AppConfig.UPLOAD_TIMEOUT_SEC)
        if (data["status"] != "success" || data["url"] == null) {
            throw ServerException(data["message"]?.toString() ?: "Gagal upload foto ke server.")
        }
        return data["url"].toString()
    }
}

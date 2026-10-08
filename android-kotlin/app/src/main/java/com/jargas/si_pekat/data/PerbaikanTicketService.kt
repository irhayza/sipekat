package com.jargas.si_pekat.data

import android.net.Uri
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.CsvParser
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.model.Ticket
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Daftar tiket aktif dari sheet Pengaduan lewat endpoint GAS filter-safe (CSV).
 * Port dari perbaikan_ticket_service.dart — termasuk perbaikan format koordinat yang rusak
 * (mis. "-7123456" → -7.123456 karena sheet berlokal Indonesia memakai koma/tanpa titik).
 */
object PerbaikanTicketService {
    suspend fun getActiveTickets(): List<Ticket> {
        val emailLower = AppSessionCache.officerEmail.trim().lowercase()
        val nameLower = AppSessionCache.officerNama.trim().lowercase()
        if (emailLower.contains("cg") || nameLower.contains("cg")) return emptyList()

        val url = GasHttp.sheetUrl(AppConfig.PENGADUAN_SHEET_NAME)
        val res = try {
            GasHttp.getText(url, AppConfig.CONNECTION_TIMEOUT_SEC)
        } catch (e: InterruptedIOException) {
            throw ApiException("Server tidak merespon. Periksa koneksi internet Anda.")
        } catch (e: IOException) {
            throw ApiException("Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.")
        }
        if (res.code != 200) throw ApiException("Server merespon status ${res.code}.")
        if (res.body.trim().isEmpty()) return emptyList()

        // Mode lama: setiap tanda kutip hanya menukar status kutip (perilaku parser Flutter di file ini).
        val allRows = CsvParser.splitLines(res.body).filter { it.trim().isNotEmpty() }
            .map { CsvParser.parseRow(it, handleEscapedQuotes = false) }
        if (allRows.isEmpty()) return emptyList()

        val cols = allRows.first()
        val rows = allRows.drop(1)
        val colIndex = HashMap<String, Int>()
        cols.forEachIndexed { i, c ->
            val label = c.trim().lowercase()
            if (label.isNotEmpty()) colIndex[label] = i
        }

        fun pickStr(row: List<String>, keys: List<String>): String {
            for (key in keys) {
                val idx = colIndex[key.lowercase()] ?: continue
                if (idx >= row.size) continue
                val v = row[idx].trim()
                if (v.isNotEmpty() && v != "null") return v
            }
            return ""
        }

        fun pickDouble(row: List<String>, keys: List<String>, isLat: Boolean): Double? {
            for (key in keys) {
                val idx = colIndex[key.lowercase()] ?: continue
                if (idx >= row.size) continue
                val raw = row[idx].trim()
                if (raw.isEmpty() || raw == "null") continue

                val cleaned = raw.replace(Regex("\\s+"), "").replace(",", ".")
                val isNegative = cleaned.startsWith("-")
                val digits = cleaned.replace(Regex("[^0-9]"), "")
                if (digits.isEmpty()) continue

                if (isLat) {
                    // Latitude Jawa Timur selalu -7.xxxxx
                    if (digits.startsWith("7")) {
                        "-7.${digits.substring(1)}".toDoubleOrNull()?.let { return it }
                    } else {
                        "${if (isNegative) "-" else ""}${digits.substring(0, 1)}.${digits.substring(1)}".toDoubleOrNull()?.let { return it }
                    }
                } else {
                    // Longitude Jawa Timur sekitar 112.xxxxx
                    if (digits.startsWith("112")) {
                        "112.${digits.substring(3)}".toDoubleOrNull()?.let { return it }
                    } else if (digits.startsWith("11") && digits.length >= 3) {
                        "${digits.substring(0, 3)}.${digits.substring(3)}".toDoubleOrNull()?.let { return it }
                    } else if (digits.length >= 3) {
                        "${digits.substring(0, 3)}.${digits.substring(3)}".toDoubleOrNull()?.let { return it }
                    } else {
                        cleaned.toDoubleOrNull()?.let { return it }
                    }
                }
            }
            return null
        }

        fun normalizePhone(raw: String): String {
            if (raw.isEmpty()) return raw
            val digitsOnly = raw.replace(Regex("[^0-9]"), "")
            if (digitsOnly == raw && digitsOnly.isNotEmpty() && digitsOnly.startsWith("8") && digitsOnly.length >= 9) return "0$digitsOnly"
            return raw
        }

        val isWr = emailLower.contains("wr") || nameLower.contains("wr")
        val isKd = emailLower.contains("kd") || nameLower.contains("kd")
        val tickets = ArrayList<Ticket>()

        for (row in rows) {
            if (row.isEmpty()) continue
            val statusVal = pickStr(row, listOf("status_rp", "status"))
            val pengaduanVal = pickStr(row, listOf("pengaduan_rp", "pengaduan", "kendala")).lowercase()

            if (statusVal.lowercase() != "open") continue
            if (pengaduanVal.contains("buka segel") || pengaduanVal.contains("pasang kembali")) continue

            val ticketNo = pickStr(row, listOf("ticket_rp", "ticket", "no tiket", "tiket", "no. tiket", "no_tiket", "nomor tiket", "kode tiket"))
            if (ticketNo.isEmpty()) continue
            val lower = ticketNo.lowercase()
            if (isWr && !lower.contains("wr")) continue
            if (isKd && !lower.contains("kd")) continue

            tickets.add(
                Ticket(
                    ticket = ticketNo,
                    nama = pickStr(row, listOf("nama_rp", "nama", "nama pelanggan", "name")),
                    status = if (statusVal.isEmpty()) AppConfig.STATUS_OPEN else statusVal.uppercase(),
                    area = pickStr(row, listOf("area", "wilayah", "zona", "rayon")),
                    idPelanggan = pickStr(row, listOf("idpel_rp", "idpel", "id pelanggan", "id_pelanggan", "no pelanggan", "id pel")),
                    alamat = pickStr(row, listOf("alamat_rp", "alamat", "address")),
                    telepon = normalizePhone(
                        pickStr(row, listOf("telfon_rp", "telepon", "no telepon", "telp", "no telp", "hp", "no hp", "handphone", "wa", "whatsapp", "no wa", "phone", "mobile", "telfon", "nohp")),
                    ),
                    kendala = pickStr(row, listOf("pengaduan_rp", "kendala", "keluhan", "pengaduan", "masalah", "deskripsi", "keterangan", "jenis pengaduan")),
                    lat = pickDouble(row, listOf("latitude_rp", "lat", "latitude", "lintang"), isLat = true),
                    lng = pickDouble(row, listOf("longitude_rp", "lng", "lon", "longitude", "bujur"), isLat = false),
                ),
            )
        }
        return tickets
    }
}

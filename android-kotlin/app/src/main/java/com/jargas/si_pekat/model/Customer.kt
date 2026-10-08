package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap

/** Data pelanggan (modul Pencatatan Meter & autofill lintas modul). Port dari models/ocr/customer.dart. */
data class Customer(
    val id: String,
    val noPelanggan: String,
    val noMeter: String? = null,
    val nama: String,
    val alamat: String? = null,
    val kelurahan: String? = null,
    val kecamatan: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val standAwal: Int = 0,
    val standBulanLalu: Int? = null,
    val bulanLalu: String? = null,
    val tarif: String? = null,
    val aktif: String? = null,
    val sektor: String? = null,
    val petugas: String? = null,
    val linkFoto: String? = null,
    val sheetRowNumber: Int? = null,
) {
    val labelBulanLalu: String
        get() {
            val value = bulanLalu ?: return "Belum ada data"
            val parts = value.split("-")
            if (parts.size < 2) return value
            val idx = parts[1].toIntOrNull() ?: 0
            if (idx < 1 || idx > 12) return value
            return "${BULAN[idx]} ${parts[0]}"
        }

    val previousStandLabel: String
        get() {
            val value = bulanLalu?.trim()
            if (value.isNullOrEmpty()) return "Stand Sebelumnya"
            if (Regex("^\\d{4}-\\d{2}$").matches(value)) return "Stand $labelBulanLalu"
            return value
        }

    val sektorLabel: String
        get() {
            val value = sektor?.trim()
            return if (value.isNullOrEmpty()) "Tanpa sektor" else value
        }

    fun toMap(): JsonMap = mapOf(
        "id" to id,
        "no_pelanggan" to noPelanggan,
        "no_meter" to noMeter,
        "nama" to nama,
        "alamat" to alamat,
        "kelurahan" to kelurahan,
        "kecamatan" to kecamatan,
        "lat" to lat,
        "lng" to lng,
        "stand_awal" to standAwal,
        "stand_bulan_lalu" to standBulanLalu,
        "bulan_lalu" to bulanLalu,
        "tarif" to tarif,
        "aktif" to aktif,
        "sektor" to sektor,
        "petugas" to petugas,
        "link" to linkFoto,
        "sheet_row_number" to sheetRowNumber,
    )

    companion object {
        private val BULAN = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")

        fun fromMap(j: JsonMap): Customer = Customer(
            id = j["id"]?.toString() ?: "",
            noPelanggan = j["no_pelanggan"]?.toString() ?: j["noPelanggan"]?.toString() ?: "",
            noMeter = j["no_meter"]?.toString() ?: j["noMeter"]?.toString(),
            nama = j["nama"]?.toString() ?: "",
            alamat = j["alamat"]?.toString(),
            kelurahan = j["kelurahan"]?.toString(),
            kecamatan = j["kecamatan"]?.toString(),
            lat = j["lat"]?.toString()?.toDoubleOrNull(),
            lng = j["lng"]?.toString()?.toDoubleOrNull(),
            standAwal = (j["stand_awal"]?.toString() ?: j["standAwal"]?.toString() ?: "0").toIntOrNull() ?: 0,
            standBulanLalu = if (j["stand_bulan_lalu"] != null) j["stand_bulan_lalu"].toString().toIntOrNull()
            else j["standBulanLalu"]?.toString()?.toIntOrNull(),
            bulanLalu = j["bulan_lalu"]?.toString() ?: j["bulanLalu"]?.toString(),
            tarif = j["tarip"]?.toString() ?: j["tarif"]?.toString(),
            aktif = j["aktif"]?.toString(),
            sektor = j["sektor"]?.toString() ?: j["Sektor"]?.toString(),
            petugas = j["petugas"]?.toString() ?: j["Petugas"]?.toString(),
            linkFoto = j["link"]?.toString() ?: j["Link"]?.toString() ?: j["linkFoto"]?.toString(),
            sheetRowNumber = if (j["sheet_row_number"] != null) j["sheet_row_number"].toString().toIntOrNull()
            else j["sheetRowNumber"]?.toString()?.toIntOrNull(),
        )
    }
}

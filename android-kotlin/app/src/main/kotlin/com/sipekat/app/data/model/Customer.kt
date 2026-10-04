package com.sipekat.app.data.model

data class Customer(
    val id: String           = "",
    val noPelanggan: String  = "",
    val noMeter: String?     = null,
    val nama: String         = "",
    val alamat: String?      = null,
    val kelurahan: String?   = null,
    val kecamatan: String?   = null,
    val lat: Double?         = null,
    val lng: Double?         = null,
    val standAwal: Int       = 0,
    val standBulanLalu: Int? = null,
    val bulanLalu: String?   = null,
    val tarif: String?       = null,
    val aktif: String?       = null,
    val sektor: String?      = null,
    val petugas: String?     = null,
    val linkFoto: String?    = null,
    val sheetRowNumber: Int? = null
) {
    val labelBulanLalu: String get() {
        val bl = bulanLalu ?: return "Belum ada data"
        val parts = bl.split("-")
        if (parts.size < 2) return bl
        val months = listOf("","Jan","Feb","Mar","Apr","Mei","Jun","Jul","Ags","Sep","Okt","Nov","Des")
        val idx = parts[1].toIntOrNull() ?: 0
        return if (idx in 1..12) "${months[idx]} ${parts[0]}" else bl
    }

    val previousStandLabel: String get() {
        val v = bulanLalu?.trim()
        if (v.isNullOrEmpty()) return "Stand Sebelumnya"
        return if (Regex("""^\d{4}-\d{2}$""").matches(v)) "Stand $labelBulanLalu" else v
    }

    val sektorLabel: String get() =
        sektor?.trim()?.takeIf { it.isNotEmpty() } ?: "Tanpa sektor"

    companion object {
        fun fromMap(j: Map<String, Any?>): Customer = Customer(
            id             = j["id"]?.toString() ?: "",
            noPelanggan    = (j["no_pelanggan"] ?: j["noPelanggan"])?.toString() ?: "",
            noMeter        = (j["no_meter"] ?: j["noMeter"])?.toString(),
            nama           = j["nama"]?.toString() ?: "",
            alamat         = j["alamat"]?.toString(),
            kelurahan      = j["kelurahan"]?.toString(),
            kecamatan      = j["kecamatan"]?.toString(),
            lat            = j["lat"]?.toString()?.toDoubleOrNull(),
            lng            = j["lng"]?.toString()?.toDoubleOrNull(),
            standAwal      = (j["stand_awal"] ?: j["standAwal"])?.toString()?.toIntOrNull() ?: 0,
            standBulanLalu = (j["stand_bulan_lalu"] ?: j["standBulanLalu"])?.toString()?.toIntOrNull(),
            bulanLalu      = (j["bulan_lalu"] ?: j["bulanLalu"])?.toString(),
            tarif          = (j["tarip"] ?: j["tarif"])?.toString(),
            aktif          = j["aktif"]?.toString(),
            sektor         = (j["sektor"] ?: j["Sektor"])?.toString(),
            petugas        = (j["petugas"] ?: j["Petugas"])?.toString(),
            linkFoto       = (j["link"] ?: j["Link"] ?: j["linkFoto"])?.toString(),
            sheetRowNumber = (j["sheet_row_number"] ?: j["sheetRowNumber"])?.toString()?.toIntOrNull()
        )
    }
}

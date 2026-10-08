package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap

/** Pelanggan pada daftar Kunjungan/Pembukaan. Port dari VisitCustomer di kunjungan_api_service.dart. */
data class VisitCustomer(
    val idpel: String,
    val nama: String,
    val alamat: String,
    val nomgrt: String,
    val bln: Int,
    val rupiah: Double,
    val status: String,
    /** Nomor telepon/WA (kolom TELEPON/TELP/HP pada sheet bila ada). */
    val telepon: String = "",
    /** Ringkasan kendala/kondisi di lapangan (kolom KONDISI/KETERANGAN). */
    val kendala: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    /** Nomor tiket (kolom Ticket_RP) bila ada — dipakai notifikasi. */
    val ticket: String = "",
) {
    fun toMap(): JsonMap = buildMap {
        put("IDPEL", idpel)
        put("NAMA", nama)
        put("ALAMAT", alamat)
        put("NOMGRT", nomgrt)
        put("BLN", bln)
        put("RUPIAH", rupiah)
        put("STATUS", status)
        put("TELEPON", telepon)
        put("KONDISI", kendala)
        put("TICKET", ticket)
        if (lat != null) put("LAT", lat)
        if (lng != null) put("LNG", lng)
    }

    companion object {
        fun fromMap(json: JsonMap): VisitCustomer = VisitCustomer(
            idpel = json["IDPEL"]?.toString() ?: "",
            nama = json["NAMA"]?.toString() ?: "",
            alamat = json["ALAMAT"]?.toString() ?: "",
            nomgrt = json["NOMGRT"]?.toString() ?: "",
            bln = (json["BLN"]?.toString() ?: "0").toIntOrNull() ?: 0,
            rupiah = (json["RUPIAH"]?.toString() ?: "0").toDoubleOrNull() ?: 0.0,
            status = json["STATUS"]?.toString() ?: "Y",
            telepon = json["TELEPON"]?.toString() ?: "",
            kendala = json["KONDISI"]?.toString() ?: "",
            lat = json["LAT"]?.toString()?.toDoubleOrNull(),
            lng = json["LNG"]?.toString()?.toDoubleOrNull(),
            ticket = json["TICKET"]?.toString() ?: "",
        )
    }
}

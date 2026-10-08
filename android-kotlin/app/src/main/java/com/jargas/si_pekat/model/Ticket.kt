package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap

/**
 * Tiket laporan perbaikan + ringkasan data pelanggan (ID, alamat, telepon, kendala, koordinat)
 * supaya petugas bisa langsung melihat & menghubungi pelanggan tanpa berpindah modul.
 * Kesetaraan (equals/hashCode) hanya berdasarkan nomor tiket, seperti di versi Dart.
 */
class Ticket(
    val ticket: String,
    val nama: String,
    val status: String,
    val area: String,
    val idPelanggan: String = "",
    val alamat: String = "",
    val telepon: String = "",
    val kendala: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
) {
    fun toMap(): JsonMap = mapOf(
        "ticket" to ticket,
        "nama" to nama,
        "status" to status,
        "area" to area,
        "idPelanggan" to idPelanggan,
        "alamat" to alamat,
        "telepon" to telepon,
        "kendala" to kendala,
        "lat" to lat,
        "lng" to lng,
    )

    override fun equals(other: Any?): Boolean = this === other || (other is Ticket && other.ticket == ticket)
    override fun hashCode(): Int = ticket.hashCode()

    companion object {
        fun fromMap(json: JsonMap): Ticket {
            fun pick(keys: List<String>): String {
                for (k in keys) {
                    val v = json[k]
                    if (v != null && v.toString().trim().isNotEmpty()) return v.toString().trim()
                }
                return ""
            }

            fun pickDouble(keys: List<String>): Double? {
                for (k in keys) {
                    val v = json[k] ?: continue
                    val parsed = v.toString().toDoubleOrNull()
                    if (parsed != null) return parsed
                }
                return null
            }

            return Ticket(
                ticket = json["ticket"]?.toString() ?: "",
                nama = json["nama"]?.toString() ?: "",
                status = json["status"]?.toString() ?: "OPEN",
                area = json["area"]?.toString() ?: "",
                idPelanggan = pick(listOf("idPelanggan", "idpel", "idPel", "id_pelanggan", "IDPEL", "noPelanggan", "no_pelanggan")),
                alamat = pick(listOf("alamat", "ALAMAT", "address")),
                telepon = pick(listOf("telepon", "telp", "noHp", "no_hp", "hp", "whatsapp", "wa", "phone", "noTelepon", "TELEPON")),
                kendala = pick(listOf("kendala", "keluhan", "masalah", "deskripsi", "keterangan", "pengaduan", "KENDALA")),
                lat = pickDouble(listOf("lat", "latitude", "LAT")),
                lng = pickDouble(listOf("lng", "lon", "longitude", "LNG")),
            )
        }
    }
}

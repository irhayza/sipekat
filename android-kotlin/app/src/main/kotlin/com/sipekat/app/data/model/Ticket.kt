package com.sipekat.app.data.model

/** Tiket laporan perbaikan — port dari Dart Ticket model. */
data class Ticket(
    val ticket: String     = "",
    val nama: String       = "",
    val status: String     = "OPEN",
    val area: String       = "",
    val idPelanggan: String= "",
    val alamat: String     = "",
    val telepon: String    = "",
    val kendala: String    = "",
    val lat: Double?       = null,
    val lng: Double?       = null
) {
    override fun equals(other: Any?) = other is Ticket && other.ticket == ticket
    override fun hashCode()          = ticket.hashCode()

    companion object {
        fun fromMap(raw: Map<String, Any?>): Ticket {
            fun pick(vararg keys: String): String {
                for (k in keys) {
                    val v = raw[k]?.toString()?.trim()
                    if (!v.isNullOrEmpty()) return v
                }
                return ""
            }
            fun pickDouble(vararg keys: String): Double? {
                for (k in keys) { val d = raw[k]?.toString()?.toDoubleOrNull(); if (d != null) return d }
                return null
            }
            return Ticket(
                ticket      = raw["ticket"]?.toString() ?: "",
                nama        = raw["nama"]?.toString()   ?: "",
                status      = raw["status"]?.toString() ?: "OPEN",
                area        = raw["area"]?.toString()   ?: "",
                idPelanggan = pick("idPelanggan","idpel","idPel","id_pelanggan","IDPEL","noPelanggan","no_pelanggan"),
                alamat      = pick("alamat","ALAMAT","address"),
                telepon     = pick("telepon","telp","noHp","no_hp","hp","whatsapp","wa","phone","noTelepon","TELEPON"),
                kendala     = pick("kendala","keluhan","masalah","deskripsi","keterangan","pengaduan","KENDALA"),
                lat         = pickDouble("lat","latitude","LAT"),
                lng         = pickDouble("lng","lon","longitude","LNG")
            )
        }
    }
}

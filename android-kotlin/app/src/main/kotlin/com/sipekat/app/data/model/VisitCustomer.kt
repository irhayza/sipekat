package com.sipekat.app.data.model

data class VisitCustomer(
    val idpel: String    = "",
    val nama: String     = "",
    val alamat: String   = "",
    val nomgrt: String   = "",
    val bln: Int         = 0,
    val rupiah: Double   = 0.0,
    val status: String   = "Y",
    val telepon: String  = "",
    val kendala: String  = "",
    val lat: Double?     = null,
    val lng: Double?     = null
) {
    companion object {
        fun fromMap(j: Map<String, Any?>): VisitCustomer = VisitCustomer(
            idpel   = j["IDPEL"]?.toString()  ?: "",
            nama    = j["NAMA"]?.toString()    ?: "",
            alamat  = j["ALAMAT"]?.toString()  ?: "",
            nomgrt  = j["NOMGRT"]?.toString()  ?: "",
            bln     = j["BLN"]?.toString()?.toIntOrNull()    ?: 0,
            rupiah  = j["RUPIAH"]?.toString()?.toDoubleOrNull() ?: 0.0,
            status  = j["STATUS"]?.toString()  ?: "Y",
            telepon = j["TELEPON"]?.toString() ?: "",
            kendala = j["KONDISI"]?.toString() ?: "",
            lat     = j["LAT"]?.toString()?.toDoubleOrNull(),
            lng     = j["LNG"]?.toString()?.toDoubleOrNull()
        )
    }
}

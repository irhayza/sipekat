package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap

data class Officer(val nama: String, val area: String) {
    fun toMap(): JsonMap = mapOf("nama" to nama, "area" to area)

    companion object {
        fun fromMap(json: JsonMap): Officer = Officer(
            nama = json["nama"]?.toString() ?: "",
            area = json["area"]?.toString() ?: "",
        )
    }
}

package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList

data class DropdownOptions(
    val petugas: List<Officer>,
    val jenis: List<String>,
    val penanganan: List<String>,
) {
    fun toMap(): JsonMap = mapOf(
        "petugas" to petugas.map { it.toMap() },
        "jenis" to jenis,
        "penanganan" to penanganan,
    )

    companion object {
        fun fromMap(json: JsonMap): DropdownOptions = DropdownOptions(
            petugas = (json["petugas"].asList() ?: emptyList<Any?>())
                .mapNotNull { it.asJsonMap()?.let(Officer.Companion::fromMap) },
            jenis = (json["jenis"].asList() ?: emptyList<Any?>()).map { it.toString() },
            penanganan = (json["penanganan"].asList() ?: emptyList<Any?>()).map { it.toString() },
        )
    }
}

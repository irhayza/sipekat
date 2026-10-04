package com.sipekat.app.data.model

data class DropdownOptions(
    val petugas: List<Officer>  = emptyList(),
    val jenis: List<String>     = emptyList(),
    val penanganan: List<String> = emptyList()
)

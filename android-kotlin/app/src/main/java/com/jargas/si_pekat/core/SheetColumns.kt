package com.jargas.si_pekat.core

/**
 * `update_row_cells` di Code.gs MELEWATI kolom yang namanya tidak ada di sheet tanpa galat (respons tetap "success").
 * Server yang sudah di-patch mengembalikan daftar `skipped`; helper ini memeriksa apakah ada kolom WAJIB yang terlewati
 * supaya petugas diberi peringatan, bukan mengira semua tersimpan.
 */
object SheetColumns {
    fun missing(skipped: List<String>, required: Collection<String>): List<String> {
        val req = required.map { it.trim().lowercase() }.toSet()
        return skipped.filter { it.trim().lowercase() in req }
    }

    fun warning(missing: List<String>): String? =
        if (missing.isEmpty()) null
        else "Data tersimpan, TETAPI kolom ${missing.joinToString(", ")} tidak ditemukan di sheet sehingga isinya tidak tersimpan. Hubungi admin."
}

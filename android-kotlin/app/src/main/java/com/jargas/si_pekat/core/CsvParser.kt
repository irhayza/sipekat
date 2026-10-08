package com.jargas.si_pekat.core

object CsvParser {
    /** Memecah teks CSV menjadi baris (CRLF/CR → LF). Baris kosong tetap ada; saring di pemanggil. */
    fun splitLines(text: String): List<String> =
        text.replace("\r\n", "\n").replace("\r", "\n").split("\n")

    /**
     * Parse satu baris CSV (menangani field berkutip).
     * [handleEscapedQuotes] = true → `""` di dalam kutipan menjadi satu tanda kutip (Auth & OCR).
     * false → perilaku lama modul Kunjungan (setiap `"` hanya menukar status kutip).
     */
    fun parseRow(line: String, handleEscapedQuotes: Boolean = true): MutableList<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (handleEscapedQuotes && inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else {
                current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    /** Indeks kolom pertama yang namanya (trim, uppercase) cocok dengan salah satu kandidat. */
    fun findColumnIndex(columns: List<String>, candidates: List<String>): Int? {
        val wanted = candidates.map { it.uppercase() }
        for ((index, col) in columns.withIndex()) {
            if (col.trim().uppercase() in wanted) return index
        }
        return null
    }
}

/** Ambil sel ke-[index] (trim) atau "" bila indeks null/di luar jangkauan. */
fun List<String>.cell(index: Int?): String =
    if (index != null && index >= 0 && index < size) this[index].trim() else ""

fun normalizeComparable(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")

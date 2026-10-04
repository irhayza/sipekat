package com.sipekat.app.util

/** CSV parser — port dari Dart _parseCsvRow() */
object CsvParser {

    fun parseRow(line: String): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    fun parseCsv(text: String): List<List<String>> =
        text.replace("\r\n", "\n").replace("\r", "\n")
            .split("\n")
            .filter { it.isNotBlank() }
            .map { parseRow(it) }

    fun findColumnIndex(columns: List<String>, candidates: List<String>): Int? {
        val norm = candidates.map { it.uppercase() }
        for (i in columns.indices) {
            if (columns[i].trim().uppercase() in norm) return i
        }
        return null
    }
}

package com.jargas.si_pekat.core

/**
 * Setara `Validation.sanitize()` di GAS: trim + uppercase, dan beri awalan `'` bila nilai diawali
 * karakter pembuka formula Google Sheets (=, +, -, @) agar input bebas tidak bisa menyuntik formula.
 */
fun sanitizeForSheet(value: String?): String {
    val s = (value ?: "").trim().uppercase()
    if (s.isEmpty()) return s
    return if (s[0] in "=+-@") "'$s" else s
}

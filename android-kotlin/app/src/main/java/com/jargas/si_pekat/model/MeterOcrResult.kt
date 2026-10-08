package com.jargas.si_pekat.model

/** Hasil pembacaan angka meter (Gemini maupun OCR lokal). */
data class MeterOcrResult(
    val digits: String = "",
    val rawText: String = "",
    val candidates: List<String> = emptyList(),
    val isConfident: Boolean = false,
    val score: Double = 0.0,
) {
    val hasDigits: Boolean get() = digits.isNotEmpty()
}

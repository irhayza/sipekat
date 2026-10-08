package com.jargas.si_pekat.data

import com.jargas.si_pekat.model.MeterOcrResult

/** Abstraksi hasil ML Kit supaya logika peringkat angka bisa diuji tanpa Android. Ukuran dalam piksel. */
class OcrBox(val text: String, val width: Float, val height: Float, val confidence: Float?)
class OcrLine(val box: OcrBox, val elements: List<OcrBox>)
class OcrBlock(val box: OcrBox, val lines: List<OcrLine>)
class OcrDoc(val text: String, val blocks: List<OcrBlock>)

/**
 * Peringkat kandidat angka meter dari teks OCR. Port 1:1 dari MeterOcrService di ocr_meter_service.dart
 * (normalisasi huruf mirip angka, penalti substitusi via LCS, bobot visual, kecocokan dengan stand bulan lalu).
 */
object MeterOcrParser {
    private const val DIGITS = 5 // AppConfig.meterDigitCount
    private const val MAX_USAGE = 5000 // AppConfig.meterMaxPlausibleUsage

    private class Candidate(
        val digits: String,
        val confidence: Double,
        val visualWeight: Double,
        val scoreBias: Double,
        val segmentCount: Int,
    ) {
        fun rank(previousStand: Int?): Double {
            var score = confidence * 100
            score += visualWeight
            score += scoreBias

            score += when (digits.length) {
                DIGITS -> 50
                DIGITS - 1 -> 45
                DIGITS + 1 -> 10
                else -> -30
            }
            if (segmentCount in (DIGITS - 1)..DIGITS) score += 18 else if (segmentCount >= 2) score += 8
            if (digits.startsWith("0") && digits.length == DIGITS) score += 10

            val value = digits.toIntOrNull() ?: return score - 120
            if (previousStand != null) {
                if (value >= previousStand) {
                    val diff = value - previousStand
                    score += 38
                    if (diff <= 250) score += 24
                    else if (diff <= 1500) score += 16
                    else if (diff <= MAX_USAGE) score += 10
                    else score -= (diff / 1500.0).coerceIn(0.0, 30.0)
                } else {
                    score -= 40 + ((previousStand - value) / 100.0).coerceIn(0.0, 60.0)
                }
                if (digits.length == DIGITS + 1 && previousStand < 10000) score -= 22
            }
            return score
        }
    }

    private fun visualWeight(w: Float, h: Float): Double =
        (h / 12.0).coerceIn(0.0, 22.0) + (w / 30.0).coerceIn(0.0, 12.0)

    private val HAS_DIGIT = Regex("\\d")

    fun parse(doc: OcrDoc, previousStand: Int?, scoreBias: Double = 0.0): MeterOcrResult {
        val candidates = mutableListOf<Candidate>()
        var maxLineHeight = 0.0
        var maxElementHeight = 0.0
        for (block in doc.blocks) for (line in block.lines) {
            if (HAS_DIGIT.containsMatchIn(line.box.text)) {
                if (line.box.height > maxLineHeight) maxLineHeight = line.box.height.toDouble()
                for (el in line.elements) {
                    if (HAS_DIGIT.containsMatchIn(el.text) && el.height > maxElementHeight) maxElementHeight = el.height.toDouble()
                }
            }
        }

        collect(candidates, doc.text, 0.35, scoreBias - 6, 0.0)

        for (block in doc.blocks) {
            val bw = visualWeight(block.box.width, block.box.height)
            collect(candidates, block.box.text, 0.48, scoreBias + bw * 0.35, bw)
            for (line in block.lines) {
                val lw = visualWeight(line.box.width, line.box.height)
                var lineBoost = 0.0
                if (maxLineHeight > 0 && HAS_DIGIT.containsMatchIn(line.box.text)) {
                    val ratio = line.box.height / maxLineHeight
                    lineBoost = if (ratio >= 0.8) 40.0 else if (ratio >= 0.5) 20.0 else 0.0
                }
                collect(candidates, line.box.text, line.box.confidence?.toDouble() ?: 0.66, scoreBias + lw * 0.65 + lineBoost, lw)
                for (el in line.elements) {
                    val ew = visualWeight(el.width, el.height)
                    var elBoost = 0.0
                    if (maxElementHeight > 0 && HAS_DIGIT.containsMatchIn(el.text)) {
                        val ratio = el.height / maxElementHeight
                        elBoost = if (ratio >= 0.8) 40.0 else if (ratio >= 0.5) 20.0 else 0.0
                    }
                    collect(candidates, el.text, el.confidence?.toDouble() ?: 0.82, scoreBias + ew + elBoost, ew)
                }
            }
        }
        return build(candidates, doc.text, previousStand)
    }

    fun parseRawText(rawText: String, previousStand: Int? = null): MeterOcrResult {
        val candidates = mutableListOf<Candidate>()
        collect(candidates, rawText, 0.6, 0.0, 0.0)
        return build(candidates, rawText, previousStand)
    }

    private fun build(candidates: List<Candidate>, rawText: String, previousStand: Int?): MeterOcrResult {
        if (candidates.isEmpty()) return MeterOcrResult(rawText = rawText.trim())
        val byDigits = LinkedHashMap<String, Candidate>()
        for (c in candidates) {
            val existing = byDigits[c.digits]
            if (existing == null || c.rank(previousStand) > existing.rank(previousStand)) byDigits[c.digits] = c
        }
        val ranked = byDigits.values.sortedByDescending { it.rank(previousStand) }
        val best = ranked.first()
        val parsed = best.digits.toIntOrNull()
        val matchesHistory = previousStand == null || (parsed != null && parsed >= previousStand)
        val confident = best.confidence >= 0.58 && best.digits.length >= DIGITS - 1 && best.digits.length <= DIGITS && matchesHistory
        return MeterOcrResult(
            digits = best.digits,
            rawText = rawText.trim(),
            candidates = ranked.map { it.digits }.take(5),
            isConfident = confident,
            score = best.rank(previousStand),
        )
    }

    private fun collect(target: MutableList<Candidate>, source: String, confidence: Double, scoreBias: Double, visualWeight: Double) {
        val normalized = normalize(source)
        if (normalized.isEmpty()) return
        val parts = normalized.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val sourceDigits = source.replace(Regex("[^0-9]"), "")

        fun add(digits: String, segmentCount: Int, extraBias: Double = 0.0) {
            if (digits.length !in 4..6) return
            val substituted = digits.length - lcs(digits, sourceDigits)
            target.add(Candidate(digits, confidence, visualWeight, scoreBias + extraBias - substituted * 15.0, segmentCount))
        }

        for (p in parts) add(p, 1)
        if (parts.size >= 2) add(parts.joinToString(""), parts.size, 10.0)
        if (parts.size >= DIGITS - 1) {
            for (start in parts.indices) {
                for (length in listOf(DIGITS - 1, DIGITS)) {
                    val end = start + length
                    if (end > parts.size) continue
                    val slice = parts.subList(start, end)
                    add(slice.joinToString(""), slice.size, 18.0 - start * 2)
                }
            }
        }
    }

    internal fun lcs(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        if (m == 0 || n == 0) return 0
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (s1[i - 1] == s2[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        return dp[m][n]
    }

    private val SUBS = mapOf('O' to '0', 'Q' to '0', 'D' to '0', 'I' to '1', 'L' to '1', '|' to '1', 'Z' to '2', 'S' to '5', 'G' to '6', 'B' to '8')

    internal fun normalize(value: String): String {
        if (value.isBlank()) return ""
        val sb = StringBuilder()
        for (ch in value.uppercase()) {
            when {
                ch.isDigit() && ch in '0'..'9' -> sb.append(ch)
                SUBS.containsKey(ch) -> sb.append(SUBS[ch])
                else -> sb.append(' ')
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}

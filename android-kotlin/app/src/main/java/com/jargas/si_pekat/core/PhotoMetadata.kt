package com.jargas.si_pekat.core

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Menentukan waktu pengambilan foto: (1) EXIF asli, (2) pola nama file (WhatsApp/kamera/screenshot),
 * (3) waktu sekarang. Port dari utils/photo_metadata_resolver.dart. Hasil = epoch millis.
 */
object PhotoMetadata {
    suspend fun resolveCapturedTime(file: File, filename: String): Long = withContext(Dispatchers.IO) {
        readExifTime(file) ?: parseFromFilename(filename, System.currentTimeMillis()) ?: System.currentTimeMillis()
    }

    /** yymmddhhmm (10 digit, tanpa pemisah). Contoh: 21 Jul 2026 14:35 → "2607211435". */
    fun toFilenameStamp(millis: Long): String = SimpleDateFormat("yyMMddHHmm", Locale.US).format(millis)

    /** "yyyy-MM-dd HH:mm:ss" untuk kolom waktu di sheet. */
    fun formatSheet(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(millis)

    /** "yyyy-MM-dd HH:mm" untuk label di UI. */
    fun formatShort(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(millis)

    private fun readExifTime(file: File): Long? = try {
        val exif = ExifInterface(file.absolutePath)
        val candidates = listOf(
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME,
        )
        candidates.firstNotNullOfOrNull { tag -> exif.getAttribute(tag)?.let(::parseExifDate) }
    } catch (e: Exception) {
        null
    }

    private val EXIF_RE = Regex("^(\\d{4}):(\\d{2}):(\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2})")

    private fun parseExifDate(raw: String): Long? {
        val m = EXIF_RE.find(raw.trim()) ?: return null
        val g = m.groupValues.drop(1).map { it.toInt() }
        return makeTime(g[0], g[1], g[2], g[3], g[4], g[5])
    }

    private fun makeTime(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): Long? {
        if (mo !in 1..12 || d !in 1..31 || h !in 0..23 || mi !in 0..59 || s !in 0..59) return null
        return Calendar.getInstance().apply { clear(); set(y, mo - 1, d, h, mi, s) }.timeInMillis
    }

    private val FULL_PATTERNS = listOf(
        Regex("(\\d{4})(\\d{2})(\\d{2})[_-](\\d{2})(\\d{2})(\\d{2})"),
        Regex("(\\d{4})-(\\d{2})-(\\d{2})[ _-](\\d{2})[.\\-:](\\d{2})[.\\-:](\\d{2})"),
        Regex("(\\d{4})_(\\d{2})_(\\d{2})[ _-](\\d{2})_(\\d{2})_(\\d{2})"),
        Regex("whatsapp\\s+image\\s+(\\d{4})-(\\d{2})-(\\d{2})\\s+at\\s+(\\d{2})[.\\-:](\\d{2})[.\\-:](\\d{2})"),
    )
    private val WA_NO_SECONDS = Regex("whatsapp\\s+image\\s+(\\d{4})-(\\d{2})-(\\d{2})\\s+at\\s+(\\d{2})[.\\-:](\\d{2})")
    private val IMG_WA = Regex("img-(\\d{4})(\\d{2})(\\d{2})-wa\\d*")

    fun parseFromFilename(filename: String, fallback: Long): Long? {
        val name = filename.trim().lowercase()
        if (name.isEmpty()) return null
        for (p in FULL_PATTERNS) {
            val m = p.find(name) ?: continue
            val g = m.groupValues.drop(1).map { it.toInt() }
            makeTime(g[0], g[1], g[2], g[3], g[4], g[5])?.let { return it }
        }
        WA_NO_SECONDS.find(name)?.let { m ->
            val g = m.groupValues.drop(1).map { it.toInt() }
            makeTime(g[0], g[1], g[2], g[3], g[4], 0)?.let { return it }
        }
        IMG_WA.find(name)?.let { m ->
            val g = m.groupValues.drop(1).map { it.toInt() }
            val c = Calendar.getInstance().apply { timeInMillis = fallback }
            makeTime(g[0], g[1], g[2], c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))?.let { return it }
        }
        return null
    }
}

package com.jargas.si_pekat.data

import android.util.Base64
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.AuthExpiredException
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.model.MeterOcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException

/**
 * OCR cloud (Gemini Vision) lewat server: aplikasi hanya mengirim foto ke Code.gs (action `gemini_ocr`);
 * kunci API, prompt, dan model disimpan di Script Properties/kode server — tidak ada di aplikasi.
 */
object GeminiOcrService {
    /** Melempar Exception berpesan ramah pengguna bila gagal; [AuthExpiredException] bila token sesi ditolak. */
    suspend fun scanImage(file: File): MeterOcrResult = withContext(Dispatchers.IO) {
        val b64 = Base64.encodeToString(ImageCompress.jpegForWidth(file, 1024, 85), Base64.NO_WRAP)
        val res = try {
            GasHttp.postJson(AppConfig.GAS_READ_SHEET_URL, mapOf("action" to "gemini_ocr", "image_base64" to b64), 45)
        } catch (e: InterruptedIOException) {
            throw Exception("Waktu tunggu OCR cloud habis, periksa koneksi internet.")
        } catch (e: IOException) {
            throw Exception("Gagal menghubungi server OCR. Periksa koneksi internet.")
        }
        if (res.code != 200) throw Exception("Server OCR error (Status ${res.code}).")

        val d = try { Json.decode(res.body).asJsonMap() } catch (e: Exception) { null }
            ?: throw Exception("Respons server OCR tidak valid.")
        if (d["status"] == null && d["message"]?.toString()?.startsWith("Unknown action") == true) {
            throw Exception("Server belum mendukung OCR cloud (pasang Code.patched.gs).")
        }
        if (d["status"] != "success") {
            val msg = d["message"]?.toString() ?: "OCR cloud gagal."
            if (d["code"] == "AUTH") throw AuthExpiredException(msg)
            throw Exception(msg)
        }
        toResult(d["angka"]?.toString().orEmpty(), d["jumlah_digit"], d["terbaca"] == true)
    }

    /** Terjemahkan jawaban server menjadi hasil OCR (tanpa Android → bisa diuji). */
    internal fun toResult(rawAngka: String, jumlahDigit: Any?, terbaca: Boolean): MeterOcrResult {
        val digits = rawAngka.replace(Regex("[^0-9]"), "")
        val n = AppConfig.METER_DIGIT_COUNT
        val validLength = digits.length == n || digits.length == n - 1
        val jumlah = (jumlahDigit as? Number)?.toInt() ?: jumlahDigit?.toString()?.toIntOrNull()
        val note = "Gemini AI: angka=${rawAngka.ifEmpty { "-" }} • jumlah_digit=${jumlah ?: "-"} • terbaca=${if (terbaca) "ya" else "tidak"}"
        return MeterOcrResult(
            digits = digits,
            rawText = note,
            candidates = if (digits.isEmpty()) emptyList() else listOf(digits),
            isConfident = terbaca && validLength,
            score = if (terbaca && validLength) 95.0 else if (digits.isNotEmpty()) 55.0 else 0.0,
        )
    }
}

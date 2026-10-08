package com.jargas.si_pekat.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.jargas.si_pekat.core.AppContext
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.core.PhotoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

class PreparedMeterPhoto(val file: File, val capturedAt: Long, val overlayLines: List<String>)

/** Foto bukti stand meter: salinan kerja + foto unggah bercap waktu/koordinat/IDPEL. Port dari ocr_photo_proof_service.dart. */
object PhotoProofService {
    private fun dir(): File = File(AppContext.app.cacheDir, "photos").apply { mkdirs() }

    /** Foto yang akan dikirim/antre offline: di filesDir (tahan lama), bukan cache yang bisa dibersihkan sistem. */
    private fun pendingDir(): File = File(AppContext.app.filesDir, "meter_pending").apply { mkdirs() }

    private fun safeId(v: String): String = v.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "meter" }

    suspend fun createWorkingCopy(source: File, idpel: String): File = withContext(Dispatchers.IO) {
        val ext = source.extension.lowercase().ifEmpty { "jpg" }
        source.copyTo(File(dir(), "meter-work-${safeId(idpel)}-${System.currentTimeMillis()}.$ext"), overwrite = true)
    }

    suspend fun prepareForUpload(
        source: File, idpel: String, latitude: Double?, longitude: Double?, capturedAt: Long?,
    ): PreparedMeterPhoto = withContext(Dispatchers.Default) {
        var bmp = ImageCompress.decodeOriented(source, 1600) ?: throw Exception("Foto tidak bisa diproses")
        if (bmp.width > 1600) bmp = Bitmap.createScaledBitmap(bmp, 1600, (bmp.height * (1600f / bmp.width)).toInt().coerceAtLeast(1), true)
        val mutable = bmp.copy(Bitmap.Config.ARGB_8888, true)

        val captured = capturedAt ?: System.currentTimeMillis()
        val lines = listOf(
            "Waktu: ${PhotoMetadata.formatSheet(captured)}",
            if (latitude != null && longitude != null) "Lat: ${String.format(Locale.US, "%.6f", latitude)} | Lng: ${String.format(Locale.US, "%.6f", longitude)}"
            else "Lat/Lng: belum tersedia",
            "IDPEL: $idpel",
        )

        val canvas = Canvas(mutable)
        val textPx = (mutable.width / 66f).coerceAtLeast(18f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = textPx; typeface = Typeface.DEFAULT }
        val lineHeight = textPx + 6f
        val blockHeight = lines.size * lineHeight + 20f
        val startY = (mutable.height - blockHeight).coerceAtLeast(0f)
        canvas.drawRect(0f, startY, mutable.width.toFloat(), mutable.height.toFloat(), Paint().apply { color = Color.argb(180, 0, 0, 0) })
        var y = startY + 10f + textPx
        for (line in lines) { canvas.drawText(line, 12f, y, paint); y += lineHeight }

        val out = File(pendingDir(), "meter-upload-${safeId(idpel)}-$captured.jpg")
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            mutable.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            out.writeBytes(stream.toByteArray())
        }
        PreparedMeterPhoto(out, captured, lines)
    }

    suspend fun deleteFile(file: File?) = withContext(Dispatchers.IO) {
        try { if (file != null && file.exists()) file.delete() } catch (_: Exception) {}
        Unit
    }
}

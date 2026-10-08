package com.jargas.si_pekat.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Kompresi foto (modul Kunjungan/Perbaikan): perbaiki orientasi EXIF, perkecil ke sisi terpanjang 800 px,
 * lalu turunkan kualitas JPEG bertahap sampai ≤ 250 KB. Port dari kunjungan_image_compress_service.dart.
 */
object ImageCompress {
    private const val MAX_DIMENSION = 800
    private const val QUALITY = 55
    private const val TARGET_MAX_BYTES = 250 * 1024

    /** Kompres file dan kembalikan data-URI base64 siap kirim. */
    suspend fun compressFileToBase64(file: File): String = withContext(Dispatchers.Default) {
        val bytes = compress(file.readBytes())
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Foto bukti: perbaiki orientasi EXIF, perkecil ke LEBAR [maxWidth] (tinggi mengikuti, tidak pernah diperbesar),
     * lalu turunkan kualitas JPEG bertahap sampai ≤ [targetMaxBytes]. Skala berdasarkan lebar sama dengan Flutter
     * (minWidth: 800, minHeight: 0) sehingga foto potret tidak lebih kecil dari versi lama.
     */
    fun compress(
        original: ByteArray,
        maxWidth: Int = MAX_DIMENSION,
        targetMaxBytes: Int = TARGET_MAX_BYTES,
        initialQuality: Int = QUALITY,
        minQuality: Int = 30,
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return original

        // Hemat memori: decode di resolusi yang sudah diperkecil (pangkat 2) tetapi sisi terpendek tetap ≥ maxWidth,
        // sehingga hasil akhir berlebar maxWidth baik foto lanskap maupun potret.
        var sample = 1
        val shortest = minOf(bounds.outWidth, bounds.outHeight)
        while (shortest / (sample * 2) >= maxWidth) sample *= 2
        var bmp = BitmapFactory.decodeByteArray(original, 0, original.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return original

        bmp = applyExifOrientation(original, bmp)

        if (bmp.width > maxWidth) {
            val h = (bmp.height * (maxWidth.toFloat() / bmp.width)).roundToInt().coerceAtLeast(1)
            bmp = Bitmap.createScaledBitmap(bmp, maxWidth, h, true)
        }

        var quality = initialQuality
        var out = encodeJpeg(bmp, quality)
        while (out.size > targetMaxBytes && quality > minQuality) {
            quality -= 5
            out = encodeJpeg(bmp, quality)
        }
        return out
    }

    private fun encodeJpeg(bmp: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    internal fun applyExifOrientation(original: ByteArray, bmp: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(ByteArrayInputStream(original))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bmp
        }
        return try {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        } catch (e: OutOfMemoryError) {
            bmp
        }
    }

    /** Bitmap berorientasi benar dari berkas, sisi terpanjang ≤ [maxDim] (hemat memori via inSampleSize). */
    fun decodeOriented(file: File, maxDim: Int): Bitmap? {
        val bytes = file.readBytes()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= maxDim) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return applyExifOrientation(bytes, bmp)
    }

    /** JPEG untuk dikirim ke Gemini: orientasi benar, lebar ≤ [maxWidth], kualitas [quality]. */
    fun jpegForWidth(file: File, maxWidth: Int = 1024, quality: Int = 85): ByteArray {
        val raw = file.readBytes()
        var bmp = decodeOriented(file, maxWidth * 2) ?: return raw
        if (bmp.width > maxWidth) {
            val h = (bmp.height * (maxWidth.toFloat() / bmp.width)).roundToInt().coerceAtLeast(1)
            bmp = Bitmap.createScaledBitmap(bmp, maxWidth, h, true)
        }
        return encodeJpeg(bmp, quality)
    }
}

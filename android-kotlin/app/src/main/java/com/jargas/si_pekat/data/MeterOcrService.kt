package com.jargas.si_pekat.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.model.MeterOcrResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * OCR lokal (ML Kit, tanpa internet) sebagai cadangan Gemini. Foto diolah menjadi beberapa varian
 * (potong jendela angka, kontras, ambang biner normal & terbalik) lalu hasil terbaik dipilih lewat [MeterOcrParser].
 */
class MeterOcrService {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private class Variant(val bitmap: Bitmap, val bias: Double)

    suspend fun scanImage(file: File, previousStand: Int?): MeterOcrResult = withContext(Dispatchers.Default) {
        val variants = buildVariants(file)
        try {
            val results = coroutineScope {
                variants.map { v ->
                    async {
                        try {
                            val text = recognizer.process(InputImage.fromBitmap(v.bitmap, 0)).await()
                            MeterOcrParser.parse(toDoc(text), previousStand, v.bias)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            MeterOcrResult()
                        }
                    }
                }.awaitAll()
            }
            var best = MeterOcrResult()
            for (r in results) if (!best.hasDigits || r.score > best.score) best = r
            best
        } finally {
            variants.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        }
    }

    fun close() = recognizer.close()

    private fun toDoc(t: Text): OcrDoc {
        fun box(text: String, r: android.graphics.Rect?, conf: Float?) =
            OcrBox(text, (r?.width() ?: 0).toFloat(), (r?.height() ?: 0).toFloat(), conf)
        return OcrDoc(
            t.text,
            t.textBlocks.map { b ->
                OcrBlock(
                    box(b.text, b.boundingBox, null),
                    b.lines.map { l -> OcrLine(box(l.text, l.boundingBox, l.confidence), l.elements.map { e -> box(e.text, e.boundingBox, e.confidence) }) },
                )
            },
        )
    }

    // ── Varian gambar ───────────────────────────────────────────────────────

    private fun buildVariants(file: File): List<Variant> {
        val base = ImageCompress.decodeOriented(file, 1800) ?: return emptyList()
        val variants = mutableListOf(Variant(base, 0.0))
        try {
            val window = upscale(crop(base, 0.12, 0.18, 0.76, 0.60))
            val digits = upscale(crop(base, 0.15, 0.28, 0.60, 0.34))
            fun add(bmp: Bitmap, bias: Double) { variants.add(Variant(bmp, bias)) }

            add(window, 18.0)
            add(contrast(window), 22.0)
            add(threshold(window, 0.42f, invert = false), 25.0)
            add(threshold(window, 0.58f, invert = true), 28.0)
            add(threshold(window, 0.70f, invert = true), 30.0)

            add(digits, 32.0)
            add(contrast(digits), 38.0)
            add(threshold(digits, 0.42f, invert = false), 42.0)
            add(threshold(digits, 0.58f, invert = true), 46.0)
            add(threshold(digits, 0.70f, invert = true), 48.0)
        } catch (e: Exception) {
            // Varian tambahan gagal dibuat (mis. memori) → cukup pakai gambar asli.
        }
        return variants
    }

    private fun crop(src: Bitmap, left: Double, top: Double, width: Double, height: Double): Bitmap {
        val x = Math.round(src.width * left).toInt().coerceIn(0, src.width - 1)
        val y = Math.round(src.height * top).toInt().coerceIn(0, src.height - 1)
        val w = Math.round(src.width * width).toInt().coerceIn(1, src.width - x)
        val h = Math.round(src.height * height).toInt().coerceIn(1, src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }

    private fun upscale(src: Bitmap): Bitmap {
        if (src.width >= 1200) return src
        val h = (src.height * (1200f / src.width)).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, 1200, h, true)
    }

    private fun luminance(px: Int): Float = (0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)) / 255f

    /** Abu-abu + kontras 1,65 + kecerahan 1,12 (setara adjustColor di paket image). */
    private fun contrast(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val v = ((((luminance(px[i]) - 0.5f) * 1.65f) + 0.5f) * 1.12f).coerceIn(0f, 1f)
            val g = (v * 255).toInt()
            px[i] = Color.rgb(g, g, g)
        }
        return createBitmap(w, h).also { it.setPixels(px, 0, w, 0, 0, w, h) }
    }

    /** Abu-abu (opsional dibalik) + kontras 180% + ambang luminans → hitam/putih murni. */
    private fun threshold(src: Bitmap, level: Float, invert: Boolean): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            var l = luminance(px[i])
            if (invert) l = 1f - l
            l = ((l - 0.5f) * 1.8f + 0.5f).coerceIn(0f, 1f)
            px[i] = if (l >= level) Color.WHITE else Color.BLACK
        }
        return createBitmap(w, h).also { it.setPixels(px, 0, w, 0, 0, w, h) }
    }
}

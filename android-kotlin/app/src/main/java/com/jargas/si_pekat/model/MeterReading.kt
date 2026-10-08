package com.jargas.si_pekat.model

import com.jargas.si_pekat.core.JsonMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Bacaan stand meter (tabel lokal stand_meter_offline). Port dari models/ocr/meter_reading.dart. */
class MeterReading(
    id: String? = null,
    val pelangganId: String,
    val petugasId: String,
    val standAngka: Int,
    val fotoPath: String? = null,
    var fotoUrl: String? = null,
    var fotoDriveId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val catatan: String? = null,
    bulan: String? = null,
    /** Epoch millis. */
    createdAt: Long? = null,
    var isSynced: Boolean = false,
    val minus: Int = 0,
) {
    val id: String = id ?: UUID.randomUUID().toString()
    val bulan: String = bulan ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val createdAt: Long = createdAt ?: System.currentTimeMillis()

    /** Payload untuk bridge GAS/backend (tanpa path foto lokal). */
    fun toApiMap(): JsonMap = mapOf(
        "id" to id, "pelanggan_id" to pelangganId, "stand_angka" to standAngka,
        "foto_url" to fotoUrl, "foto_gdrive_id" to fotoDriveId,
        "lat" to lat, "lng" to lng, "catatan" to catatan, "bulan" to bulan, "minus" to minus,
    )

    companion object {
        private const val ISO = "yyyy-MM-dd'T'HH:mm:ss.SSS"

        fun formatIso(millis: Long): String = SimpleDateFormat(ISO, Locale.US).format(Date(millis))

        /** Membaca ISO-8601 dari Dart (boleh berakhiran mikrodetik); pecahan detik diabaikan. */
        fun parseIso(text: String?): Long? {
            if (text.isNullOrBlank() || text.length < 19) return null
            return try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(text.substring(0, 19))?.time
            } catch (e: Exception) {
                null
            }
        }

        /** Tampilan "yyyy-MM-dd HH:mm" seperti `createdAt.toString().substring(0, 16)` di Dart. */
        fun formatShort(millis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
    }
}

package com.jargas.si_pekat.data

import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.PendingPhotos
import com.jargas.si_pekat.core.ServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jargas.si_pekat.model.MeterReading

/**
 * Mengirim satu laporan pending (hasil antrean offline) ke server: unggah foto → tulis ke sheet yang sesuai.
 * Port dari _SyncProgressDialogState._startSync (kunjungan_dashboard_screen.dart).
 * Melempar ApiException (jaringan) / ServerException (ditolak server) bila gagal.
 */
object KunjunganSync {
    /** Foto antrean: dari berkas (format baru) atau base64 di entri lama. Berkas hilang → galat jelas, bukan terkirim tanpa foto. */
    private suspend fun photoOf(item: Map<String, Any?>, pathKey: String, b64Key: String, label: String, idpel: String): String {
        val path = item[pathKey]?.toString().orEmpty()
        if (path.isNotEmpty()) {
            if (!PendingPhotos.exists(path)) throw ServerException("Foto $label untuk $idpel hilang dari perangkat. Hapus entri ini dari riwayat lalu input ulang.")
            return withContext(Dispatchers.IO) { PendingPhotos.readDataUri(path) }
        }
        return item[b64Key]?.toString().orEmpty()
    }

    /** Mengembalikan daftar kolom wajib yang tidak ditemukan di sheet (kosong = semua tersimpan). */
    suspend fun sendOne(item: Map<String, Any?>): List<String> {
        val idpel = item["idpel"]?.toString() ?: ""
        val actionType = item["actionType"]?.toString() ?: ""

        var urlSeb = ""
        var urlSes = ""
        val b64Seb = photoOf(item, "fotoSebPath", "fotoSebBase64", "sebelum", idpel)
        val b64Ses = photoOf(item, "fotoSesPath", "fotoSesBase64", "sesudah", idpel)
        if (b64Seb.isNotEmpty()) urlSeb = KunjunganApiService.uploadPhoto(b64Seb, "sync_${idpel}_seb.jpg")
        if (b64Ses.isNotEmpty()) urlSes = KunjunganApiService.uploadPhoto(b64Ses, "sync_${idpel}_ses.jpg")

        val actionLower = actionType.lowercase()
        val isPembukaan = actionLower == "pasang kembali" || actionLower == "buka segel"
        val isPasang = actionLower == "pasang kembali"
        val isCabut = actionLower == "cabut"

        val lat = item["lat"]?.toString() ?: ""
        val lng = item["lng"]?.toString() ?: ""
        val timeBefore = item["timeBefore"]?.toString() ?: ""
        val timeAfter = item["timeAfter"]?.toString() ?: ""
        val nomgrtBru = item["nomgrtBru"]?.toString() ?: ""
        val stMgrt = item["stMgrt"]?.toString() ?: ""

        if (isPembukaan) {
            val updates = buildMap<String, Any?> {
                put("PENGADUAN_RP", actionType.uppercase())
                put("STATUS_RP", "SELESAI")
                put("Foto_Seb_RP", urlSeb)
                put("Foto_Ses_RP", urlSes)
                if (timeBefore.isNotEmpty()) put("Waktu_Seb_RP", timeBefore)
                if (timeAfter.isNotEmpty()) put("Waktu_Ses_RP", timeAfter)
                if (lat.isNotEmpty()) put("Latitude_RP", lat)
                if (lng.isNotEmpty()) put("Longitude_RP", lng)
                if (isPasang) { put("No_MGRT_Baru_RP", nomgrtBru); put("Angka_MGRT_Baru_RP", stMgrt) }
                put("Petugas_RP", AppSessionCache.officerNama)
            }
            return KunjunganApiService.updateRowCells(
                sheetName = AppConfig.PENGADUAN_SHEET_NAME, idpel = idpel,
                keyColumn = "IDPEL_RP", filterColumn = "PENGADUAN_RP", filterValue = actionType.uppercase(), updates = updates,
                required = updates.keys,
            )
        } else {
            val updates = buildMap<String, Any?> {
                put("KJG", actionType.uppercase())
                put("LINK_SEB", urlSeb)
                put("LINK_SES", urlSes)
                if (lat.isNotEmpty()) put("LATITUDE_KJG", lat)
                if (lng.isNotEmpty()) put("LONGITUDE_KJG", lng)
                if (timeBefore.isNotEmpty() && !isCabut) put("EXIF_SEB", timeBefore)
                if (timeAfter.isNotEmpty() && !isCabut) put("EXIF_SES", timeAfter)
                if (isCabut && nomgrtBru.isNotEmpty()) put("NOMGRT_BARU", nomgrtBru)
                if (isCabut && stMgrt.isNotEmpty()) put("STMGRT_BARU", stMgrt)
                put("TGL_KJG", timeAfter.ifEmpty { MeterReading.formatIso(System.currentTimeMillis()) })
                put("ORDER", "2")
            }
            return KunjunganApiService.updateRowCells(sheetName = AppConfig.OCR_SHEET_NAME, idpel = idpel, updates = updates, required = updates.keys)
        }
    }
}

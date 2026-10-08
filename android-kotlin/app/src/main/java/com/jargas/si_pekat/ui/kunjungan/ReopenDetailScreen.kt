package com.jargas.si_pekat.ui.kunjungan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.AppScope
import com.jargas.si_pekat.core.GpsResult
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.core.PhotoMetadata
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.core.sanitizeForSheet
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.KunjunganApiService
import com.jargas.si_pekat.data.KunjunganLocalCache
import com.jargas.si_pekat.data.PerbaikanApiService
import com.jargas.si_pekat.model.VisitCustomer
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val REOPEN_ACTIONS = listOf("BUKA SEGEL", "PASANG KEMBALI")

/** Form Pembukaan Aliran (buka segel / pasang kembali). Port dari reopen_detail_screen.dart. */
@Composable
fun ReopenDetailScreen(customer: VisitCustomer, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifier = rememberNotifier()
    val gps = rememberVisitGps()

    var action by remember { mutableStateOf("BUKA SEGEL") }
    var noMgrt by remember { mutableStateOf("") }
    var stMgrt by remember { mutableStateOf("") }
    var noMgrtErr by remember { mutableStateOf<String?>(null) }
    var stMgrtErr by remember { mutableStateOf<String?>(null) }
    var photoBefore by remember { mutableStateOf<PickedPhoto?>(null) }
    var photoAfter by remember { mutableStateOf<PickedPhoto?>(null) }
    var saving by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var resultWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { gps.prefetch() }
    BackHandler(enabled = saving) {}
    BackHandler(enabled = !saving && result == null) { onClose() }

    val pickBefore = rememberPhotoPicker({ photoBefore = it }, { notifier.show(it, true) })
    val pickAfter = rememberPhotoPicker({ photoAfter = it }, { notifier.show(it, true) })
    val isPasang = action == "PASANG KEMBALI"

    suspend fun queueOffline(payload: Map<String, Any?>) {
        KunjunganLocalCache.savePendingSubmission(payload)
        KunjunganLocalCache.saveHistorySubmission(payload.toMutableMap().also { it["status"] = "pending" })
        KunjunganLocalCache.removeCustomerFromCache(customer.idpel)
        saving = false
        result = true
    }

    fun submit() {
        noMgrtErr = if (isPasang && noMgrt.isBlank()) "No MGRT Baru wajib diisi" else null
        stMgrtErr = if (isPasang && stMgrt.isBlank()) "Stand Meter Baru wajib diisi" else null
        if (noMgrtErr != null || stMgrtErr != null) return

        val before = photoBefore
        val after = photoAfter
        if (before == null || after == null) {
            notifier.show("Foto sebelum dan sesudah pengerjaan wajib dilampirkan.", true)
            return
        }

        scope.launch {
            saving = true
            step = "Mengunci koordinat GPS..."
            when (gps.ensurePermission()) {
                GpsResult.SERVICE_DISABLED -> { saving = false; notifier.show("GPS wajib aktif sebelum laporan dikirim.", true); return@launch }
                GpsResult.PERMISSION_DENIED -> { saving = false; notifier.show("Izin lokasi ditolak. Berikan izin lokasi agar koordinat GPS dapat dicatat.", true); return@launch }
                GpsResult.PERMISSION_DENIED_FOREVER -> { saving = false; notifier.show("Izin lokasi ditolak permanen. Buka Pengaturan Aplikasi > Izin > Lokasi, lalu aktifkan secara manual.", true); return@launch }
                else -> Unit
            }
            gps.ensureFix()?.let { saving = false; notifier.show(it, true); return@launch }

            val lat = coord(gps.lat)
            val lng = coord(gps.lng)
            val timeBefore = PhotoMetadata.formatSheet(before.capturedAt)
            val timeAfter = PhotoMetadata.formatSheet(after.capturedAt)
            val metadata = "GPS: $lat, $lng | Sebelum: $timeBefore | Sesudah: $timeAfter"
            val legacyPayload = mapOf<String, Any?>(
                "idpel" to customer.idpel, "nama" to customer.nama, "alamat" to customer.alamat,
                "actionType" to action.lowercase(),
                "nomgrtBru" to if (isPasang) noMgrt.trim() else null,
                "stMgrt" to if (isPasang) stMgrt.trim() else null,
                "metadata" to metadata, "lat" to lat, "lng" to lng,
                "timeBefore" to timeBefore, "timeAfter" to timeAfter,
            )

            // 1) Pastikan tiket masih OPEN/PROSES di server
            step = "Memeriksa status tiket di server..."
            var assumedOffline = false
            try {
                val found = KunjunganApiService.findRow(
                    sheetName = AppConfig.PENGADUAN_SHEET_NAME, idpel = customer.idpel,
                    keyColumn = "IDPEL_RP", filterColumn = "PENGADUAN_RP", filterValue = action,
                )
                val current = (found.row["STATUS_RP"]?.toString() ?: found.row["Status"]?.toString() ?: "").trim().uppercase()
                if (current.isNotEmpty() && current != "OPEN" && current != "PROSES") {
                    saving = false
                    notifier.show("Tiket pelanggan ${customer.idpel} sudah berstatus $current, tidak bisa diproses ulang dari sini.", true)
                    return@launch
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerException) {
                saving = false; notifier.show(e.message ?: "Gagal memeriksa status tiket.", true); return@launch
            } catch (e: Exception) {
                assumedOffline = true // ApiException / lainnya: kemungkinan besar tidak ada koneksi
            }

            // 2) Kompres foto (paralel)
            step = "Mengompresi foto..."
            val encoded: Pair<String, String> = try {
                coroutineScope {
                    val d1 = async { ImageCompress.compressFileToBase64(before.file) }
                    val d2 = async { ImageCompress.compressFileToBase64(after.file) }
                    d1.await() to d2.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saving = false; notifier.show("Gagal memproses gambar: ${e.message ?: e}", true); return@launch
            }
            val b64Before = encoded.first
            val b64After = encoded.second
            val offlinePayload = legacyPayload + mapOf("fotoSebBase64" to b64Before, "fotoSesBase64" to b64After)
            if (assumedOffline) { queueOffline(offlinePayload); return@launch }

            // 3) Unggah foto (paralel)
            step = "Mengunggah foto..."
            val uploaded: Pair<String, String> = try {
                coroutineScope {
                    val u1 = async { KunjunganApiService.uploadPhoto(b64Before, "${PhotoMetadata.toFilenameStamp(before.capturedAt)}_seb.jpg") }
                    val u2 = async { KunjunganApiService.uploadPhoto(b64After, "${PhotoMetadata.toFilenameStamp(after.capturedAt)}_ses.jpg") }
                    u1.await() to u2.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerException) {
                saving = false; notifier.show("Gagal upload foto: ${e.message}", true); return@launch
            } catch (e: ApiException) {
                queueOffline(offlinePayload); return@launch
            }
            val urlSeb = uploaded.first
            val urlSes = uploaded.second

            // 4) Tulis ke sheet Pengaduan (nama kolom baru *_RP + nama lama untuk kompatibilitas)
            val officer = AppSessionCache.officerNama
            val updates = buildMap<String, Any?> {
                put("STATUS_RP", "SELESAI"); put("Tindakan_RP", action)
                put("Foto_Seb_RP", urlSeb); put("Foto_Ses_RP", urlSes)
                if (timeBefore.isNotEmpty()) put("Waktu_Seb_RP", timeBefore)
                if (timeAfter.isNotEmpty()) put("Waktu_Ses_RP", timeAfter)
                if (lat.isNotEmpty()) put("Latitude_RP", lat)
                if (lng.isNotEmpty()) put("Longitude_RP", lng)
                if (isPasang) { put("No_MGRT_Baru_RP", sanitizeForSheet(noMgrt.trim())); put("Angka_MGRT_Baru_RP", sanitizeForSheet(stMgrt.trim())) }
                put("Petugas_RP", officer)

                put("Status", "SELESAI"); put("Keterangan", action)
                put("Foto Sebelum", urlSeb); put("Foto Sesudah", urlSes)
                if (timeBefore.isNotEmpty()) put("Waktu Sebelum", timeBefore)
                if (timeAfter.isNotEmpty()) put("Waktu Sesudah", timeAfter)
                if (lat.isNotEmpty()) put("Latitude", lat)
                if (lng.isNotEmpty()) put("Longitude", lng)
                if (isPasang) { put("No MGRT Baru", noMgrt.trim()); put("Angka MGRT Baru", stMgrt.trim()) }
                put("Petugas", officer); put("Tindakan", action)
            }
            step = "Menyimpan ke server..."
            try {
                val missing = KunjunganApiService.updateRowCells(
                    sheetName = AppConfig.PENGADUAN_SHEET_NAME, idpel = customer.idpel,
                    keyColumn = "IDPEL_RP", filterColumn = "PENGADUAN_RP", filterValue = action, updates = updates,
                    required = updates.keys.filter { it.endsWith("_RP") },
                )
                resultWarning = SheetColumns.warning(missing)
                // Notifikasi WA/Telegram (best-effort; tetap berjalan walau layar ditutup)
                AppScope.launch {
                    PerbaikanApiService.notifySelesai(
                        ticket = customer.ticket.ifEmpty { customer.idpel }, idpel = customer.idpel, nama = customer.nama, pengaduan = customer.kendala,
                        petugas = officer, tindakan = action, fotoSeb = urlSeb, fotoSes = urlSes,
                        lat = lat.ifEmpty { null }, lng = lng.ifEmpty { null },
                    )
                }
                val sent = offlinePayload.toMutableMap().also {
                    it["fotoSebBase64"] = urlSeb; it["fotoSesBase64"] = urlSes; it["status"] = "sent"
                }
                KunjunganLocalCache.saveHistorySubmission(sent)
                KunjunganLocalCache.removeCustomerFromCache(customer.idpel)
                saving = false
                result = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerException) {
                saving = false; notifier.show(e.message ?: "Gagal menyimpan.", true)
            } catch (e: ApiException) {
                queueOffline(offlinePayload)
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Canvas,
        snackbarHost = { NotifierHost(notifier) },
        topBar = { ModuleTopBar("Form Pembukaan Aliran", customer.idpel, onBack = onClose, backEnabled = !saving) },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
                GpsWarnings(gps) { scope.launch { gps.prefetch() } }

                FormSectionHeader("Detail Pelanggan")
                CustomerInfoCard(
                    idPelanggan = customer.idpel, nama = customer.nama, alamat = customer.alamat,
                    telepon = customer.telepon.ifEmpty { null }, kendala = customer.kendala.ifEmpty { null },
                    lat = customer.lat, lng = customer.lng, extraRows = listOf("No MGRT" to customer.nomgrt),
                )
                Spacer(Modifier.height(12.dp))

                FormSectionHeader("Pilih Tindakan Pembukaan")
                ActionRadioCard(REOPEN_ACTIONS, action) { action = it; noMgrtErr = null; stMgrtErr = null }
                Spacer(Modifier.height(12.dp))

                if (isPasang) {
                    FormSectionHeader("Data MGRT Baru")
                    MgrtBaruCard(noMgrt, { noMgrt = it; noMgrtErr = null }, noMgrtErr, stMgrt, { stMgrt = it; stMgrtErr = null }, stMgrtErr)
                    Spacer(Modifier.height(12.dp))
                }

                FormSectionHeader("Dokumentasi Pembukaan")
                FormCard {
                    PhotoCaptureTile("Foto Sebelum Pengerjaan *", photoBefore, pickBefore, placeholder = "Ketuk untuk ambil foto")
                    Spacer(Modifier.height(16.dp)); HorizontalDivider(color = AppColors.Line); Spacer(Modifier.height(16.dp))
                    PhotoCaptureTile("Foto Sesudah Pengerjaan *", photoAfter, pickAfter, placeholder = "Ketuk untuk ambil foto")
                }
                Spacer(Modifier.height(20.dp))
                SubmitButton("Simpan Pembukaan", Icons.Rounded.CheckCircle) { submit() }
                Spacer(Modifier.height(24.dp))
            }
            if (saving) SavingOverlay(step)
        }
    }

    result?.let { offline ->
        SubmissionResultDialog("Pembukaan Dicatat!", customer.idpel, action, offline, resultWarning) { result = null; onClose() }
    }
}

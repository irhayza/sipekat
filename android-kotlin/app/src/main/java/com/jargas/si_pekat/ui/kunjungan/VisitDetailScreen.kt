package com.jargas.si_pekat.ui.kunjungan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.GpsResult
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.core.PhotoMetadata
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.data.KunjunganApiService
import com.jargas.si_pekat.data.KunjunganLocalCache
import com.jargas.si_pekat.model.MeterReading
import com.jargas.si_pekat.model.VisitCustomer
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val ACTIONS = listOf("CABUT", "SEGEL", "LUNAS", "RUMAH TERTUTUP")

/** Form Kunjungan Aliran (penutupan). Port dari visit_detail_screen.dart. */
@Composable
fun VisitDetailScreen(customer: VisitCustomer, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifier = rememberNotifier()
    val gps = rememberVisitGps()

    var action by remember { mutableStateOf("CABUT") }
    var noMgrt by remember { mutableStateOf("") }
    var stMgrt by remember { mutableStateOf("") }
    var noMgrtErr by remember { mutableStateOf<String?>(null) }
    var stMgrtErr by remember { mutableStateOf<String?>(null) }
    var photoBefore by remember { mutableStateOf<PickedPhoto?>(null) }
    var photoAfter by remember { mutableStateOf<PickedPhoto?>(null) }
    var saving by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) } // true = offline
    var resultWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { gps.prefetch() }
    BackHandler(enabled = saving) {}
    BackHandler(enabled = !saving && result == null) { onClose() }

    val pickBefore = rememberPhotoPicker({ photoBefore = it }, { notifier.show(it, true) })
    val pickAfter = rememberPhotoPicker({ photoAfter = it }, { notifier.show(it, true) })
    val isCabut = action == "CABUT"

    suspend fun queueOffline(payload: Map<String, Any?>) {
        KunjunganLocalCache.savePendingSubmission(payload)
        val history = payload.toMutableMap().also { it["status"] = "pending" }
        KunjunganLocalCache.saveHistorySubmission(history)
        KunjunganLocalCache.removeCustomerFromCache(customer.idpel)
        saving = false
        result = true
    }

    fun submit() {
        noMgrtErr = if (isCabut && noMgrt.isBlank()) "No MGRT Baru wajib diisi" else null
        stMgrtErr = if (isCabut && stMgrt.isBlank()) "Stand Meter Baru wajib diisi" else null
        if (noMgrtErr != null || stMgrtErr != null) return

        val needsBefore = action != "RUMAH TERTUTUP"
        val before = photoBefore
        val after = photoAfter
        if ((needsBefore && before == null) || after == null) {
            notifier.show(if (needsBefore) "Foto sebelum dan sesudah pengerjaan wajib dilampirkan." else "Foto sesudah pengerjaan wajib dilampirkan.", true)
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

            step = "Mengompresi foto..."
            val encoded: Pair<String?, String> = try {
                coroutineScope {
                    val d1 = async { if (needsBefore && before != null) ImageCompress.compressFileToBase64(before.file) else null }
                    val d2 = async { ImageCompress.compressFileToBase64(after.file) }
                    d1.await() to d2.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saving = false; notifier.show("Gagal memproses gambar: ${e.message ?: e}", true); return@launch
            }
            val b64Before: String? = encoded.first
            val b64After: String = encoded.second

            step = "Mengirim data ke server..."
            val lat = coord(gps.lat)
            val lng = coord(gps.lng)
            val timeBefore = before?.let { PhotoMetadata.formatSheet(it.capturedAt) } ?: ""
            val timeAfter = PhotoMetadata.formatSheet(after.capturedAt)
            val metadata = "GPS: $lat, $lng | Sebelum: $timeBefore | Sesudah: $timeAfter"

            val offlinePayload = mapOf<String, Any?>(
                "idpel" to customer.idpel, "nama" to customer.nama, "alamat" to customer.alamat,
                "actionType" to action.lowercase(),
                "nomgrtBru" to if (isCabut) noMgrt.trim() else null,
                "stMgrt" to if (isCabut) stMgrt.trim() else null,
                "metadata" to metadata, "lat" to lat, "lng" to lng,
                "timeBefore" to timeBefore, "timeAfter" to timeAfter,
                "fotoSebBase64" to (b64Before ?: ""), "fotoSesBase64" to b64After,
            )

            // Unggah foto (paralel)
            step = "Mengunggah foto..."
            val uploaded: Pair<String, String> = try {
                coroutineScope {
                    val u1 = async { if (b64Before != null && before != null) KunjunganApiService.uploadPhoto(b64Before, "${PhotoMetadata.toFilenameStamp(before.capturedAt)}_seb.jpg") else "" }
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

            // Kolom yang ditimpa di sheet dbase
            val updates = buildMap<String, Any?> {
                put("KJG", action)
                put("LINK_SEB", urlSeb)
                put("LINK_SES", urlSes)
                if (lat.isNotEmpty()) put("LATITUDE_KJG", lat)
                if (lng.isNotEmpty()) put("LONGITUDE_KJG", lng)
                if (timeBefore.isNotEmpty() && !isCabut) put("EXIF_SEB", timeBefore)
                if (timeAfter.isNotEmpty() && !isCabut) put("EXIF_SES", timeAfter)
                if (isCabut && noMgrt.trim().isNotEmpty()) put("NOMGRT_BARU", noMgrt.trim())
                if (isCabut && stMgrt.trim().isNotEmpty()) put("STMGRT_BARU", stMgrt.trim())
                put("TGL_KJG", timeAfter.ifEmpty { MeterReading.formatIso(System.currentTimeMillis()) })
                put("ORDER", "2")
            }
            try {
                val missing = KunjunganApiService.updateRowCells(sheetName = "dbase", idpel = customer.idpel, updates = updates, required = updates.keys)
                resultWarning = SheetColumns.warning(missing)
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
        topBar = { ModuleTopBar("Form Kunjungan Aliran", customer.idpel, onBack = onClose, backEnabled = !saving) },
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

                FormSectionHeader("Pilih Tindakan Penutupan")
                ActionRadioCard(ACTIONS, action) { action = it; noMgrtErr = null; stMgrtErr = null }
                Spacer(Modifier.height(12.dp))

                if (isCabut) {
                    FormSectionHeader("Data MGRT Baru")
                    MgrtBaruCard(noMgrt, { noMgrt = it; noMgrtErr = null }, noMgrtErr, stMgrt, { stMgrt = it; stMgrtErr = null }, stMgrtErr)
                    Spacer(Modifier.height(12.dp))
                }

                FormSectionHeader("Dokumentasi Kunjungan")
                FormCard {
                    if (action != "RUMAH TERTUTUP") {
                        PhotoCaptureTile("Foto Sebelum Pengerjaan *", photoBefore, pickBefore, placeholder = "Ketuk untuk ambil foto")
                        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = AppColors.Line); Spacer(Modifier.height(16.dp))
                    }
                    PhotoCaptureTile("Foto Sesudah Pengerjaan *", photoAfter, pickAfter, placeholder = "Ketuk untuk ambil foto")
                }
                Spacer(Modifier.height(20.dp))
                SubmitButton("Simpan Kunjungan", Icons.Rounded.Save) { submit() }
                Spacer(Modifier.height(24.dp))
            }
            if (saving) SavingOverlay(step)
        }
    }

    result?.let { offline ->
        SubmissionResultDialog("Kunjungan Dicatat!", customer.idpel, action, offline, resultWarning) { result = null; onClose() }
    }
}

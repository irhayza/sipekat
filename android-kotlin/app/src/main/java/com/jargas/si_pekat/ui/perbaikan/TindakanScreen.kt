package com.jargas.si_pekat.ui.perbaikan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.AppScope
import com.jargas.si_pekat.core.ContactLauncher
import com.jargas.si_pekat.core.ImageCompress
import com.jargas.si_pekat.core.LocationService
import com.jargas.si_pekat.core.PhotoMetadata
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.core.SheetColumns
import com.jargas.si_pekat.core.sanitizeForSheet
import com.jargas.si_pekat.data.ActivityHistory
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.PerbaikanApiService
import com.jargas.si_pekat.model.DropdownOptions
import com.jargas.si_pekat.model.Ticket
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private fun Throwable.userMessage(fallback: String): String =
    if (this is ApiException || this is ServerException) (message ?: fallback) else fallback

/** Form Tindakan & Dokumentasi untuk satu tiket. Port dari _TindakanScreen (perbaikan_screen.dart). */
@Composable
fun TindakanScreen(ticket: Ticket, options: DropdownOptions?, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifier = rememberNotifier()
    val officerName = AppSessionCache.officerNama

    var jenis by remember { mutableStateOf<String?>(null) }
    var tindakan by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(AppConfig.STATUS_SELESAI) }
    var noMgrt by remember { mutableStateOf("") }
    var angkaMgrt by remember { mutableStateOf("") }
    var noMgrtBaru by remember { mutableStateOf("") }
    var angkaMgrtBaru by remember { mutableStateOf("") }

    var jenisErr by remember { mutableStateOf<String?>(null) }
    var tindakanErr by remember { mutableStateOf<String?>(null) }
    var noMgrtErr by remember { mutableStateOf<String?>(null) }
    var angkaMgrtErr by remember { mutableStateOf<String?>(null) }
    var noMgrtBaruErr by remember { mutableStateOf<String?>(null) }
    var angkaMgrtBaruErr by remember { mutableStateOf<String?>(null) }

    var photoBefore by remember { mutableStateOf<PickedPhoto?>(null) }
    var photoAfter by remember { mutableStateOf<PickedPhoto?>(null) }

    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }
    var gpsState by remember { mutableStateOf<GpsBannerState?>(null) }
    var gpsMessage by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var savingStep by remember { mutableStateOf<String?>(null) }
    var successStatus by remember { mutableStateOf<String?>(null) }
    var successWarning by remember { mutableStateOf<String?>(null) }

    val jenisOptions = options?.jenis?.takeIf { it.isNotEmpty() } ?: listOf("Data Kosong")
    val tindakanOptions = options?.penanganan?.takeIf { it.isNotEmpty() } ?: listOf("Data Kosong")

    val isSelesai = status == AppConfig.STATUS_SELESAI
    val tindakanKey = tindakan?.lowercase()?.trim()
    val mgrtRequired = tindakanKey != null && tindakanKey in AppConfig.mgrtTriggers
    val mgrtBaruRequired = tindakanKey == AppConfig.MGRT_BARU_TRIGGER

    // Prefetch GPS diam-diam agar pengiriman lebih cepat.
    LaunchedEffect(Unit) {
        val outcome = LocationService.getCurrentLocation(context)
        if (outcome.isOk) { gpsLat = outcome.location!!.latitude; gpsLng = outcome.location.longitude }
    }

    BackHandler(enabled = saving) {}
    BackHandler(enabled = !saving && successStatus == null) { onClose() }

    val pickBefore = rememberPhotoPicker(onPicked = { photoBefore = it }, onError = { notifier.show(it, isError = true) })
    val pickAfter = rememberPhotoPicker(onPicked = { photoAfter = it }, onError = { notifier.show(it, isError = true) })

    fun onTindakanChanged(v: String) {
        tindakan = v; tindakanErr = null
        val k = v.lowercase().trim()
        if (k !in AppConfig.mgrtTriggers) { noMgrt = ""; angkaMgrt = "" }
        if (k != AppConfig.MGRT_BARU_TRIGGER) { noMgrtBaru = ""; angkaMgrtBaru = "" }
    }

    fun validate(): Boolean {
        jenisErr = if (jenis == null) "Jenis pengaduan wajib dipilih" else null
        tindakanErr = if (tindakan == null) "Tindakan penanganan wajib dipilih" else null
        noMgrtErr = if (mgrtRequired && noMgrt.isBlank()) "No MGRT Lama wajib diisi" else null
        angkaMgrtErr = if (mgrtRequired && angkaMgrt.isBlank()) "Angka MGRT Lama wajib diisi" else null
        noMgrtBaruErr = if (mgrtBaruRequired && noMgrtBaru.isBlank()) "No MGRT Baru wajib diisi" else null
        angkaMgrtBaruErr = if (mgrtBaruRequired && angkaMgrtBaru.isBlank()) "Angka MGRT Baru wajib diisi" else null
        return listOf(jenisErr, tindakanErr, noMgrtErr, angkaMgrtErr, noMgrtBaruErr, angkaMgrtBaruErr).all { it == null }
    }

    suspend fun encode(p: PickedPhoto): String {
        p.base64?.let { return it }
        val encoded = ImageCompress.compressFileToBase64(p.file)
        p.base64 = encoded
        return encoded
    }

    fun submit() {
        if (saving) return
        if (!validate()) return
        if (isSelesai && (photoBefore == null || photoAfter == null)) {
            notifier.show("Foto sebelum & sesudah wajib untuk status SELESAI.", isError = true)
            return
        }
        scope.launch {
            saving = true
            savingStep = "Memeriksa status tiket di server..."

            // 1) Pastikan tiket belum SELESAI di server (mencegah proses ulang).
            try {
                val row = PerbaikanApiService.findRow(AppConfig.PENGADUAN_SHEET_NAME, ticket.ticket, "Ticket_RP")
                val current = (row["STATUS_RP"]?.toString() ?: row["Status"]?.toString() ?: "").trim().uppercase()
                if (current == AppConfig.STATUS_SELESAI.uppercase()) {
                    saving = false
                    notifier.show("Tiket ${ticket.ticket} sudah berstatus SELESAI, tidak bisa diproses ulang.", isError = true)
                    return@launch
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saving = false
                notifier.show("${e.userMessage("Gagal memeriksa status tiket.")} Coba lagi.", isError = true)
                return@launch
            }

            // 2) Kunci GPS
            savingStep = "Mengunci titik GPS..."
            gpsState = GpsBannerState.Loading
            gpsMessage = "Mengunci koordinat Anda..."
            val gps = LocationService.getCurrentLocation(context)
            if (gps.isOk) {
                gpsLat = gps.location!!.latitude; gpsLng = gps.location.longitude
                gpsState = GpsBannerState.Ok
                gpsMessage = "Lokasi terkunci: ${"%.5f".format(java.util.Locale.US, gpsLat)}, ${"%.5f".format(java.util.Locale.US, gpsLng)}"
            } else {
                gpsLat = null; gpsLng = null
                gpsState = GpsBannerState.Warning
                gpsMessage = LocationService.messageFor(gps.result)
            }
            val lat = gpsLat?.toString() ?: ""
            val lng = gpsLng?.toString() ?: ""
            val before = photoBefore
            val after = photoAfter
            val waktuSeb = before?.let { PhotoMetadata.formatSheet(it.capturedAt) } ?: ""
            val waktuSes = after?.let { PhotoMetadata.formatSheet(it.capturedAt) } ?: ""

            // 3) Kompres + unggah foto (paralel) — hanya untuk SELESAI
            var urlSeb: String? = null
            var urlSes: String? = null
            if (isSelesai && before != null && after != null) {
                savingStep = "Mengompresi & mengunggah foto..."
                try {
                    val urls: List<String> = coroutineScope {
                        val enc = listOf(async { encode(before) }, async { encode(after) }).awaitAll()
                        listOf(
                            async { PerbaikanApiService.uploadPhoto(enc[0], "${PhotoMetadata.toFilenameStamp(before.capturedAt)}_before.jpg") },
                            async { PerbaikanApiService.uploadPhoto(enc[1], "${PhotoMetadata.toFilenameStamp(after.capturedAt)}_after.jpg") },
                        ).awaitAll()
                    }
                    urlSeb = urls[0]; urlSes = urls[1]
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    saving = false
                    notifier.show(e.userMessage("Gagal upload foto: ${e.message ?: e}"), isError = true)
                    return@launch
                }
            }

            // 4) Susun kolom yang ditimpa (nama kolom baru *_RP + nama kolom lama untuk kompatibilitas)
            val updates = buildMap<String, Any?> {
                put("STATUS_RP", sanitizeForSheet(status))
                put("Petugas_RP", sanitizeForSheet(officerName))
                put("PENGADUAN_RP", sanitizeForSheet(jenis))
                put("Tindakan_RP", sanitizeForSheet(tindakan))
                if (isSelesai) {
                    put("Foto_Seb_RP", urlSeb ?: ""); put("Foto_Ses_RP", urlSes ?: "")
                    put("Waktu_Seb_RP", waktuSeb); put("Waktu_Ses_RP", waktuSes)
                    if (lat.isNotEmpty() && lng.isNotEmpty()) { put("Latitude_RP", lat); put("Longitude_RP", lng) }
                }
                // Sesuai Code.gs: meter LAMA → NoMGRT_RP/Angka_MGRT_RP, meter BARU → No_MGRT_Baru_RP/Angka_MGRT_Baru_RP
                if (mgrtRequired) { put("NoMGRT_RP", sanitizeForSheet(noMgrt.trim())); put("Angka_MGRT_RP", sanitizeForSheet(angkaMgrt.trim())) }
                if (mgrtBaruRequired) { put("No_MGRT_Baru_RP", sanitizeForSheet(noMgrtBaru.trim())); put("Angka_MGRT_Baru_RP", sanitizeForSheet(angkaMgrtBaru.trim())) }

                put("Status", sanitizeForSheet(status))
                put("Petugas", sanitizeForSheet(officerName))
                put("Pengaduan", sanitizeForSheet(jenis))
                put("Keterangan", sanitizeForSheet(tindakan))
                if (isSelesai) {
                    put("Foto Sebelum", urlSeb ?: ""); put("Foto Sesudah", urlSes ?: "")
                    put("Waktu Sebelum", waktuSeb); put("Waktu Sesudah", waktuSes)
                }
                put("Latitude", lat); put("Longitude", lng)
                if (mgrtRequired) { put("No MGRT", sanitizeForSheet(noMgrt.trim())); put("Angka MGRT", sanitizeForSheet(angkaMgrt.trim())) }
                if (mgrtBaruRequired) { put("No MGRT Baru", sanitizeForSheet(noMgrtBaru.trim())); put("Angka MGRT Baru", sanitizeForSheet(angkaMgrtBaru.trim())) }
            }

            // 5) Simpan
            savingStep = "Menyimpan ke server..."
            try {
                // Kolom *_RP wajib ada di sheet; nama lama (Status, Petugas, ...) hanya untuk kompatibilitas.
                val missing = PerbaikanApiService.updateRowCells(
                    sheetName = AppConfig.PENGADUAN_SHEET_NAME,
                    keyValue = ticket.ticket,
                    updates = updates,
                    keyColumn = "Ticket_RP",
                    required = updates.keys.filter { it.endsWith("_RP") },
                )
                successWarning = SheetColumns.warning(missing)
                // Notifikasi best-effort: tidak boleh menggagalkan submit, dan harus lanjut walau layar ditutup.
                val tindakanText = tindakan ?: ""
                val sebFinal = urlSeb; val sesFinal = urlSes
                AppScope.launch {
                    PerbaikanApiService.notifySelesai(
                        ticket = ticket.ticket, idpel = ticket.idPelanggan, nama = ticket.nama, pengaduan = ticket.kendala,
                        petugas = officerName, tindakan = tindakanText, fotoSeb = sebFinal, fotoSes = sesFinal,
                        lat = lat.ifEmpty { null }, lng = lng.ifEmpty { null },
                    )
                }
                PerbaikanApiService.clearCache()
                ActivityHistory.add(ActivityHistory.Kind.PERBAIKAN, ticket.ticket, ticket.nama, ticket.idPelanggan, tindakanText, status)
                saving = false
                successStatus = status
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saving = false
                notifier.show("${e.userMessage("Gagal mengirim laporan.")} Data yang sudah diisi tetap tersimpan — coba kirim lagi.", isError = true)
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Canvas,
        snackbarHost = { NotifierHost(notifier) },
        topBar = { ModuleTopBar("Tindakan & Dokumentasi", ticket.ticket, onBack = onClose, backEnabled = !saving) },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)) {
                gpsState?.let { GpsBanner(it, gpsMessage) }

                TicketSummary(ticket)
                Spacer(Modifier.height(16.dp))

                SectionCard(Icons.Rounded.Construction, "Tindakan Penanganan") {
                    AppDropdown("Jenis Pengaduan (Validasi)", jenis, jenisOptions, { jenis = if (it == "Data Kosong") null else it; jenisErr = null }, error = jenisErr)
                    Spacer(Modifier.height(16.dp))
                    AppDropdown("Aksi / Tindakan", tindakan, tindakanOptions, { if (it == "Data Kosong") { tindakan = null } else onTindakanChanged(it) }, error = tindakanErr)

                    if (mgrtRequired) {
                        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = AppColors.Line); Spacer(Modifier.height(16.dp))
                        AppTextField(noMgrt, { noMgrt = it; noMgrtErr = null }, label = "No MGRT Lama", placeholder = "Contoh: 12345678", error = noMgrtErr)
                        Spacer(Modifier.height(16.dp))
                        AppTextField(angkaMgrt, { angkaMgrt = it.filter(Char::isDigit); angkaMgrtErr = null }, label = "Angka MGRT Lama", placeholder = "Masukkan angka meteran", keyboardType = KeyboardType.Number, error = angkaMgrtErr)
                    }
                    if (mgrtBaruRequired) {
                        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = AppColors.Line); Spacer(Modifier.height(16.dp))
                        AppTextField(noMgrtBaru, { noMgrtBaru = it; noMgrtBaruErr = null }, label = "No MGRT Baru", placeholder = "Contoh: 87654321", error = noMgrtBaruErr)
                        Spacer(Modifier.height(16.dp))
                        AppTextField(angkaMgrtBaru, { angkaMgrtBaru = it.filter(Char::isDigit); angkaMgrtBaruErr = null }, label = "Angka MGRT Baru", placeholder = "Masukkan angka meteran awal", keyboardType = KeyboardType.Number, error = angkaMgrtBaruErr)
                    }

                    Spacer(Modifier.height(16.dp))
                    val statusLabel = mapOf(
                        AppConfig.STATUS_SELESAI to "🟢 SELESAI (Pekerjaan Tuntas)",
                        AppConfig.STATUS_PROSES to "🟡 PROSES (Sedang Dikerjakan)",
                    )
                    AppDropdown("Update Status", statusLabel[status], statusLabel.values.toList(), { picked ->
                        status = statusLabel.entries.first { it.value == picked }.key
                    })
                }

                if (isSelesai) {
                    Spacer(Modifier.height(16.dp))
                    SectionCard(Icons.Rounded.CameraAlt, "Dokumentasi", subtitle = "Foto sebelum & sesudah wajib untuk status SELESAI.") {
                        PhotoCaptureTile("Foto Sebelum Pengerjaan", photoBefore, onCapture = pickBefore, placeholder = "Ketuk untuk buka kamera")
                        Spacer(Modifier.height(16.dp))
                        PhotoCaptureTile("Foto Sesudah Pengerjaan", photoAfter, onCapture = pickAfter, placeholder = "Ketuk untuk buka kamera")
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionCard(Icons.Rounded.Send, "Kirim Laporan", subtitle = "Pastikan seluruh data sudah benar sebelum mengirim.") {
                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan Penanganan", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (saving) SavingOverlay(savingStep)
        }
    }

    successStatus?.let { st ->
        SuccessSheet(
            ticket = ticket.ticket, status = st,
            message = successWarning ?: "Data penanganan sudah dicatat di server.",
            onFinish = { successStatus = null; onClose() },
            onContinue = { successStatus = null; onClose() },
        )
    }
}

@Composable
private fun TicketSummary(t: Ticket) {
    val context = LocalContext.current
    SummaryHero {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assignment, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(t.ticket, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusBadge(t.status)
        }
        Spacer(Modifier.height(8.dp))
        Text(t.nama.ifEmpty { "—" }, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (t.idPelanggan.isNotEmpty()) Text("IDPEL: ${t.idPelanggan}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        if (t.alamat.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.LocationOn, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 1.dp).size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(t.alamat, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (t.kendala.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.ReportProblem, null, tint = Color(0xFFFFB74D), modifier = Modifier.padding(top = 1.dp).size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(t.kendala, color = Color(0xFFFFCC80), fontSize = 12.sp, lineHeight = 16.sp, fontStyle = FontStyle.Italic)
            }
        }
        val hasPhone = t.telepon.isNotEmpty()
        val hasLocation = t.lat != null && t.lng != null
        if (hasPhone || hasLocation) {
            Spacer(Modifier.height(12.dp))
            FlowRowCompat {
                if (hasPhone) HeroChip(Icons.Rounded.Chat, t.telepon) {
                    val ok = ContactLauncher.openWhatsApp(context, t.telepon, "Halo ${t.nama}, kami dari petugas Jargas ingin menindaklanjuti laporan Anda (No. Tiket: ${t.ticket}).")
                    if (!ok) ContactLauncher.showLaunchFailure(context, "WhatsApp")
                }
                if (hasLocation) HeroChip(Icons.Rounded.Map, "Lihat Lokasi") {
                    if (!ContactLauncher.openMap(context, t.lat!!, t.lng!!)) ContactLauncher.showLaunchFailure(context, "aplikasi peta")
                }
            }
        }
    }
}

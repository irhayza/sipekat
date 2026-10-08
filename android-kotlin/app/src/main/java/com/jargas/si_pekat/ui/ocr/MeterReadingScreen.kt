package com.jargas.si_pekat.ui.ocr

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.core.AppScope
import com.jargas.si_pekat.core.AuthExpiredException
import com.jargas.si_pekat.core.Connectivity
import com.jargas.si_pekat.core.GpsResult
import com.jargas.si_pekat.data.*
import com.jargas.si_pekat.model.Customer
import com.jargas.si_pekat.model.MeterOcrResult
import com.jargas.si_pekat.model.MeterReading
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

private const val GEMINI_LABEL = "Gemini AI (Cloud)"
private val KONDISI_PLACEHOLDER = "Pilih Kondisi (Normal/Lainnya)"
private val KONDISI = listOf("Rumah Terkunci", "Meter Tertimbun", "Meter Tidak Terjangkau", "Ganti Meter", "Meter Dicabut")

private class SaveResult(val synced: Boolean, val message: String, val stand: Int, val keptPhoto: Boolean)

/** Layar pembacaan stand meter: foto → OCR (Gemini, cadangan ML Kit lokal) → simpan/kirim. Port dari meter_reading_screen.dart. */
@Composable
fun MeterReadingScreen(customer: Customer, officerNama: String, officerEmail: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gps = rememberVisitGps()
    val ocrService = remember { MeterOcrService() }
    val photoHolder = remember { arrayOfNulls<File>(1) }

    var stand by remember { mutableStateOf("") }
    var standError by remember { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf<File?>(null) }
    var capturedAt by remember { mutableStateOf<Long?>(null) }
    var photoSource by remember { mutableStateOf<String?>(null) }
    var ocrSource by remember { mutableStateOf<String?>(null) }
    var ocrLoading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var kondisi by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SaveResult?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val previousStand = customer.standBulanLalu ?: customer.standAwal

    LaunchedEffect(Unit) { gps.prefetch() }
    DisposableEffect(Unit) {
        onDispose {
            ocrService.close()
            AppScope.launch { PhotoProofService.deleteFile(photoHolder[0]) }
        }
    }
    BackHandler(enabled = saving) {}
    BackHandler(enabled = !saving && result == null && !showHistory) { onClose() }

    fun showMsg(msg: String, isError: Boolean = false) { statusMsg = msg; statusIsError = isError }

    fun sourceLabel(src: String?) = when (src) {
        "camera" -> "OCR lokal (foto kamera)"
        "gallery" -> "OCR lokal (foto galeri)"
        GEMINI_LABEL -> GEMINI_LABEL
        else -> "OCR lokal"
    }

    fun applyOcr(r: MeterOcrResult, source: String) {
        ocrSource = source
        if (r.hasDigits) stand = r.digits
        val label = sourceLabel(source)
        when {
            !r.hasDigits -> showMsg("Angka meter 4-5 digit belum terbaca dari $label. Ambil ulang foto dengan fokus ke angka putih berlatar hitam, lalu pastikan area angka utama terlihat besar.", true)
            r.isConfident -> showMsg("Stand meter terbaca otomatis dari $label: ${r.digits}")
            else -> showMsg("OCR otomatis dari $label menemukan ${r.digits}. Mohon periksa ulang sebelum simpan.", true)
        }
    }

    suspend fun runOcr(file: File, source: String) {
        ocrLoading = true
        var authHint: String? = null
        try {
            if (Connectivity.isOnline(context)) {
                try {
                    val g = GeminiOcrService.scanImage(file)
                    if (g.hasDigits) { ocrLoading = false; applyOcr(g, GEMINI_LABEL); return }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AuthExpiredException) {
                    authHint = "Sesi OCR cloud habis — keluar lalu masuk lagi untuk mengaktifkan Gemini. Memakai OCR lokal."
                } catch (_: Exception) {
                    // Gemini gagal/tidak yakin → lanjut ke OCR lokal.
                }
            }
            val local = ocrService.scanImage(file, previousStand)
            ocrLoading = false
            applyOcr(local, source)
            authHint?.let { statusMsg = listOfNotNull(statusMsg, it).joinToString("\n") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ocrLoading = false
            showMsg("OCR gagal: ${e.message ?: e}", true)
        }
    }

    fun onPicked(p: PickedPhoto) {
        scope.launch {
            val previous = photo
            photo = p.file
            photoHolder[0] = p.file
            capturedAt = p.capturedAt
            photoSource = if (p.fromCamera) "camera" else "gallery"
            ocrSource = photoSource
            stand = ""
            if (previous != null && previous.path != p.file.path) PhotoProofService.deleteFile(previous)
            runOcr(p.file, photoSource!!)
        }
    }

    val sources = rememberPhotoSources(::onPicked) { showMsg("Gagal menyiapkan foto: $it", true) }

    fun save() {
        if (stand.isBlank()) { standError = "Stand meter wajib diisi"; return }
        val current = stand.trim().toIntOrNull() ?: run { standError = "Hanya angka yang diizinkan"; return }
        scope.launch {
            saving = true
            statusMsg = null
            statusIsError = false
            var prepared: PreparedMeterPhoto? = null
            try {
                val isGallery = photoSource == "gallery"
                var lat: Double? = null
                var lng: Double? = null
                if (!isGallery) {
                    showMsg("Mengambil lokasi GPS...")
                    when (gps.ensurePermission()) {
                        GpsResult.SERVICE_DISABLED -> throw Exception("GPS wajib aktif sebelum data bisa dikirim.")
                        GpsResult.PERMISSION_DENIED -> throw Exception("Izin lokasi ditolak. GPS wajib diizinkan.")
                        GpsResult.PERMISSION_DENIED_FOREVER -> throw Exception("Izin lokasi ditolak permanen. Aktifkan lagi dari pengaturan aplikasi.")
                        else -> Unit
                    }
                    if (gps.ensureFix() != null) throw Exception("GPS wajib aktif dan lokasi harus berhasil diambil sebelum simpan.")
                    lat = gps.lat; lng = gps.lng
                }

                val working = photo
                if (working != null) {
                    showMsg("Menyiapkan foto bukti...")
                    prepared = PhotoProofService.prepareForUpload(working, customer.noPelanggan, lat, lng, capturedAt)
                }

                val timestamp = prepared?.capturedAt ?: capturedAt ?: System.currentTimeMillis()
                val isMinus = current < previousStand
                val keterangan = listOfNotNull(kondisi.ifEmpty { null }, if (isMinus) "Minus" else null).joinToString(", ")

                val draft = MeterReading(
                    pelangganId = customer.noPelanggan, petugasId = officerEmail, standAngka = current,
                    fotoPath = prepared?.file?.path, lat = lat, lng = lng,
                    catatan = keterangan.ifEmpty { null }, createdAt = timestamp, minus = if (isMinus) 1 else 0,
                )

                var toSave = draft
                var remoteSynced = false
                val message: String
                if (Connectivity.isOnline(context)) {
                    showMsg("Mengirim foto dan update OCRDAPEL...")
                    message = try {
                        toSave = OcrApiService.syncPendingReading(draft, officerNama)
                        remoteSynced = true
                        "Data OCR berhasil dikirim ke sheet OCRDAPEL dan foto sudah masuk Google Drive."
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Data disimpan lokal karena sinkronisasi OCRDAPEL gagal: ${e.message ?: e}"
                    }
                } else {
                    message = "Data disimpan lokal. Sinkronisasi OCRDAPEL bisa dilakukan nanti saat internet tersedia."
                }

                OcrLocalDb.saveReading(toSave)
                if (remoteSynced) PhotoProofService.deleteFile(prepared?.file)
                if (working != null && working.path != toSave.fotoPath) PhotoProofService.deleteFile(working)

                photo = null; photoHolder[0] = null; capturedAt = null
                saving = false; statusMsg = null
                result = SaveResult(remoteSynced, message, toSave.standAngka, !remoteSynced && prepared != null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val preparedPath = prepared?.file?.path
                if (preparedPath != null && preparedPath != photo?.path) PhotoProofService.deleteFile(prepared?.file)
                saving = false
                showMsg(e.message ?: e.toString(), true)
            }
        }
    }

    if (showHistory) {
        BackHandler { showHistory = false }
        CustomerHistoryScreen(customer) { showHistory = false }
        return
    }

    Scaffold(
        containerColor = Color(0xFFF0F2F5),
        topBar = {
            ModuleTopBar(customer.nama, onBack = onClose, backEnabled = !saving, containerColor = AppColors.Meter) {
                IconButton(onClick = { showHistory = true }) { Icon(Icons.Rounded.History, "Riwayat") }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Info pelanggan
            MeterCard {
                InfoLine(Icons.Rounded.Person, "Nama - ID Pelanggan", "${customer.nama} - ${customer.noPelanggan}")
                InfoLine(Icons.Rounded.LocationOn, "Alamat", customer.alamat ?: "-")
                InfoLine(Icons.Rounded.Speed, "No. Meter", customer.noMeter ?: "-")
                InfoLine(Icons.Rounded.CalendarMonth, "Stand Meter Terakhir", previousStand.toString())
                InfoLine(Icons.Rounded.GridView, "Sektor", customer.sektorLabel)
            }

            // Stand sekarang
            MeterCard {
                Text("Stand Sekarang (m3)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = stand,
                    onValueChange = { stand = it.filter(Char::isDigit); standError = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0 0 0 0 0", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = AppColors.Muted, fontSize = 24.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace) },
                    isError = standError != null,
                    supportingText = standError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = AppColors.Meter, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppColors.Canvas, unfocusedContainerColor = AppColors.Canvas, errorContainerColor = AppColors.Canvas,
                        focusedBorderColor = AppColors.Meter, unfocusedBorderColor = AppColors.Line, cursorColor = AppColors.Meter,
                    ),
                )
                if (customer.standBulanLalu != null && stand.isNotEmpty()) {
                    val diff = (stand.toIntOrNull() ?: 0) - customer.standBulanLalu
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pemakaian: $diff m3", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = if (diff < 0) AppColors.Danger else AppColors.Selesai,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (diff < 0) AppColors.DangerBg else AppColors.SelesaiBg).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // Foto
            MeterCard {
                Text("Foto Stand Meter", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                Spacer(Modifier.height(8.dp))
                val bmp = rememberFileBitmap(photo)
                if (photo != null) {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.05f)).border(1.dp, AppColors.Muted, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bmp != null) Image(bmp, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.Line).border(1.dp, AppColors.Muted, RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = AppColors.Muted, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Belum ada foto", color = AppColors.Muted, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = sources.gallery, modifier = Modifier.weight(1f), enabled = !ocrLoading && !saving,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Meter),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Meter),
                    ) { Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Upload Galeri", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = sources.camera, modifier = Modifier.weight(1f), enabled = !ocrLoading && !saving,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Meter),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Meter),
                    ) { Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ambil Foto", fontSize = 12.sp) }
                }
                if (ocrLoading) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Membaca OCR otomatis...", fontSize = 11.sp, color = AppColors.Muted)
                    }
                }
            }

            // GPS & kondisi
            MeterCard {
                Text("Status GPS & Kondisi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                Spacer(Modifier.height(8.dp))
                GpsBanner(
                    if (gps.serviceEnabled) GpsBannerState.Ok else GpsBannerState.Warning,
                    if (gps.serviceEnabled) "Kondisi GPS: Aktif" else "Kondisi GPS: Tidak Aktif",
                )
                gps.warning?.let { w ->
                    GpsBanner(GpsBannerState.Warning, w, onTap = { if (w.contains("permanen")) gps.openSettings() else scope.launch { gps.prefetch() } })
                }
                Text("Kondisi Meter (Opsional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Muted)
                Spacer(Modifier.height(6.dp))
                AppDropdown(
                    label = "Kondisi", selected = kondisi.ifEmpty { null },
                    options = listOf(KONDISI_PLACEHOLDER) + KONDISI, placeholder = KONDISI_PLACEHOLDER,
                    onSelected = { kondisi = if (it == KONDISI_PLACEHOLDER) "" else it },
                )
            }

            statusMsg?.takeIf { !saving }?.let { msg ->
                val c = if (statusIsError) AppColors.Danger else AppColors.Selesai
                Text(
                    msg, color = c, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (statusIsError) AppColors.DangerBg else AppColors.SelesaiBg)
                        .border(1.dp, c, RoundedCornerShape(8.dp)).padding(10.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                enabled = !saving && !ocrLoading,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Meter, contentColor = Color.White),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (saving) (statusMsg ?: "Menyimpan...") else "Simpan Data", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text(if (r.synced) "Berhasil!" else "Tersimpan Lokal", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (r.synced) Icons.Rounded.CheckCircle else Icons.Rounded.SaveAlt, null,
                        tint = if (r.synced) AppColors.Selesai else AppColors.Proses, modifier = Modifier.size(60.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(r.message, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Stand: ${r.stand}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter)
                    if (r.keptPhoto) {
                        Spacer(Modifier.height(8.dp))
                        Text("Foto bukti tetap disimpan sementara di perangkat agar bisa disinkronkan nanti.", fontSize = 12.sp, color = AppColors.Muted, textAlign = TextAlign.Center)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { result = null; onClose() }) { Text("Selesai", color = AppColors.Meter, fontWeight = FontWeight.Bold) } },
        )
    }
}

@Composable
private fun MeterCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", color = AppColors.Muted, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

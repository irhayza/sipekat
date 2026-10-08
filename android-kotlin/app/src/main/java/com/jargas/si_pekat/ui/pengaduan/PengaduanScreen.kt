package com.jargas.si_pekat.ui.pengaduan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.ContactLauncher
import com.jargas.si_pekat.core.sanitizeForSheet
import com.jargas.si_pekat.data.ActivityHistory
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.KunjunganApiService
import com.jargas.si_pekat.data.OcrApiService
import com.jargas.si_pekat.data.PengaduanApiService
import com.jargas.si_pekat.ui.components.AppTextField
import com.jargas.si_pekat.ui.components.SectionCard
import com.jargas.si_pekat.ui.components.SuccessSheet
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_CHARS = 200
private val PHONE_REGEX = Regex("^(08|\\+62|62)\\d{7,12}$")

/** Jalankan [block]; hasilnya null bila gagal. CancellationException tetap diteruskan agar debounce bisa membatalkan. */
private suspend inline fun <T> attempt(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

private class SuccessInfo(val ticket: String, val overdue: Boolean)

/**
 * Form Pengaduan. [userName] = email/ID akun (dipakai menebak wilayah WR/KD), [userNama] = nama petugas.
 * Port dari screens/pengaduan/pengaduan_screen.dart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaduanScreen(userName: String, userNama: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    var snackIsError by remember { mutableStateOf(false) }

    var idText by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var telp by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var pengaduan by remember { mutableStateOf("") }

    var idError by remember { mutableStateOf<String?>(null) }
    var namaError by remember { mutableStateOf<String?>(null) }
    var telpError by remember { mutableStateOf<String?>(null) }
    var alamatError by remember { mutableStateOf<String?>(null) }
    var pengaduanError by remember { mutableStateOf<String?>(null) }

    var idStatus by remember { mutableStateOf("") }
    var idStatusColor by remember { mutableStateOf(AppColors.Muted) }
    var mrsValue by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<SuccessInfo?>(null) }

    fun showSnack(message: String, isError: Boolean = false) {
        snackIsError = isError
        scope.launch {
            snackbarHost.currentSnackbarData?.dismiss()
            snackbarHost.showSnackbar(message)
        }
    }

    /** Tiket aktif (belum SELESAI) milik pelanggan ini, dari cache tiket Perbaikan. */
    fun activeTicketFor(digits: String) = AppSessionCache.perbaikanTickets.firstOrNull {
        it.idPelanggan.lowercase() == digits.lowercase() && it.status.uppercase() != "SELESAI"
    }

    // ── Autofill saat ID diketik: cek tiket ganda lalu cari di dbase → dapell → get_customer_by_id ──
    LaunchedEffect(idText) {
        val digits = idText.filter { it.isDigit() }
        if (digits.length < 8) {
            idStatus = ""
            return@LaunchedEffect
        }
        activeTicketFor(digits)?.let {
            showSnack("Pelanggan ini masih punya tiket ${it.ticket} berstatus ${it.status}", isError = true)
        }

        idStatus = "⏳ Mencari di dbase..."
        idStatusColor = AppColors.Flame
        delay(400) // debounce: dibatalkan otomatis bila ID berubah lagi

        val row = attempt { KunjunganApiService.findRow(sheetName = AppConfig.OCR_SHEET_NAME, idpel = digits).row }
        if (row != null && row.isNotEmpty()) {
            nama = row["NAMA"]?.toString() ?: ""
            alamat = row["ALAMAT"]?.toString() ?: ""
            mrsValue = row["MRS"]?.toString()?.trim()?.uppercase()
            idStatus = "✅ Ditemukan"
            idStatusColor = AppColors.Selesai
            return@LaunchedEffect
        }

        val dapel = attempt { OcrApiService.findCustomerInDapel(digits) }
        if (dapel != null) {
            nama = dapel.nama
            alamat = dapel.alamat ?: ""
            idStatus = "✅ Ditemukan (Dapel)"
            idStatusColor = AppColors.Selesai
            return@LaunchedEffect
        }

        val fallback = PengaduanApiService.checkCustomerId(digits)
        if (fallback != null) {
            nama = fallback.nama
            alamat = fallback.alamat
            idStatus = "✅ Ditemukan"
            idStatusColor = AppColors.Selesai
        } else {
            idStatus = "❌ Tidak ditemukan — isi manual di bawah"
            idStatusColor = AppColors.Danger
        }
    }

    fun resetForm() {
        idText = ""; nama = ""; telp = ""; alamat = ""; pengaduan = ""
        mrsValue = null
        idStatus = ""
        idError = null; namaError = null; telpError = null; alamatError = null; pengaduanError = null
    }

    /** Validasi isian (galat per kolom) + aturan bisnis (tiket aktif, format telepon). */
    fun validate(): Boolean {
        idError = if (idText.isBlank()) "ID pelanggan wajib diisi" else null
        namaError = if (nama.isBlank()) "Nama lengkap wajib diisi" else null
        telpError = if (telp.isBlank()) "Nomor telepon wajib diisi" else null
        alamatError = if (alamat.isBlank()) "Alamat wajib diisi" else null
        pengaduanError = if (pengaduan.isBlank()) "Isi pengaduan wajib diisi" else null
        if (listOf(idError, namaError, telpError, alamatError, pengaduanError).any { it != null }) return false

        // Aturan tiket aktif/24 jam diputuskan server (Code.gs): <24 jam ditolak dengan pesan, ≥24 jam menjadi pembaruan tiket.
        if (!PHONE_REGEX.matches(telp.replace(Regex("[\\s\\-().]"), ""))) {
            telpError = "Format tidak valid. Contoh: 08123456789"
            return false
        }
        return true
    }

    fun submit() {
        if (!validate()) return
        isLoading = true
        scope.launch {
            val mrs = mrsValue
            val wilayah = when {
                mrs == "KD" || mrs == "WR" -> mrs
                userName.lowercase().contains("wr") -> "WR"
                userName.lowercase().contains("kd") -> "KD"
                else -> "XX"
            }
            val response = PengaduanApiService.submitComplaint(
                mapOf(
                    "idPelanggan" to idText,
                    "nama" to sanitizeForSheet(nama),
                    "telpon" to telp,
                    "alamat" to sanitizeForSheet(alamat),
                    "pengaduan" to sanitizeForSheet(pengaduan),
                    "petugas" to userNama,
                    "wilayah" to wilayah,
                ),
            )
            isLoading = false
            if (response.ok) {
                val ticket = response.ticket ?: ""
                if (ticket.isNotEmpty()) {
                    ActivityHistory.add(
                        ActivityHistory.Kind.PENGADUAN, ticket, nama.trim(), idText, pengaduan.trim(),
                        if (response.isOverdueUpdate) "DIPERBARUI" else "OPEN",
                    )
                }
                success = SuccessInfo(ticket, response.isOverdueUpdate)
            } else {
                showSnack(response.message ?: "Laporan ditolak", isError = true)
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Canvas,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(data, containerColor = if (snackIsError) AppColors.Danger else AppColors.Selesai, contentColor = Color.White)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Form Pengaduan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Petugas: $userNama", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Pengaduan),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        ) {
            SectionCard(Icons.Rounded.PersonSearch, "Informasi Pelanggan", subtitle = "Masukkan ID Pelanggan untuk melacak data.") {
                FieldLabel("ID Pelanggan", statusText = idStatus, statusColor = idStatusColor)
                AppTextField(
                    value = idText, onValueChange = { idText = it; idError = null },
                    placeholder = "Contoh: 6120990001", keyboardType = KeyboardType.Number, error = idError,
                )
                Spacer(Modifier.height(14.dp))
                FieldLabel("Nama Lengkap", isRequired = true)
                AppTextField(nama, { nama = it; namaError = null }, placeholder = "Nama lengkap pelanggan", error = namaError)
                Spacer(Modifier.height(14.dp))
                FieldLabel("Nomor Telepon / WA", isRequired = true)
                AppTextField(
                    telp, { telp = it; telpError = null },
                    placeholder = "Contoh: 08123456789", keyboardType = KeyboardType.Phone, error = telpError,
                )
                if (ContactLauncher.hasValidPhone(telp)) {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = {
                            if (!ContactLauncher.callPhone(context, telp)) ContactLauncher.showLaunchFailure(context, "aplikasi telepon")
                        }) {
                            Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp), tint = AppColors.Brand)
                            Spacer(Modifier.size(6.dp))
                            Text("Telepon", color = AppColors.Brand)
                        }
                        TextButton(onClick = {
                            if (!ContactLauncher.openWhatsApp(context, telp)) ContactLauncher.showLaunchFailure(context, "WhatsApp")
                        }) {
                            Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(16.dp), tint = AppColors.WhatsApp)
                            Spacer(Modifier.size(6.dp))
                            Text("WhatsApp", color = AppColors.WhatsApp)
                        }
                    }
                }
            }

            SectionCard(Icons.Rounded.ChatBubbleOutline, "Detail Pengaduan", subtitle = "Jelaskan keluhan pelanggan dengan jelas.") {
                FieldLabel("Alamat Lokasi", isRequired = true)
                AppTextField(
                    alamat, { alamat = it; alamatError = null },
                    placeholder = "Alamat lengkap lokasi pengaduan", singleLine = false, minLines = 2, maxLines = 2, error = alamatError,
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    FieldLabel("Isi Pengaduan", isRequired = true)
                    Text(
                        "${pengaduan.length} / $MAX_CHARS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (pengaduan.length >= MAX_CHARS) AppColors.Danger else AppColors.Muted,
                    )
                }
                AppTextField(
                    pengaduan, { pengaduan = it.take(MAX_CHARS); pengaduanError = null },
                    placeholder = "Tulis kendala / masalah yang dilaporkan...", singleLine = false, minLines = 4, maxLines = 4, error = pengaduanError,
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { submit() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Pengaduan, contentColor = Color.White,
                    disabledContainerColor = AppColors.Pengaduan.copy(alpha = 0.6f), disabledContentColor = Color.White,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Text("Kirim Pengaduan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    success?.let { info ->
        SuccessSheet(
            ticket = info.ticket,
            ticketLabel = "NOMOR TIKET ANDA",
            title = if (info.overdue) "Peringatan Terkirim!" else "Pengaduan Terkirim!",
            message = if (info.overdue) "Pengaduan Overdue berhasil dilaporkan kembali kepada tim teknis."
            else "Data pengaduan pelanggan telah berhasil dicatat ke sistem.",
            finishLabel = "Dashboard",
            onFinish = { success = null; onBack() },
            continueLabel = "Buat Baru",
            onContinue = { success = null; resetForm() },
        )
    }
}

@Composable
private fun FieldLabel(text: String, isRequired: Boolean = false, statusText: String = "", statusColor: Color = AppColors.Muted) {
    Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        if (isRequired) Text(" *", color = AppColors.Danger, fontWeight = FontWeight.Bold)
        if (statusText.isNotEmpty()) {
            Text(statusText, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

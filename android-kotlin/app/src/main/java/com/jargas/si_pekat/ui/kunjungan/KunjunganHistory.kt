package com.jargas.si_pekat.ui.kunjungan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.jargas.si_pekat.core.ServerException
import com.jargas.si_pekat.data.KunjunganLocalCache
import com.jargas.si_pekat.data.KunjunganSync
import com.jargas.si_pekat.ui.components.EmptyState
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private typealias HistoryItem = MutableMap<String, Any?>

/**
 * Riwayat laporan Kunjungan/Pembukaan: terkirim vs pending (offline), hapus, kirim satu, dan "Sinkronkan Semua".
 * Dipakai di tab Riwayat dan di halaman Riwayat modul Kunjungan.
 */
@Composable
fun KunjunganHistory(modifier: Modifier = Modifier, onChanged: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var list by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var syncItems by remember { mutableStateOf<List<Map<String, Any?>>?>(null) }
    var deleteTarget by remember { mutableStateOf<HistoryItem?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun reload() {
        scope.launch {
            list = KunjunganLocalCache.getHistorySubmissions()
            loading = false
            onChanged()
        }
    }
    LaunchedEffect(Unit) { reload() }

    val pending = list.filter { it["status"] == "pending" }

    Column(modifier.fillMaxSize()) {
        if (pending.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().background(AppColors.ProsesBg).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CloudUpload, null, tint = AppColors.Proses)
                Spacer(Modifier.width(12.dp))
                Text("Ada ${pending.size} data laporan offline tertunda.", color = AppColors.Proses, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = { syncItems = pending.sortedBy { it["timestamp"]?.toString() ?: "" } },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Proses, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("Sinkronkan Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
            list.isEmpty() -> EmptyState(Icons.Rounded.HistoryToggleOff, "Belum ada riwayat kunjungan", "Laporan yang Anda kirim akan muncul di sini.")
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it["idpel"]?.toString() ?: it.hashCode().toString() }) { item ->
                    HistoryCard(
                        item,
                        onDelete = { deleteTarget = item },
                        onSend = { syncItems = listOf(item) },
                    )
                }
            }
        }
    }

    deleteTarget?.let { item ->
        val idpel = item["idpel"]?.toString() ?: ""
        val isPending = item["status"] == "pending"
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Hapus Riwayat?") },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus data laporan untuk pelanggan $idpel dari riwayat lokal?" +
                        if (isPending) "\n\nLaporan ini bertanda PENDING dan akan dihapus permanen dari antrean pengiriman offline." else "",
                )
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal", color = AppColors.Body) } },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        KunjunganLocalCache.removeHistorySubmission(idpel)
                        if (isPending) KunjunganLocalCache.removePendingSubmission(idpel)
                        android.widget.Toast.makeText(context, "Data riwayat berhasil dihapus.", android.widget.Toast.LENGTH_SHORT).show()
                        reload()
                    }
                }) { Text("Hapus", color = AppColors.Danger) }
            },
        )
    }

    syncItems?.let { items ->
        SyncProgressDialog(items = items, onFinished = { reload() }, onClose = { syncItems = null })
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onDelete: () -> Unit, onSend: () -> Unit) {
    val idpel = item["idpel"]?.toString() ?: ""
    val nama = item["nama"]?.toString() ?: "Pelanggan"
    val actionType = item["actionType"]?.toString() ?: ""
    val isPending = (item["status"]?.toString() ?: "sent") == "pending"
    val alamat = item["alamat"]?.toString()?.ifEmpty { null } ?: "Alamat tidak tersedia"
    val ts = item["timestamp"]?.toString() ?: ""
    val dateStr = if (ts.length >= 16) ts.substring(0, 16).replace('T', ' ') else ts
    val statusColor = if (isPending) AppColors.Proses else AppColors.Selesai

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("IDPEL: $idpel", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.Ink, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(if (isPending) AppColors.ProsesBg else AppColors.SelesaiBg)
                    .border(1.dp, statusColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (isPending) Icons.Rounded.OfflinePin else Icons.Rounded.CheckCircle, null, tint = statusColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isPending) "PENDING" else "TERKIRIM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(nama, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppColors.Ink)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.LocationOn, null, tint = AppColors.Muted, modifier = Modifier.padding(top = 1.dp).size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(alamat, fontSize = 12.sp, color = AppColors.Muted)
        }
        Spacer(Modifier.height(6.dp))
        Row { Text("Tindakan: ", fontSize = 12.sp, color = AppColors.Muted); Text(actionType.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Body) }
        Spacer(Modifier.height(2.dp))
        Row { Text("Waktu: ", fontSize = 12.sp, color = AppColors.Muted); Text(dateStr, fontSize = 12.sp, color = AppColors.Body) }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AppColors.Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Danger)) {
                Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Hapus", fontSize = 12.sp)
            }
            if (isPending) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSend, shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Kunjungan, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Send, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("Kirim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Dialog progres sinkronisasi: kirim satu per satu (2 percobaan), dengan log langsung. */
@Composable
fun SyncProgressDialog(items: List<Map<String, Any?>>, onFinished: () -> Unit, onClose: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var success by remember { mutableIntStateOf(0) }
    var fail by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Memulai sinkronisasi...") }
    var finished by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    val scroll = rememberScrollState()
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    fun log(msg: String) { logs.add("[${clock.format(System.currentTimeMillis())}] $msg") }

    LaunchedEffect(Unit) {
        log("Menemukan ${items.size} data laporan pending.")
        for ((i, item) in items.withIndex()) {
            val idpel = item["idpel"]?.toString() ?: ""
            val actionType = item["actionType"]?.toString() ?: ""
            index = i
            status = "Mengirim data $idpel ($actionType)..."
            log("Mengirim $idpel ($actionType)...")

            var attempts = 0
            var ok = false
            var err = ""
            while (attempts < 2 && !ok) {
                attempts++
                try {
                    val missing = KunjunganSync.sendOne(item)
                    if (missing.isNotEmpty()) log("PERINGATAN $idpel: kolom ${missing.joinToString(", ")} tidak ditemukan di sheet (isinya tidak tersimpan).")
                    ok = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    err = if (e is ServerException) (e.message ?: "") else e.toString()
                }
                if (!ok && attempts < 2) {
                    log("Gagal: $err. Percobaan mengirim kembali ($attempts/2)...")
                    delay(2000)
                }
            }
            if (ok) {
                log("IDPEL $idpel sukses terkirim.")
                KunjunganLocalCache.updateHistorySubmissionStatus(idpel, "sent")
                KunjunganLocalCache.removePendingSubmission(idpel)
                success++
            } else {
                log("IDPEL $idpel gagal dikirim setelah 2 percobaan. Error: $err")
                fail++
            }
        }
        finished = true
        status = "Sinkronisasi selesai!"
        log("Proses sinkronisasi selesai. Sukses: $success, Gagal: $fail.")
        onFinished()
    }
    LaunchedEffect(logs.size) { scroll.animateScrollTo(scroll.maxValue) }

    val total = items.size
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Sync, null, tint = AppColors.Kunjungan); Spacer(Modifier.width(10.dp)); Text("Sinkronisasi Data", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(status, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (finished) 1f else if (total > 0) (index + 1f) / total else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AppColors.Kunjungan, trackColor = AppColors.Line,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Proses: ${minOf(index + 1, total)}/$total", fontSize = 11.sp, color = AppColors.Muted)
                    Text("Sukses: $success | Gagal: $fail", fontSize = 11.sp, color = AppColors.Muted)
                }
                Spacer(Modifier.height(16.dp))
                Text("Log Pengiriman:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Column(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0F172A))
                        .verticalScroll(scroll).padding(8.dp),
                ) {
                    logs.forEach { Text(it, color = Color(0xFF4ADE80), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp)) }
                }
            }
        },
        confirmButton = {
            if (finished) Button(
                onClick = onClose, shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Kunjungan, contentColor = Color.White),
            ) { Text("Tutup") }
        },
    )
}

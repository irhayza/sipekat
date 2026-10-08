package com.jargas.si_pekat.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jargas.si_pekat.data.OcrLocalDb
import com.jargas.si_pekat.model.Customer
import com.jargas.si_pekat.model.MeterReading
import com.jargas.si_pekat.ui.components.EmptyState
import com.jargas.si_pekat.ui.components.ModuleTopBar
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Hapus Riwayat") },
        text = { Text("Apakah Anda yakin ingin menghapus data bacaan ini dari riwayat lokal?") },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = AppColors.Body) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Hapus", color = AppColors.Danger) } },
    )
}

/** Semua bacaan meter yang tersimpan di perangkat (terkirim & offline), dengan hapus dan tarik-untuk-segarkan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingsList(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var list by remember { mutableStateOf<List<MeterReading>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<MeterReading?>(null) }

    fun load(isRefresh: Boolean = false) {
        scope.launch {
            if (isRefresh) refreshing = true
            try {
                list = OcrLocalDb.getAllReadings()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            }
            loading = false
            refreshing = false
        }
    }
    LaunchedEffect(Unit) { load() }

    Box(modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Gagal memuat riwayat: $error") }
            list.isEmpty() -> EmptyState(Icons.Rounded.History, "Belum ada riwayat pembacaan", "Bacaan stand meter yang Anda catat akan muncul di sini.")
            else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = { load(true) }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.id }) { r -> ReadingCard(r, onDelete = { deleteTarget = r }) }
                }
            }
        }
    }

    deleteTarget?.let { r ->
        ConfirmDeleteDialog(onDismiss = { deleteTarget = null }) {
            deleteTarget = null
            scope.launch { OcrLocalDb.deleteReading(r.id); load() }
        }
    }
}

@Composable
private fun ReadingCard(r: MeterReading, onDelete: () -> Unit) {
    val color = if (r.isSynced) AppColors.Selesai else AppColors.Proses
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Surface), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(if (r.isSynced) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("IDPEL: ${r.pelangganId}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text("Tanggal: ${MeterReading.formatShort(r.createdAt)}", fontSize = 11.sp, color = AppColors.Muted)
                if (!r.catatan.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Catatan: ${r.catatan}", fontSize = 11.sp, color = AppColors.Muted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${r.standAngka}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter)
                Spacer(Modifier.height(4.dp))
                Text(if (r.isSynced) "Tersinkron" else "Offline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, "Hapus", tint = AppColors.Danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Riwayat bacaan satu pelanggan (dengan selisih pemakaian antarbulan). */
@Composable
fun CustomerHistoryScreen(customer: Customer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var list by remember { mutableStateOf<List<MeterReading>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<MeterReading?>(null) }

    fun load() {
        scope.launch { loading = true; list = OcrLocalDb.getByPelanggan(customer.id); loading = false }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        containerColor = AppColors.Canvas,
        topBar = {
            ModuleTopBar("Riwayat - ${customer.nama}", onBack = onBack, containerColor = AppColors.Meter) {
                IconButton(onClick = { load() }) { Icon(Icons.Rounded.Refresh, "Muat ulang") }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
                list.isEmpty() -> EmptyState(Icons.Rounded.History, "Belum ada riwayat bacaan", "Bacaan untuk pelanggan ini akan muncul di sini.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list.size, key = { list[it].id }) { i ->
                        val r = list[i]
                        val prev = list.getOrNull(i + 1)
                        val pemakaian = prev?.let { r.standAngka - it.standAngka }
                        Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Surface), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!r.fotoUrl.isNullOrEmpty()) {
                                    AsyncImage(r.fotoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)))
                                } else {
                                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.Line), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.ImageNotSupported, null, tint = AppColors.Muted)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(r.bulan, fontWeight = FontWeight.Bold, color = AppColors.Muted)
                                        if (!r.isSynced) {
                                            Text(
                                                "Offline", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Proses).padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text("Stand: ${r.standAngka}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                                    if (pemakaian != null) Text("Pemakaian: $pemakaian m3", fontSize = 13.sp, color = if (pemakaian < 0) AppColors.Danger else AppColors.Selesai)
                                    if (!r.catatan.isNullOrEmpty()) Text("Catatan: ${r.catatan}", fontSize = 12.sp, color = AppColors.Muted)
                                }
                                IconButton(onClick = { deleteTarget = r }) { Icon(Icons.Rounded.DeleteOutline, "Hapus", tint = AppColors.Danger) }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { r ->
        ConfirmDeleteDialog(onDismiss = { deleteTarget = null }) {
            deleteTarget = null
            scope.launch { OcrLocalDb.deleteReading(r.id); load() }
        }
    }
}

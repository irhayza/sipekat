package com.jargas.si_pekat.ui.perbaikan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.PerbaikanTicketService
import com.jargas.si_pekat.model.DropdownOptions
import com.jargas.si_pekat.model.Ticket
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Modul Laporan Perbaikan: daftar tiket aktif → form Tindakan & Dokumentasi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerbaikanScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Ticket?>(null) }
    var options by remember { mutableStateOf<DropdownOptions?>(null) }
    var opening by remember { mutableStateOf(false) }

    var allTickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    fun load(isRefresh: Boolean = false) {
        scope.launch {
            if (isRefresh) refreshing = true else loading = true
            error = null
            try {
                val tickets = PerbaikanTicketService.getActiveTickets()
                allTickets = tickets.filter { t ->
                    val k = t.kendala.lowercase()
                    !k.contains("buka segel") && !k.contains("pasang kembali")
                }
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

    val filtered = remember(allTickets, query) {
        val q = query.lowercase()
        allTickets.filter {
            it.ticket.lowercase().contains(q) || it.nama.lowercase().contains(q) ||
                it.idPelanggan.lowercase().contains(q) || it.kendala.lowercase().contains(q)
        }
    }

    // Layar tindakan menimpa daftar selama tiket terpilih.
    val current = selected
    if (current != null) {
        TindakanScreen(
            ticket = current,
            options = options ?: AppSessionCache.perbaikanOptions,
            onClose = { selected = null; load(isRefresh = true) },
        )
        return
    }

    Scaffold(
        containerColor = AppColors.Canvas,
        topBar = {
            ModuleTopBar("Laporan Perbaikan", "Tiket Aktif", onBack = onBack) {
                IconButton(onClick = { load(isRefresh = true) }, enabled = !loading) { Icon(Icons.Rounded.Refresh, "Muat ulang") }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Brand, trackColor = AppColors.BrandLight)
            SearchField(query, { query = it }, "Cari tiket, nama, IDPEL, atau pengaduan...", Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp))

            if (!loading && error == null) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Assignment, null, tint = AppColors.Muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${filtered.size} tiket aktif", fontSize = 12.sp, color = AppColors.Muted)
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
                error != null -> EmptyState(Icons.Rounded.CloudOff, "Gagal memuat tiket", error!!, actionLabel = "Coba Lagi", onAction = { load() })
                filtered.isEmpty() -> EmptyState(
                    Icons.Rounded.CheckCircle,
                    if (allTickets.isEmpty()) "Belum Ada Tiket Aktif" else "Tidak Ada Hasil",
                    if (allTickets.isEmpty()) "Semua pengaduan sudah tertangani atau belum ada data." else "Tidak ada tiket yang cocok dengan pencarian Anda.",
                )
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = { load(isRefresh = true) }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered, key = { it.ticket }) { t ->
                            TicketListTile(t) {
                                // Muat opsi dropdown dulu bila belum ada di cache.
                                scope.launch {
                                    var opts = AppSessionCache.perbaikanOptions
                                    if (opts == null) {
                                        opening = true
                                        try {
                                            AppSessionCache.refreshPerbaikan()
                                            opts = AppSessionCache.perbaikanOptions
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (_: Exception) {
                                        }
                                        opening = false
                                    }
                                    options = opts
                                    selected = t
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (opening) BlockingProgressDialog()
}

/** Kartu tiket untuk daftar Laporan Perbaikan. */
@Composable
fun TicketListTile(ticket: Ticket, onClick: () -> Unit) {
    val statusColor = when (ticket.status.uppercase()) {
        "OPEN" -> AppColors.Open
        "PROSES" -> AppColors.Proses
        "SELESAI" -> AppColors.Selesai
        else -> AppColors.Muted
    }
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(AppColors.Surface).border(1.dp, AppColors.Line, shape).clickable(onClick = onClick)) {
        // Header tiket
        Row(
            Modifier.fillMaxWidth().background(AppColors.Ink.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.Ink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Assignment, null, tint = AppColors.Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ticket.ticket, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Ink, letterSpacing = 0.3.sp)
                if (ticket.idPelanggan.isNotEmpty()) Text("IDPEL: ${ticket.idPelanggan}", fontSize = 11.5.sp, color = AppColors.Muted)
            }
            Text(
                ticket.status, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = statusColor, letterSpacing = 0.3.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        HorizontalDivider(color = AppColors.Line)

        Column(Modifier.padding(16.dp)) {
            Text(ticket.nama.ifEmpty { "Nama tidak tersedia" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            Spacer(Modifier.height(8.dp))
            if (ticket.alamat.isNotEmpty()) InfoRow(Icons.Rounded.LocationOn, ticket.alamat, AppColors.Muted, AppColors.Body)
            if (ticket.kendala.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                InfoRow(Icons.Rounded.ReportProblem, ticket.kendala, AppColors.Proses, AppColors.Proses, maxLines = 2)
            }
            if (ticket.telepon.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                InfoRow(Icons.Rounded.Phone, ticket.telepon, AppColors.Ink, AppColors.Ink, bold = true)
            }
            if (com.jargas.si_pekat.core.ContactLauncher.hasValidPhone(ticket.telepon) || (ticket.lat != null && ticket.lng != null)) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = AppColors.Line)
                Spacer(Modifier.height(12.dp))
                ContactActionRow(
                    telepon = ticket.telepon, nama = ticket.nama, lat = ticket.lat, lng = ticket.lng,
                    waMessage = "Halo ${ticket.nama}, kami dari petugas Jargas ingin menindaklanjuti laporan Anda (No. Tiket: ${ticket.ticket}).",
                    showShareLocation = false,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconColor: Color, textColor: Color, maxLines: Int = 1, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.padding(top = 1.dp).size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text, fontSize = 12.5.sp, color = textColor, lineHeight = 17.sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

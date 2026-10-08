package com.jargas.si_pekat.ui.kunjungan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.core.ApiException
import com.jargas.si_pekat.core.ContactLauncher
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.KunjunganLocalCache
import com.jargas.si_pekat.model.VisitCustomer
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Sub { Home, Visit, Reopen, History }

/** Modul Laporan Kunjungan: beranda modul → daftar Kunjungan / Pembukaan / Riwayat. */
@Composable
fun KunjunganScreen(onBack: () -> Unit) {
    var sub by rememberSaveable { mutableStateOf(Sub.Home) }
    BackHandler(enabled = sub != Sub.Home) { sub = Sub.Home }
    when (sub) {
        Sub.Home -> KunjunganHome(onBack, onVisit = { sub = Sub.Visit }, onReopen = { sub = Sub.Reopen }, onHistory = { sub = Sub.History })
        Sub.Visit -> CustomerListScreen(isReopen = false, onBack = { sub = Sub.Home })
        Sub.Reopen -> CustomerListScreen(isReopen = true, onBack = { sub = Sub.Home })
        Sub.History -> Scaffold(
            containerColor = AppColors.Canvas,
            topBar = { ModuleTopBar("Riwayat Laporan", "Kunjungan & Pembukaan", onBack = { sub = Sub.Home }) },
        ) { pad -> KunjunganHistory(Modifier.padding(pad)) }
    }
}

@Composable
private fun KunjunganHome(onBack: () -> Unit, onVisit: () -> Unit, onReopen: () -> Unit, onHistory: () -> Unit) {
    val userName = AppSessionCache.officerNama.ifEmpty { "Petugas" }
    val userRole = AppSessionCache.officerRole.ifEmpty { "petugas" }
    var pendingCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { pendingCount = KunjunganLocalCache.getPendingSubmissions().size }

    Scaffold(
        containerColor = AppColors.Canvas,
        topBar = { ModuleTopBar("Laporan Kunjungan", onBack = onBack) },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            // Header profil
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)).background(AppColors.Kunjungan)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(userName.first().uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Kunjungan)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        userRole.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.24f)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Column(Modifier.padding(20.dp)) {
                // Banner laporan offline tertunda (tambahan: jalan pintas ke riwayat untuk menyinkronkan)
                if (pendingCount > 0) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppColors.ProsesBg).clickable(onClick = onHistory).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.CloudUpload, null, tint = AppColors.Proses)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("$pendingCount laporan offline belum terkirim", color = AppColors.Proses, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Ketuk untuk membuka riwayat lalu sinkronkan saat online.", color = AppColors.Proses, fontSize = 11.5.sp)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = AppColors.Proses)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("PILIHAN MENU UTAMA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Muted, letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))
                MenuCard("KUNJUNGAN", "Pencatatan Kunjungan (Cabut / Segel / Lunas / Rumah Tertutup)", Icons.Rounded.Gavel, AppColors.Kunjungan, Color(0xFF1E40AF), onVisit)
                Spacer(Modifier.height(16.dp))
                MenuCard("PEMBUKAAN ALIRAN", "Pencatatan Pembukaan Aliran (Buka Segel / Pasang Kembali)", Icons.Rounded.CheckCircle, AppColors.Selesai, Color(0xFF15803D), onReopen)
                Spacer(Modifier.height(16.dp))
                MenuCard("RIWAYAT LAPORAN", "Lihat laporan terkirim & kirim ulang data offline", Icons.Rounded.History, Color(0xFF475569), Color(0xFF1E293B), onHistory)
                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, AppColors.Line, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Info, null, tint = AppColors.Kunjungan, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Petunjuk Singkat", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Gunakan menu Kunjungan untuk mencatat pemutusan aliran, dan menu Pembukaan untuk memproses penyambungan kembali.",
                            color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(title: String, subtitle: String, icon: ImageVector, c1: Color, c2: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().shadow(10.dp, shape, ambientColor = c1.copy(alpha = 0.3f), spotColor = c1.copy(alpha = 0.3f)).clip(shape)
            .background(Brush.linearGradient(listOf(c1, c2))).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 17.sp)
        }
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.24f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daftar pelanggan (Kunjungan & Pembukaan)
// Urutan sumber: cache sesi → server → salinan offline terakhir di perangkat.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerListScreen(isReopen: Boolean, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifier = rememberNotifier()
    var customers by remember { mutableStateOf<List<VisitCustomer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var fromOffline by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<VisitCustomer?>(null) }

    val what = if (isReopen) "pembukaan" else "kunjungan"

    fun fetch(force: Boolean = false) {
        scope.launch {
            if (force && customers.isNotEmpty()) refreshing = true else loading = true
            errorMsg = null
            fromOffline = false
            val cache = AppSessionCache
            fun cached() = if (isReopen) cache.pembukaanList else cache.kunjunganList
            fun cacheError() = if (isReopen) cache.pembukaanError else cache.kunjunganError
            try {
                if (!force && cached().isNotEmpty() && cacheError() == null) {
                    val list = cached()
                    customers = list
                    launch { if (isReopen) KunjunganLocalCache.saveReopenList(list) else KunjunganLocalCache.saveVisitList(list) }
                } else {
                    if (isReopen) cache.refreshPembukaan() else cache.refreshKunjungan()
                    cacheError()?.let { throw ApiException(it) }
                    val list = cached()
                    if (isReopen) KunjunganLocalCache.saveReopenList(list) else KunjunganLocalCache.saveVisitList(list)
                    customers = list
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val offline = if (isReopen) KunjunganLocalCache.getCachedReopenList() else KunjunganLocalCache.getCachedVisitList()
                if (offline.isNotEmpty()) {
                    customers = offline
                    fromOffline = true
                    notifier.show("Menampilkan daftar $what dari memori lokal (Offline).")
                } else {
                    errorMsg = "Gagal memuat daftar $what: ${e.message ?: e}"
                }
            }
            loading = false
            refreshing = false
        }
    }
    LaunchedEffect(Unit) { fetch() }

    val filtered = remember(customers, query, isReopen) {
        val q = query.lowercase()
        customers.filter {
            it.idpel.lowercase().contains(q) || it.nama.lowercase().contains(q) || (!isReopen && it.alamat.lowercase().contains(q))
        }
    }

    selected?.let { c ->
        if (isReopen) ReopenDetailScreen(c, onClose = { selected = null; fetch(force = true) })
        else VisitDetailScreen(c, onClose = { selected = null; fetch(force = true) })
        return
    }

    Scaffold(
        containerColor = AppColors.Canvas,
        snackbarHost = { NotifierHost(notifier) },
        topBar = {
            ModuleTopBar(if (isReopen) "Daftar Pembukaan" else "Daftar Kunjungan", onBack = onBack) {
                IconButton(onClick = { fetch(force = true) }, enabled = !loading) { Icon(Icons.Rounded.Refresh, "Segarkan") }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (fromOffline) {
                Row(Modifier.fillMaxWidth().background(AppColors.ProsesBg).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudOff, null, tint = AppColors.Proses, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mode offline — data terakhir tersimpan", fontSize = 11.5.sp, color = AppColors.Proses)
                }
            }
            SearchField(query, { query = it }, if (isReopen) "Cari IDPEL atau Nama Pelanggan..." else "Cari IDPEL, Nama, atau Alamat...", Modifier.padding(12.dp))
            if (!loading && errorMsg == null) {
                Text("${filtered.size} pelanggan", fontSize = 12.sp, color = AppColors.Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
                errorMsg != null -> EmptyState(Icons.Rounded.CloudOff, "Gagal memuat data", errorMsg!!, actionLabel = "Coba Lagi", onAction = { fetch(force = true) })
                filtered.isEmpty() -> EmptyState(
                    Icons.Rounded.Inbox, "Tidak Ada Data",
                    if (isReopen) "Tidak ada data pembukaan segel/cabut untuk saat ini." else "Tidak ada data kunjungan penutupan untuk saat ini.",
                )
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = { fetch(force = true) }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.idpel + it.nomgrt + it.kendala }) { c ->
                            CustomerListTile(c, accent = if (isReopen) AppColors.Pengaduan else AppColors.Kunjungan, isReopen = isReopen) { selected = c }
                        }
                    }
                }
            }
        }
    }
}

/** Kartu ringkas pelanggan untuk daftar Kunjungan & Pembukaan. */
@Composable
fun CustomerListTile(customer: VisitCustomer, accent: Color, isReopen: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val hasPhone = ContactLauncher.hasValidPhone(customer.telepon)
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(AppColors.Surface).border(1.dp, AppColors.Line, shape).clickable(onClick = onClick).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(customer.idpel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.Muted, modifier = Modifier.weight(1f))
            if (!isReopen) {
                Text(
                    "Tunggakan: ${customer.bln} Bln", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.10f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(customer.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.Ink)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.LocationOn, null, tint = AppColors.Muted, modifier = Modifier.padding(top = 1.dp).size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(customer.alamat.trim().ifEmpty { "Alamat tidak tersedia (Cek penamaan kolom di Sheet)" }, color = AppColors.Muted, fontSize = 13.sp)
        }
        if (isReopen && customer.telepon.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Phone, null, tint = AppColors.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Telfon: ", fontWeight = FontWeight.Bold, color = AppColors.Muted, fontSize = 12.sp)
                Text(customer.telepon, color = AppColors.Muted, fontSize = 12.sp)
            }
        }
        if (customer.kendala.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isReopen) Icons.Rounded.Assignment else Icons.Rounded.ReportProblem, null, tint = AppColors.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                if (isReopen) Text("Pengaduan: ", fontWeight = FontWeight.Bold, color = AppColors.Muted, fontSize = 12.sp)
                Text(
                    customer.kendala, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                    color = if (isReopen) AppColors.Ink else AppColors.Muted, fontWeight = if (isReopen) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AppColors.Line)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("No Meter: ${customer.nomgrt}", fontSize = 12.sp, color = AppColors.Muted)
            if (!isReopen) Text(formatRupiah(customer.rupiah), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Danger)
        }
        if (hasPhone) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChip(Icons.Rounded.Call, "Telepon", AppColors.Ink, { if (!ContactLauncher.callPhone(context, customer.telepon)) ContactLauncher.showLaunchFailure(context, "aplikasi telepon") })
                QuickChip(Icons.Rounded.Chat, "WhatsApp", AppColors.WhatsApp, { if (!ContactLauncher.openWhatsApp(context, customer.telepon)) ContactLauncher.showLaunchFailure(context, "WhatsApp") })
            }
        }
    }
}

package com.jargas.si_pekat.ui.ocr

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.OcrApiService
import com.jargas.si_pekat.data.OcrLocalDb
import com.jargas.si_pekat.data.OcrSyncService
import com.jargas.si_pekat.model.Customer
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Beranda modul Pencatatan Meter: pilih sektor & IDPEL → buka layar pembacaan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrHomeScreen(userNama: String, userEmail: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notifier = rememberNotifier()
    val listState = rememberLazyListState()

    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedSektor by remember { mutableStateOf<String?>(null) }
    var selectedIdpel by remember { mutableStateOf<String?>(null) }
    var unsynced by remember { mutableIntStateOf(0) }
    var reading by remember { mutableStateOf<Customer?>(null) }
    var showAllHistory by remember { mutableStateOf(false) }
    var showCustomers by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }

    val sektorOptions = remember(customers) { customers.map { it.sektorLabel }.toSet().sorted() }
    val bySektor = remember(customers, selectedSektor) { if (selectedSektor == null) emptyList() else customers.filter { it.sektorLabel == selectedSektor } }
    val selectedCustomer = bySektor.firstOrNull { it.noPelanggan == selectedIdpel }

    suspend fun applyCustomers(list: List<Customer>) {
        val unsyncedIds = OcrLocalDb.getUnsynced().map { it.pelangganId }.toSet()
        val visible = list.filter { it.noPelanggan !in unsyncedIds }
        customers = visible
        if (selectedSektor !in visible.map { it.sektorLabel }.toSet()) selectedSektor = null
        if (visible.none { it.noPelanggan == selectedIdpel }) selectedIdpel = null
    }

    suspend fun loadUnsynced() { unsynced = OcrLocalDb.countUnsynced() }

    suspend fun loadAssigned(showSpinner: Boolean = true) {
        if (showSpinner) { loading = true; loadError = null }
        try {
            val list = OcrApiService.getAssignedCustomers(userNama)
            AppSessionCache.ocrCustomers = list
            applyCustomers(list)
            loadError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (showSpinner) loadError = e.message ?: e.toString()
        }
        loading = false
    }

    // Tampilkan cache sesi dulu (instan), lalu segarkan diam-diam; bila cache kosong ambil dari server.
    LaunchedEffect(Unit) {
        loadUnsynced()
        val cached = AppSessionCache.ocrCustomers
        if (cached.isNotEmpty()) {
            applyCustomers(cached)
            loading = false
            loadAssigned(showSpinner = false)
        } else {
            loadAssigned()
        }
    }
    // Sinkron otomatis saat jaringan kembali
    LaunchedEffect(Unit) {
        OcrSyncService.autoSync().collect { count ->
            if (count > 0) { loadUnsynced(); loadAssigned(showSpinner = false) }
        }
    }

    fun manualSync() {
        if (syncing) return
        scope.launch {
            syncing = true
            val count = OcrSyncService.syncAll()
            loadUnsynced()
            loadAssigned(showSpinner = false)
            syncing = false
            notifier.show(if (count > 0) "$count data berhasil disinkronisasi ke OCRDAPEL" else "Tidak ada data yang berhasil disinkronisasi", isError = count == 0)
        }
    }

    // ── Sub-layar ──
    reading?.let { c ->
        MeterReadingScreen(c, userNama, userEmail, onClose = {
            reading = null
            scope.launch { loadUnsynced(); loadAssigned(showSpinner = false) }
        })
        return
    }
    if (showAllHistory) {
        BackHandler { showAllHistory = false }
        Scaffold(
            containerColor = AppColors.Canvas,
            topBar = { ModuleTopBar("Riwayat Pembacaan", "Pencatatan Meter", onBack = { showAllHistory = false }, containerColor = AppColors.Meter) },
        ) { pad -> ReadingsList(Modifier.padding(pad)) }
        return
    }

    Scaffold(containerColor = AppColors.Canvas, snackbarHost = { NotifierHost(notifier) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // ── Header ──
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)).background(AppColors.Meter)
                    .statusBarsPadding().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = Color.White) }
                    Text("Pencatatan Meter", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                    if (unsynced > 0) {
                        IconButton(onClick = { manualSync() }) {
                            BadgedBox(badge = { Badge { Text("$unsynced") } }) { Icon(Icons.Rounded.CloudUpload, "Sinkronisasi data offline", tint = Color.White) }
                        }
                    }
                    IconButton(onClick = { scope.launch { loadAssigned(); loadUnsynced() } }) { Icon(Icons.Rounded.Refresh, "Muat ulang", tint = Color.White) }
                }
                Row(Modifier.padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(userNama.firstOrNull()?.uppercase() ?: "P", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Halo, $userNama", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Pelanggan harus dicatat: ${customers.size} | Unsynced: $unsynced", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; loadAssigned(showSpinner = false); loadUnsynced(); refreshing = false } },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        MenuGrid(
                            listOf(
                                MenuItem(Icons.Rounded.QrCodeScanner, "Input Stand", AppColors.Meter) { scope.launch { listState.animateScrollToItem(1) } },
                                MenuItem(Icons.Rounded.People, "Pelanggan", AppColors.Meter) { showCustomers = true },
                                MenuItem(Icons.Rounded.CloudUpload, if (syncing) "Mengirim..." else "Sync Offline", AppColors.Selesai) { manualSync() },
                                MenuItem(Icons.Rounded.History, "Riwayat", AppColors.Proses) { showAllHistory = true },
                            ),
                        )
                    }
                    item {
                        when {
                            loading -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
                            loadError != null -> InfoCard(Icons.Rounded.CloudOff, loadError!!, null, actionLabel = "Coba Lagi") { scope.launch { loadAssigned() } }
                            customers.isEmpty() -> InfoCard(
                                Icons.Rounded.ListAlt, "Belum ada pelanggan untuk petugas ini",
                                "Periksa kolom Petugas pada sheet OCRDAPEL agar sesuai dengan nama petugas pada sheet LOGIN.",
                            )
                            else -> SelectorCard(
                                sektorOptions, selectedSektor,
                                onSektor = { selectedSektor = it; selectedIdpel = null },
                                customersBySektor = bySektor, selectedIdpel = selectedIdpel,
                                onIdpel = { id ->
                                    selectedIdpel = id
                                    bySektor.firstOrNull { it.noPelanggan == id }?.let { reading = it }
                                },
                            )
                        }
                    }
                    selectedCustomer?.let { c -> item { SelectedCustomerCard(c) { reading = c } } }
                }
            }
        }
    }

    if (showCustomers) {
        ModalBottomSheet(onDismissRequest = { showCustomers = false }, containerColor = Color.White) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daftar Pelanggan Penugasan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter)
                    IconButton(onClick = { showCustomers = false }) { Icon(Icons.Rounded.Close, "Tutup") }
                }
                HorizontalDivider(color = AppColors.Line)
                if (customers.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Tidak ada pelanggan penugasan") }
                } else {
                    LazyColumn(Modifier.navigationBarsPadding()) {
                        items(customers, key = { it.noPelanggan }) { c ->
                            Row(
                                Modifier.fillMaxWidth().clickable { showCustomers = false; selectedSektor = c.sektorLabel; selectedIdpel = c.noPelanggan }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(AppColors.Meter.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, null, tint = AppColors.Meter)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.nama, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("IDPEL: ${c.noPelanggan} | Sektor: ${c.sektorLabel}", fontSize = 12.sp, color = AppColors.Muted)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = AppColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

private class MenuItem(val icon: ImageVector, val label: String, val color: Color, val onClick: () -> Unit)

@Composable
private fun MenuGrid(items: List<MenuItem>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { item ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = item.onClick).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(item.color.copy(alpha = 0.10f)).border(1.5.dp, item.color.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(item.icon, null, tint = item.color, modifier = Modifier.size(23.dp)) }
                Spacer(Modifier.height(8.dp))
                Text(item.label, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = AppColors.Ink, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
private fun SelectorCard(
    sektorOptions: List<String>, selectedSektor: String?, onSektor: (String) -> Unit,
    customersBySektor: List<Customer>, selectedIdpel: String?, onIdpel: (String) -> Unit,
) {
    val labels = customersBySektor.associate { "${it.noPelanggan} - ${it.nama}" to it.noPelanggan }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Pilih Sektor & Pelanggan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter)
            Spacer(Modifier.height(6.dp))
            Text("Pilih sektor, lalu pilih IDPEL untuk membuka scanner stand meter.", color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(14.dp))
            AppDropdown("Sektor", selectedSektor, sektorOptions, onSektor)
            Spacer(Modifier.height(12.dp))
            AppDropdown(
                "IDPEL", labels.entries.firstOrNull { it.value == selectedIdpel }?.key, labels.keys.toList(),
                onSelected = { onIdpel(labels.getValue(it)) }, enabled = customersBySektor.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun SelectedCustomerCard(c: Customer, onOpen: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(c.nama, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Meter)
            Spacer(Modifier.height(8.dp))
            Text("IDPEL: ${c.noPelanggan}", fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("No. Meter: ${c.noMeter?.takeIf { it.isNotEmpty() } ?: "-"}", fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("Alamat: ${c.alamat?.takeIf { it.isNotEmpty() } ?: "-"}", fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("St Lalu: ${c.standBulanLalu ?: c.standAwal}", fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onOpen, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Meter, contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.QrCodeScanner, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Buka Scan Meter", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, description: String?, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = AppColors.Muted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, textAlign = TextAlign.Center, fontSize = if (description != null) 14.sp else 12.sp, fontWeight = if (description != null) FontWeight.Bold else FontWeight.Normal, color = if (description != null) AppColors.Ink else AppColors.Muted)
            if (description != null) { Spacer(Modifier.height(8.dp)); Text(description, textAlign = TextAlign.Center, color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp) }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onAction) { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(actionLabel, fontSize = 12.sp) }
            }
        }
    }
}

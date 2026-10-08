package com.jargas.si_pekat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.core.ContactLauncher
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Snackbar berwarna (sukses hijau / galat merah) ────────────────────────────

class Notifier(val host: SnackbarHostState, private val scope: CoroutineScope) {
    var isError by mutableStateOf(false)
        private set

    fun show(message: String, isError: Boolean = false) {
        this.isError = isError
        scope.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(message)
        }
    }
}

@Composable
fun rememberNotifier(): Notifier {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember { Notifier(host, scope) }
}

@Composable
fun NotifierHost(n: Notifier) {
    SnackbarHost(n.host) { data ->
        Snackbar(data, containerColor = if (n.isError) AppColors.Danger else AppColors.Selesai, contentColor = Color.White)
    }
}

// ── Top bar modul ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    containerColor: Color = AppColors.BrandDark,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, enabled = backEnabled) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", tint = Color.White.copy(alpha = if (backEnabled) 1f else 0.4f))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor, actionIconContentColor = Color.White),
    )
}

// ── Keadaan kosong / galat ────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(AppColors.BrandLight), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppColors.Brand, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = AppColors.Muted, lineHeight = 18.sp, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.White)) { Text(actionLabel) }
        }
    }
}

// ── Banner GPS ────────────────────────────────────────────────────────────────

enum class GpsBannerState { Loading, Ok, Warning }

@Composable
fun GpsBanner(state: GpsBannerState, message: String, modifier: Modifier = Modifier, onTap: (() -> Unit)? = null) {
    val (bg, fg) = when (state) {
        GpsBannerState.Loading -> Color(0xFFEFF6FF) to Color(0xFF1E40AF)
        GpsBannerState.Ok -> AppColors.SelesaiBg to AppColors.Selesai
        GpsBannerState.Warning -> AppColors.ProsesBg to AppColors.Proses
    }
    Row(
        modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            GpsBannerState.Loading -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = fg)
            GpsBannerState.Ok -> Icon(Icons.Rounded.LocationOn, null, tint = fg, modifier = Modifier.size(16.dp))
            GpsBannerState.Warning -> Icon(Icons.Rounded.LocationOff, null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(message, color = fg, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onTap != null) Icon(Icons.Rounded.ChevronRight, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

// ── Chip aksi cepat (Telepon / WhatsApp / Lokasi) ─────────────────────────────

@Composable
fun QuickChip(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.10f)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** Baris chip Telepon + WhatsApp (+ Lihat/Bagikan Lokasi bila ada koordinat). */
@Composable
fun ContactActionRow(
    telepon: String?,
    nama: String?,
    lat: Double?,
    lng: Double?,
    modifier: Modifier = Modifier,
    waMessage: String? = null,
    showShareLocation: Boolean = true,
) {
    val context = LocalContext.current
    val hasPhone = ContactLauncher.hasValidPhone(telepon)
    val hasLocation = lat != null && lng != null
    if (!hasPhone && !hasLocation) return
    FlowRowCompat(modifier, spacing = 8.dp) {
        if (hasPhone) {
            QuickChip(Icons.Rounded.Call, "Telepon", AppColors.Brand, {
                if (!ContactLauncher.callPhone(context, telepon)) ContactLauncher.showLaunchFailure(context, "aplikasi telepon")
            })
            QuickChip(Icons.Rounded.Chat, "WhatsApp", AppColors.WhatsApp, {
                val msg = waMessage ?: nama?.let { "Halo $it, kami dari petugas Jargas ingin menindaklanjuti laporan Anda." }
                if (!ContactLauncher.openWhatsApp(context, telepon, msg)) ContactLauncher.showLaunchFailure(context, "WhatsApp")
            })
        }
        if (hasLocation) {
            QuickChip(Icons.Rounded.Map, "Lihat Lokasi", AppColors.Proses, {
                if (!ContactLauncher.openMap(context, lat!!, lng!!)) ContactLauncher.showLaunchFailure(context, "aplikasi peta")
            })
            if (showShareLocation) {
                QuickChip(Icons.Rounded.ShareLocation, "Bagikan Lokasi", AppColors.Pengaduan, {
                    if (!ContactLauncher.shareLocationViaWhatsApp(context, lat!!, lng!!, telepon, nama)) ContactLauncher.showLaunchFailure(context, "WhatsApp")
                })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowCompat(modifier: Modifier = Modifier, spacing: androidx.compose.ui.unit.Dp = 8.dp, content: @Composable () -> Unit) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) { content() }
}

// ── Kartu info pelanggan terpadu ──────────────────────────────────────────────

@Composable
fun CustomerInfoCard(
    modifier: Modifier = Modifier,
    idPelanggan: String? = null,
    nama: String? = null,
    alamat: String? = null,
    telepon: String? = null,
    kendala: String? = null,
    lat: Double? = null,
    lng: Double? = null,
    extraRows: List<Pair<String, String>> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
) {
    fun iconRow(icon: ImageVector, text: String): @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = AppColors.Muted, modifier = Modifier.size(15.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, color = AppColors.Body, lineHeight = 17.5.sp)
        }
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (!nama.isNullOrBlank()) Text(nama, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                if (!idPelanggan.isNullOrBlank()) Text("ID Pelanggan: $idPelanggan", fontSize = 12.sp, color = AppColors.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            trailing?.invoke()
        }
        if (!alamat.isNullOrBlank()) { Spacer(Modifier.height(10.dp)); iconRow(Icons.Rounded.LocationOn, alamat)() }
        if (!telepon.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); iconRow(Icons.Rounded.Phone, telepon)() }
        if (!kendala.isNullOrBlank()) { Spacer(Modifier.height(8.dp)); iconRow(Icons.Rounded.ReportProblem, kendala)() }
        for ((k, v) in extraRows) {
            Spacer(Modifier.height(8.dp))
            Row {
                Text(k, fontSize = 12.5.sp, color = AppColors.Muted, modifier = Modifier.width(96.dp))
                Text(v, fontSize = 13.sp, color = AppColors.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
        if (ContactLauncher.hasValidPhone(telepon) || (lat != null && lng != null)) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = AppColors.Line)
            Spacer(Modifier.height(12.dp))
            ContactActionRow(telepon, nama, lat, lng)
        }
    }
}

// ── Dropdown (ExposedDropdownMenu) ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    placeholder: String = "Pilih...",
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = AppColors.Muted) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = AppColors.Canvas,
                focusedBorderColor = AppColors.Brand, unfocusedBorderColor = AppColors.Line, errorBorderColor = AppColors.Danger,
                focusedLabelColor = AppColors.Brand, focusedTextColor = AppColors.Ink, unfocusedTextColor = AppColors.Ink,
                disabledTextColor = AppColors.Body, errorTextColor = AppColors.Ink, errorContainerColor = Color.White,
            ),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelected(opt); expanded = false })
            }
        }
    }
}

// ── Kolom cari ────────────────────────────────────────────────────────────────

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AppColors.Muted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = AppColors.Brand) },
        trailingIcon = if (value.isNotEmpty()) ({ IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Clear, "Hapus") } }) else null,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            focusedBorderColor = AppColors.Brand, unfocusedBorderColor = AppColors.Line, cursorColor = AppColors.Brand,
        ),
    )
}

// ── Overlay proses simpan ─────────────────────────────────────────────────────

@Composable
fun SavingOverlay(step: String?, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(230.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.Surface).padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = AppColors.Brand)
            Spacer(Modifier.height(16.dp))
            Text(step ?: "Menyimpan...", textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        }
    }
}

/** Dialog bulat berputar yang tidak bisa ditutup (mis. saat memuat opsi dropdown). */
@Composable
fun BlockingProgressDialog() {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) { CircularProgressIndicator(color = Color.White) }
}

/** Kartu ringkasan berlatar gradien biru (ringkasan tiket/pelanggan di atas form). */
@Composable
fun SummaryHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(AppColors.BlueAccent, Color(0xFF003A70))))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun HeroChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
    }
}

fun formatRupiah(v: Double): String {
    val n = v.toLong().toString()
    val sb = StringBuilder()
    n.reversed().forEachIndexed { i, c -> if (i > 0 && i % 3 == 0) sb.append('.'); sb.append(c) }
    return "Rp " + sb.reverse().toString()
}

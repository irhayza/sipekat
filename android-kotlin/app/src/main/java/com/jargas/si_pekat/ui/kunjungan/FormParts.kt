package com.jargas.si_pekat.ui.kunjungan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.ui.components.*
import com.jargas.si_pekat.ui.theme.AppColors
import java.util.Locale

fun coord(v: Double?): String = v?.let { String.format(Locale.US, "%.6f", it) } ?: ""

@Composable
fun FormSectionHeader(title: String) {
    Text(
        title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.Muted, letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp)).padding(16.dp),
        content = content,
    )
}

/** Peringatan GPS mati / izin belum diberikan, bisa diketuk untuk memperbaiki. */
@Composable
fun GpsWarnings(gps: VisitGpsState, onRetry: () -> Unit) {
    if (!gps.serviceEnabled) {
        GpsBanner(GpsBannerState.Warning, "Peringatan: GPS tidak aktif! Nyalakan lokasi perangkat Anda.")
    }
    val w = gps.warning
    if (gps.serviceEnabled && w != null) {
        GpsBanner(GpsBannerState.Warning, w, onTap = { if (w.contains("permanen")) gps.openSettings() else onRetry() })
    }
}

/** Pilihan tindakan (radio) dalam satu kartu. */
@Composable
fun ActionRadioCard(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp)).padding(vertical = 8.dp),
    ) {
        options.forEach { opt ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(opt) }.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == opt, onClick = { onSelect(opt) },
                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.Kunjungan, unselectedColor = AppColors.Muted),
                )
                Text(opt, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppColors.Ink)
            }
        }
    }
}

@Composable
fun MgrtBaruCard(
    noMgrt: String, onNoMgrt: (String) -> Unit, noMgrtError: String?,
    stMgrt: String, onStMgrt: (String) -> Unit, stMgrtError: String?,
) {
    FormCard {
        AppTextField(noMgrt, onNoMgrt, label = "No MGRT Baru *", placeholder = "Contoh: 87654321", leadingIcon = Icons.Rounded.Pin, error = noMgrtError)
        Spacer(Modifier.height(16.dp))
        AppTextField(
            stMgrt, { onStMgrt(it.filter(Char::isDigit)) }, label = "Stand Meter Baru *", placeholder = "Masukkan angka stand meter",
            leadingIcon = Icons.Rounded.Speed, keyboardType = KeyboardType.Number, error = stMgrtError,
        )
    }
}

@Composable
fun SubmitButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Kunjungan, contentColor = Color.White),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Dialog hasil: tersimpan di server atau tersimpan lokal (offline). */
@Composable
fun SubmissionResultDialog(title: String, idpel: String, action: String, offline: Boolean, warning: String? = null, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { Text(if (offline) "Tersimpan Secara Lokal!" else title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (offline) Icons.Rounded.OfflinePin else Icons.Rounded.CheckCircle, null,
                    tint = if (offline) AppColors.Proses else AppColors.Selesai, modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (offline) "Status pelanggan $idpel disimpan lokal karena offline. Jangan lupa sinkronisasi data saat online (menu Riwayat Laporan)."
                    else "Status pelanggan $idpel berhasil diubah menjadi $action ke server.",
                    textAlign = TextAlign.Center,
                )
                if (warning != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(warning, color = AppColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = onOk, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Kunjungan, contentColor = Color.White)) { Text("OK") }
            }
        },
    )
}

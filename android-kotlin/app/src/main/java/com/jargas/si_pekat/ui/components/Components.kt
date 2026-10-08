package com.jargas.si_pekat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.ui.theme.AppColors

/** Kartu seksi form: header (ikon + judul kapital + subjudul) di atas kartu putih berbingkai. */
@Composable
fun SectionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.padding(bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.BrandLight).padding(8.dp),
            ) { Icon(icon, null, tint = AppColors.Brand, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = AppColors.Ink)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 11.5.sp, color = AppColors.Muted)
                }
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = AppColors.Ink.copy(alpha = 0.03f))
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) { content() }
    }
}

/** Lencana status (SELESAI / PROSES / OPEN) berwarna. */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier, fontSize: Int = 11) {
    var bg = AppColors.BrandLight
    var fg = AppColors.Muted
    var label = status.ifEmpty { "—" }
    when (status.uppercase()) {
        "SELESAI" -> { bg = AppColors.SelesaiBg; fg = AppColors.Selesai; label = "SELESAI" }
        "PROSES" -> { bg = AppColors.ProsesBg; fg = AppColors.Proses; label = "PROSES" }
        "OPEN" -> { bg = AppColors.OpenBg; fg = AppColors.Open; label = "OPEN" }
    }
    Row(
        modifier.clip(RoundedCornerShape(99.dp)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(end = 6.dp).size(6.dp).clip(CircleShape).background(fg))
        Text(label, color = fg, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

/**
 * Lembar konfirmasi sukses terpadu untuk semua modul. Tidak bisa ditutup dengan geser/ketuk di luar —
 * pengguna harus memilih tombol. [status] kosong → badge status disembunyikan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessSheet(
    ticket: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    status: String = "",
    title: String = "Laporan tersimpan",
    message: String = "Data penanganan sudah dicatat di server.",
    ticketLabel: String = "TIKET",
    finishLabel: String = "Selesai",
    onContinue: (() -> Unit)? = null,
    continueLabel: String = "Lanjut Update",
) {
    ModalBottomSheet(
        onDismissRequest = {}, // sengaja kosong: tidak boleh ditutup tanpa memilih tombol
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden }),
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(AppColors.SelesaiBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = AppColors.Selesai, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = AppColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.Canvas)
                    .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(ticketLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.Muted, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(4.dp))
                Text(ticket, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = AppColors.Ink)
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(status, fontSize = 12)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, AppColors.Line)) {
                    Text(finishLabel, color = AppColors.Ink)
                }
                if (onContinue != null) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.White),
                    ) { Text(continueLabel) }
                }
            }
        }
    }
}

/** Kolom isian bergaya SiPEKAT: sudut 12 dp, bingkai abu, fokus merah, pesan galat di bawah. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    error: String? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
    onDone: (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    containerColor: Color = Color.White,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it) } },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = AppColors.Muted) }) else null,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, null, tint = AppColors.Brand) } },
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            errorContainerColor = containerColor,
            focusedBorderColor = AppColors.Brand,
            unfocusedBorderColor = AppColors.Line,
            errorBorderColor = AppColors.Danger,
            cursorColor = AppColors.Brand,
            focusedLabelColor = AppColors.Brand,
            focusedTextColor = AppColors.Ink,
            unfocusedTextColor = AppColors.Ink,
            errorTextColor = AppColors.Ink,
        ),
    )
}

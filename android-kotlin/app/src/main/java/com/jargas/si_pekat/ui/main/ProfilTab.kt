package com.jargas.si_pekat.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.data.AuthService
import com.jargas.si_pekat.ui.components.AppTextField
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilTab(userNama: String, userEmail: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogout by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val initial = userNama.firstOrNull()?.uppercase() ?: "P"

    Column(Modifier.fillMaxSize().background(AppColors.Canvas)) {
        TopAppBar(
            title = { Text("Profil Pengguna") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BrandDark, titleContentColor = Color.White),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(100.dp).clip(CircleShape).background(AppColors.Brand.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Text(initial, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = AppColors.Brand)
            }
            Spacer(Modifier.height(16.dp))
            Text(userNama, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            Text(userEmail, fontSize = 14.sp, color = AppColors.Muted)
            Spacer(Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column {
                    MenuRow(Icons.Outlined.Lock, "Ganti Password", AppColors.Brand, chevron = true) { showPassword = true }
                    HorizontalDivider(color = AppColors.Line)
                    MenuRow(Icons.Outlined.Settings, "Pengaturan Aplikasi", AppColors.Brand, chevron = true) {}
                    HorizontalDivider(color = AppColors.Line)
                    MenuRow(Icons.Outlined.HelpOutline, "Pusat Bantuan", AppColors.Brand, chevron = true) {}
                    HorizontalDivider(color = AppColors.Line)
                    MenuRow(Icons.AutoMirrored.Rounded.Logout, "Keluar", AppColors.Danger, bold = true, textColor = AppColors.Danger) { showLogout = true }
                }
            }
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = { Text("Keluar Aplikasi?") },
            text = { Text("Sesi aktif Anda akan dihapus. Yakin ingin keluar?") },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Batal", color = AppColors.Body) } },
            confirmButton = {
                Button(
                    onClick = { showLogout = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger, contentColor = Color.White),
                ) { Text("Keluar") }
            },
        )
    }

    if (showPassword) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }

        fun submit() {
            error = when {
                oldPw.isBlank() -> "Password lama wajib diisi."
                newPw.length < 6 -> "Password baru minimal 6 karakter."
                newPw == oldPw -> "Password baru harus berbeda dari password lama."
                newPw != confirmPw -> "Konfirmasi password tidak cocok."
                else -> null
            }
            if (error != null) return
            scope.launch {
                busy = true
                try {
                    AuthService.changePassword(userEmail, oldPw, newPw)
                    showPassword = false
                    Toast.makeText(context, "Password berhasil diubah.", Toast.LENGTH_LONG).show()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = e.message ?: "Gagal mengubah password."
                }
                busy = false
            }
        }

        AlertDialog(
            onDismissRequest = { if (!busy) showPassword = false },
            containerColor = Color.White,
            title = { Text("Ganti Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val hide = PasswordVisualTransformation()
                    AppTextField(oldPw, { oldPw = it; error = null }, label = "Password Lama", leadingIcon = Icons.Outlined.Lock, keyboardType = KeyboardType.Password, visualTransformation = hide)
                    AppTextField(newPw, { newPw = it; error = null }, label = "Password Baru (min. 6 karakter)", leadingIcon = Icons.Rounded.Lock, keyboardType = KeyboardType.Password, visualTransformation = hide)
                    AppTextField(confirmPw, { confirmPw = it; error = null }, label = "Konfirmasi Password Baru", leadingIcon = Icons.Rounded.LockReset, keyboardType = KeyboardType.Password, visualTransformation = hide)
                    error?.let { Text(it, color = AppColors.Danger, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
                }
            },
            dismissButton = { TextButton(onClick = { showPassword = false }, enabled = !busy) { Text("Batal", color = AppColors.Body) } },
            confirmButton = {
                Button(
                    onClick = { submit() }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.White),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text("Simpan")
                }
            },
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    chevron: Boolean = false,
    bold: Boolean = false,
    textColor: Color = AppColors.Ink,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconColor)
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), fontSize = 14.sp, color = textColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        if (chevron) Icon(Icons.Rounded.ChevronRight, null, tint = AppColors.Body)
    }
}

package com.jargas.si_pekat.ui.login

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.R
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.GpsResult
import com.jargas.si_pekat.core.LocationService
import com.jargas.si_pekat.core.SessionStore
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.data.AuthService
import com.jargas.si_pekat.ui.components.AppTextField
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoggedIn: (nama: String, email: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var passErr by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    // Alur login (setelah izin lokasi beres): GPS wajib aktif → autentikasi → muat semua data modul sekali.
    fun proceed() {
        scope.launch {
            loading = true
            errorMsg = null
            stage = "Memeriksa GPS perangkat..."
            try {
                val gps = LocationService.getCurrentLocation(context, timeLimitMs = 6_000)
                if (!gps.isOk) {
                    errorMsg = when (gps.result) {
                        GpsResult.SERVICE_DISABLED -> "GPS perangkat Anda mati. Mohon aktifkan GPS lalu coba lagi."
                        GpsResult.PERMISSION_DENIED, GpsResult.PERMISSION_DENIED_FOREVER ->
                            "Aplikasi membutuhkan izin lokasi/GPS. Mohon berikan izin lokasi lalu coba lagi."
                        else -> "GPS perangkat Anda harus aktif untuk masuk ke aplikasi."
                    }
                    loading = false
                    return@launch
                }

                stage = "Melakukan autentikasi..."
                val res = AuthService.login(email.trim(), pass)
                val nama = res.nama.ifEmpty { email.trim() }
                val mail = res.email.ifEmpty { email.trim() }
                SessionStore.save(mail, nama, res.token)

                // Muat daftar semua modul SEKALI di sini; layar modul lalu cukup membaca cache.
                stage = "Menyiapkan data awal..."
                AppSessionCache.preloadAll(nama = nama, email = mail, onProgress = { stage = it })
                onLoggedIn(nama, mail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMsg = e.message ?: "Login gagal. Coba lagi."
                loading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        proceed() // izin ditolak pun diteruskan: proceed() melaporkan pesan yang sesuai
    }

    fun onLoginClick() {
        emailErr = if (email.isBlank()) "User ID wajib diisi" else null
        passErr = if (pass.isEmpty()) "Password wajib diisi" else null
        if (emailErr != null || passErr != null) return
        if (LocationService.hasPermission(context)) {
            proceed()
        } else {
            loading = true
            errorMsg = null
            stage = "Memeriksa GPS perangkat..."
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Box(Modifier.fillMaxSize().background(AppColors.Brand)) {
        // Dekorasi lingkaran latar
        Box(Modifier.align(Alignment.TopEnd).offset(60.dp, (-80).dp).size(280.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        Box(Modifier.align(Alignment.BottomStart).offset((-80).dp, 100.dp).size(320.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.04f)))

        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            AnimatedVisibility(
                visible = started,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { (it * 0.12f).toInt() },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.White).padding(4.dp),
                    ) {
                        Image(
                            painterResource(R.drawable.app_logo), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("SiPEKAT", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sistem Pelaporan Kegiatan\ndan Tindakan Petugas Jargas",
                        fontSize = 13.sp, lineHeight = 19.5.sp, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                visible = started,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { (it * 0.12f).toInt() },
            ) {
                Column(
                    Modifier.fillMaxWidth().shadow(24.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(24.dp),
                ) {
                    Text("Masuk ke Akun Anda", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Gunakan User ID dan password yang diberikan oleh admin.", fontSize = 12.5.sp, color = AppColors.Muted)
                    Spacer(Modifier.height(24.dp))

                    if (errorMsg != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(12.dp))
                                .background(AppColors.DangerBg).border(1.dp, AppColors.Danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = AppColors.Danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(errorMsg!!, color = AppColors.Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    AppTextField(
                        value = email, onValueChange = { email = it; emailErr = null },
                        label = "User ID", placeholder = "Contoh: sda01", error = emailErr,
                        leadingIcon = Icons.Outlined.Badge, containerColor = AppColors.Canvas,
                    )
                    Spacer(Modifier.height(16.dp))
                    AppTextField(
                        value = pass, onValueChange = { pass = it; passErr = null },
                        label = "Password", error = passErr,
                        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                        onDone = { if (!loading) onLoginClick() },
                        leadingIcon = Icons.Outlined.Lock, containerColor = AppColors.Canvas,
                        visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                        trailing = {
                            IconButton(onClick = { obscure = !obscure }) {
                                Icon(
                                    if (obscure) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null,
                                    tint = AppColors.Muted, modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = { onLoginClick() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brand, contentColor = Color.White,
                            disabledContainerColor = AppColors.Brand.copy(alpha = 0.6f), disabledContentColor = Color.White,
                        ),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        } else {
                            Text("MASUK", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        }
                    }
                    if (loading && stage != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(stage!!, fontSize = 12.sp, color = AppColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "${AppConfig.APP_VERSION} · SiPEKAT — Jargas Petugas App",
                color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

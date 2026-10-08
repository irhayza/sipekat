package com.jargas.si_pekat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jargas.si_pekat.core.SessionStore
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.ui.login.LoginScreen
import com.jargas.si_pekat.ui.main.MainNavScreen
import com.jargas.si_pekat.ui.ocr.OcrHomeScreen
import com.jargas.si_pekat.ui.kunjungan.KunjunganScreen
import com.jargas.si_pekat.ui.pengaduan.PengaduanScreen
import com.jargas.si_pekat.ui.perbaikan.PerbaikanScreen
import com.jargas.si_pekat.ui.theme.AppColors

enum class Route { Main, Pengaduan, Meter, Kunjungan, Perbaikan }

/**
 * Akar aplikasi: login ⇄ halaman utama ⇄ modul. Navigasi sengaja sederhana (state + BackHandler)
 * karena hanya ada satu tingkat: Main → modul.
 */
@Composable
fun AppRoot() {
    var session by remember { mutableStateOf(SessionStore.load()) }
    // Bila ada sesi tersimpan, cache lokal dimuat dulu (setara `await initFromLocal` sebelum runApp di Flutter).
    var ready by remember { mutableStateOf(session == null) }
    var route by rememberSaveable { mutableStateOf(Route.Main) }

    LaunchedEffect(Unit) {
        session?.let { AppSessionCache.initFromLocal(it.nama, it.email) }
        ready = true
    }

    Box(Modifier.fillMaxSize().background(AppColors.Canvas)) {
        val s = session
        when {
            !ready -> Unit
            s == null -> LoginScreen(
                onLoggedIn = { nama, email ->
                    session = SessionStore.Session(email = email, nama = nama)
                    route = Route.Main
                },
            )
            else -> {
                BackHandler(enabled = route != Route.Main) { route = Route.Main }
                val back = { route = Route.Main }
                when (route) {
                    Route.Main -> MainNavScreen(
                        userNama = s.nama,
                        userEmail = s.email,
                        onOpenModule = { route = it },
                        onLogout = {
                            SessionStore.clear()
                            AppSessionCache.clear()
                            session = null
                            route = Route.Main
                        },
                    )
                    Route.Pengaduan -> PengaduanScreen(userName = s.email, userNama = s.nama, onBack = back)
                    Route.Meter -> OcrHomeScreen(userNama = s.nama, userEmail = s.email, onBack = back)
                    Route.Kunjungan -> KunjunganScreen(onBack = back)
                    Route.Perbaikan -> PerbaikanScreen(onBack = back)
                }
            }
        }
    }
}

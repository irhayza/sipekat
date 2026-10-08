package com.jargas.si_pekat.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.data.AppSessionCache
import com.jargas.si_pekat.ui.Route
import com.jargas.si_pekat.ui.theme.AppColors

@Composable
fun DashboardScreen(userNama: String, userEmail: String, onOpenModule: (Route) -> Unit) {
    var warming by remember { mutableStateOf(false) }

    // Jaring pengaman: sesi lama (aplikasi dibuka ulang, bukan lewat form login) mungkin belum punya cache →
    // muat diam-diam sekali supaya modul tidak antre ke Google Sheet saat pertama dibuka.
    LaunchedEffect(Unit) {
        if (!AppSessionCache.isPreloaded && !AppSessionCache.isPreloading) {
            warming = true
            try {
                AppSessionCache.preloadAll(nama = userNama, email = userEmail)
            } finally {
                warming = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Canvas).verticalScroll(rememberScrollState())) {
        // ── Header ──
        Box(
            Modifier.fillMaxWidth().clipToBounds()
                .background(Brush.verticalGradient(listOf(AppColors.BrandDark, AppColors.Brand))),
        ) {
            Box(Modifier.align(Alignment.TopEnd).offset(50.dp, (-50).dp).size(250.dp).clip(CircleShape).background(AppColors.Pengaduan.copy(alpha = 0.10f)))
            Box(Modifier.align(Alignment.BottomStart).offset((-40).dp, 80.dp).size(200.dp).clip(CircleShape).background(AppColors.Selesai.copy(alpha = 0.10f)))

            Column(Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).shadow(6.dp, CircleShape, ambientColor = AppColors.Flame, spotColor = AppColors.Flame)
                            .clip(CircleShape).background(Color.White).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Person, null, tint = AppColors.Flame, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Selamat datang,", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            userNama, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(1.dp, AppColors.Line.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = AppColors.Selesai, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sistem Online", color = AppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        AppConfig.APP_VERSION, color = AppColors.Flame, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(AppColors.Flame.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // ── Indikator pemanasan cache ──
        if (warming) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(AppColors.BrandLight).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AppColors.Ink)
                Spacer(Modifier.width(10.dp))
                Text("Menyegarkan data modul di latar belakang...", fontSize = 12.sp, color = AppColors.Ink, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Grid modul ──
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleCard(Icons.Rounded.Campaign, "Pengaduan", "Laporan keluhan pelanggan", AppColors.Pengaduan, Modifier.weight(1f)) { onOpenModule(Route.Pengaduan) }
                ModuleCard(Icons.Rounded.Speed, "Pencatatan Meter", "Baca & catat stand meter", AppColors.Meter, Modifier.weight(1f)) { onOpenModule(Route.Meter) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModuleCard(Icons.Rounded.DirectionsWalk, "Laporan Kunjungan", "Kunjungan & reopen tiket", AppColors.Kunjungan, Modifier.weight(1f)) { onOpenModule(Route.Kunjungan) }
                ModuleCard(Icons.Rounded.Build, "Laporan Perbaikan", "Form penanganan petugas", AppColors.Perbaikan, Modifier.weight(1f)) { onOpenModule(Route.Perbaikan) }
            }
        }
    }
}

@Composable
private fun ModuleCard(icon: ImageVector, label: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .aspectRatio(0.95f)
            .shadow(8.dp, shape, ambientColor = color.copy(alpha = 0.3f), spotColor = color.copy(alpha = 0.3f))
            .clip(shape)
            .background(AppColors.Surface.copy(alpha = 0.85f))
            .border(1.5.dp, AppColors.Line.copy(alpha = 0.8f), shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(12.dp),
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.weight(1f))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Ink, letterSpacing = 0.3.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 12.sp, color = AppColors.Body, lineHeight = 15.6.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.clip(CircleShape).background(AppColors.BrandDark).border(1.dp, AppColors.Line, CircleShape).padding(8.dp)) {
                Icon(Icons.Rounded.ArrowForward, null, tint = AppColors.Flame, modifier = Modifier.size(16.dp))
            }
        }
    }
}

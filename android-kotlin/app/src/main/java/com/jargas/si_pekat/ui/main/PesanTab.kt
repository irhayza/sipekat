package com.jargas.si_pekat.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.ui.theme.AppColors

private data class Pesan(val title: String, val desc: String, val time: String, val icon: ImageVector)

private val PESAN = listOf(
    Pesan("Penugasan Baru Tersedia", "Data penugasan sektor baru telah dimuat. Silakan periksa daftar penugasan Anda.", "Baru saja", Icons.Rounded.AssignmentTurnedIn),
    Pesan("Sinkronisasi Berhasil", "Data pembacaan stand meter berhasil dikirim ke server google sheet.", "1 jam yang lalu", Icons.Rounded.CheckCircle),
    Pesan("Tips Membaca OCR", "Posisikan kamera sejajar dengan angka meter dan pastikan cahaya cukup untuk hasil maksimal.", "Kemarin", Icons.Rounded.TipsAndUpdates),
)

/** Konten masih statis, sama seperti versi Flutter. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesanTab() {
    Column(Modifier.fillMaxSize().background(AppColors.Canvas)) {
        TopAppBar(
            title = { Text("Pesan & Notifikasi") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BrandDark, titleContentColor = Color.White),
        )
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(PESAN) { m ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(AppColors.Meter.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(m.icon, null, tint = AppColors.Meter)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Ink)
                            Spacer(Modifier.height(8.dp))
                            Text(m.desc, fontSize = 12.sp, lineHeight = 16.8.sp, color = AppColors.Body)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(m.time, color = AppColors.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

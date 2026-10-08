package com.jargas.si_pekat.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.data.OcrLocalDb
import com.jargas.si_pekat.model.MeterReading
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.CancellationException

private val TABS = listOf("Catat Meter", "Pengaduan", "Kunjungan", "Perbaikan")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatTab() {
    var selected by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(AppColors.Canvas)) {
        Column(Modifier.background(AppColors.BrandDark)) {
            TopAppBar(
                title = { Text("Riwayat Pekerjaan") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BrandDark, titleContentColor = Color.White),
            )
            ScrollableTabRow(
                selectedTabIndex = selected,
                containerColor = AppColors.BrandDark,
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { positions ->
                    with(TabRowDefaults) {
                        SecondaryIndicator(Modifier.tabIndicatorOffset(positions[selected]), color = AppColors.Selesai)
                    }
                },
            ) {
                TABS.forEachIndexed { i, label ->
                    Tab(
                        selected = selected == i,
                        onClick = { selected = i },
                        text = { Text(label) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
        when (selected) {
            0 -> com.jargas.si_pekat.ui.ocr.ReadingsList()
            1 -> ActivityHistoryList(com.jargas.si_pekat.data.ActivityHistory.Kind.PENGADUAN, "Belum ada riwayat pengaduan")
            2 -> com.jargas.si_pekat.ui.kunjungan.KunjunganHistory()
            else -> ActivityHistoryList(com.jargas.si_pekat.data.ActivityHistory.Kind.PERBAIKAN, "Belum ada riwayat laporan perbaikan")
        }
    }
}

@Composable
private fun EmptyHistory(title: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.History, null, tint = AppColors.Muted, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = AppColors.Muted)
    }
}

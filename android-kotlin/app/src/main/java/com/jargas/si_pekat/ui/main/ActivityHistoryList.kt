package com.jargas.si_pekat.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.data.ActivityHistory
import com.jargas.si_pekat.ui.components.EmptyState
import com.jargas.si_pekat.ui.components.StatusBadge
import com.jargas.si_pekat.ui.theme.AppColors
import kotlinx.coroutines.launch

/** Daftar riwayat laporan (Pengaduan / Perbaikan) di perangkat ini, dengan hapus per entri. */
@Composable
fun ActivityHistoryList(kind: ActivityHistory.Kind, emptyTitle: String) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    fun reload() { scope.launch { items = ActivityHistory.list(kind); loading = false } }
    LaunchedEffect(kind) { reload() }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Brand) }
        items.isEmpty() -> EmptyState(Icons.Rounded.HistoryToggleOff, emptyTitle, "Laporan yang Anda kirim dari perangkat ini akan muncul di sini.")
        else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { m ->
                val ts = m["timestamp"]?.toString().orEmpty()
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Surface), elevation = CardDefaults.cardElevation(3.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m["id"]?.toString().orEmpty(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = AppColors.Ink, modifier = Modifier.weight(1f, fill = false))
                                Spacer(Modifier.width(8.dp))
                                StatusBadge(m["status"]?.toString().orEmpty(), fontSize = 10)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(m["nama"]?.toString().orEmpty().ifEmpty { "—" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppColors.Ink)
                            val idpel = m["idpel"]?.toString().orEmpty()
                            if (idpel.isNotEmpty()) Text("IDPEL: $idpel", fontSize = 11.5.sp, color = AppColors.Muted)
                            val title = m["title"]?.toString().orEmpty()
                            if (title.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(title, fontSize = 12.sp, color = AppColors.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(if (ts.length >= 16) ts.substring(0, 16).replace('T', ' ') else ts, fontSize = 11.sp, color = AppColors.Muted)
                        }
                        IconButton(onClick = { deleteId = m["id"]?.toString() }) { Icon(Icons.Rounded.DeleteOutline, "Hapus", tint = AppColors.Danger) }
                    }
                }
            }
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Hapus Riwayat?") },
            text = { Text("Entri $id dihapus dari riwayat di perangkat ini. Data di server tidak berubah.") },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal", color = AppColors.Body) } },
            confirmButton = { TextButton(onClick = { deleteId = null; scope.launch { ActivityHistory.remove(kind, id); reload() } }) { Text("Hapus", color = AppColors.Danger) } },
        )
    }
}
